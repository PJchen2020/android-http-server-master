/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package ro.polak.webserver.base;

import static ro.polak.http.configuration.impl.ServerConfigImpl.PROPERTIES_FILE_NAME;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import ro.polak.http.configuration.ServerConfig;
import ro.polak.http.configuration.ServerConfigFactory;
import ro.polak.http.controller.Controller;
import ro.polak.http.controller.impl.ControllerImpl;
import ro.polak.http.gui.ServerGui;
import ro.polak.webserver.base.impl.BaseAndroidServerConfigFactory;
import ro.polak.webserver.base.logic.AssetUtil;

/**
 * Main application service that holds http server.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201709
 */
public abstract class BaseMainService extends Service implements ServerGui {

    private final Logger LOGGER = Logger.getLogger(BaseMainService.class.getName());
    private static final int NOTIFICATION_ID = 0;
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int NOTIFY_ID = 100;

    @Nullable
    private BaseMainActivity activity = null;
    private Controller controller;
    private final IBinder binder = new LocalBinder();
    private boolean isServiceStarted = false;

    NotificationCompat.Builder notification; //added by Derek
    NotificationManagerCompat notificationManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        isServiceStarted = true;
        //Toast.makeText(getApplicationContext(), "MainServiceThreadID = " + Thread.currentThread().getId(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(), "AppThreadID = " + Looper.getMainLooper().getThread().getId(), Toast.LENGTH_SHORT).show();

        if (activity == null) {
            throw new NullPointerException("Activity must be set at this point.");
        }

        //return AndroidServerConfigFactory object for MainService
        ServerConfigFactory serverConfigFactory = getServerConfigFactory(activity);

        doFirstRunChecks(serverConfigFactory);

        controller = new ControllerImpl(serverConfigFactory, ServerSocketFactory.getDefault(), this);
        controller.start();

        //start BT test service
        //intent.setClass(this, BluetoothTester.class);
        //startService(intent);
        //bindBTService();

        Notification notification1 = notification.build();
        startForeground(100, notification1);//这个就是之前说的startForeground

        return START_STICKY;
    }

    protected void bindBTService() {}

    /**
     * Allows overwriting server config factory.
     *
     * @param context
     * @return
     */
    @NonNull
    protected BaseAndroidServerConfigFactory getServerConfigFactory(final Context context) {
        return new BaseAndroidServerConfigFactory(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        LOGGER.log(Level.INFO, "BaseMainService is destroy");

        if (getServiceState().isWebServerStarted()) {
            LOGGER.log(Level.INFO, "call controller.stop from BaseMainService::onDestroy");
            controller.stop();
        }

        //re-start MainService
//        Intent intentRestartMainService = new Intent("serviceKilledByServiceBroadcast");
//        if(activity != null) {
//            LOGGER.log(Level.INFO, "sendBroadcast for serviceKilledByServiceBroadcast");
//            activity.moveTaskToBack(false);
//            activity.startBackgroundService();
//            activity.sendBroadcast(intentRestartMainService);
//        }

        isServiceStarted = false;
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager.cancel(NOTIFY_ID);
    }

    /**
     * Registers client to allow activity-service communication.
     *
     * @param activity
     */
    public void registerClient(final BaseMainActivity activity) {
        this.activity = activity;
    }

    /**
     * Returns webserver controller.
     *
     * @return
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Returns current service state.
     *
     * @return
     */
    public ServiceStateDTO getServiceState() {
        String accessUrl = "Initializing";
        if (controller != null && controller.getWebServer() != null) {
            accessUrl = "http://"
                    + getLocalIpAddress()
                    + getPort(controller.getWebServer().getServerConfig().getListenPort())
                    + '/';
        }

        boolean isWebserverStarted = controller != null
                && controller.getWebServer() != null
                && controller.getWebServer().isRunning();

        return new ServiceStateDTO(isServiceStarted, isWebserverStarted, accessUrl);
    }

    @NonNull
    private String getPort(final int port) {
        if (port != DEFAULT_HTTP_PORT) {
            return ":" + port;
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void start() {
        if (activity != null) {
            createNotification();
            activity.notifyStateChanged();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void  createNotification() {
        //https://blog.csdn.net/xxdw1992/article/details/80948315  -- PendingIntent详解
        //https://blog.csdn.net/shanshui911587154/article/details/105683683   -- Android10.0通知Notification
        Intent notificationIntent = new Intent(this, getActivityClass());
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setComponent(new ComponentName(this, getActivityClass()));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); //added by Derek
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        //added by Derek
        String channelID = createNotificationChannel("httpd-server-ID", "httpd-server", NotificationManager.IMPORTANCE_HIGH);
        assert channelID != null;
        notification = new NotificationCompat.Builder(this, channelID).setContentTitle("httpd-server")
                .setContentText("Server has started")
                .setContentIntent(pIntent)
                .setSmallIcon(R.drawable.online)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFY_ID, notification.build());
        //end add

        //setNotification(getNotificationBuilder(pIntent, "Started", R.drawable.online).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (activity != null) {
            activity.notifyStateChanged();
        }

        //https://blog.csdn.net/xxdw1992/article/details/80948315
//        Intent notificationIntent = new Intent(this, getActivityClass());
//        PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//        setNotification(getNotificationBuilder(pIntent, "Stopped", R.drawable.offline).build());

        //LOGGER.log(Level.INFO, "service is stopped");
        notification.setSmallIcon(R.drawable.offline).setContentText("Server has stopped");
        notificationManager.notify(NOTIFY_ID, notification.build());
    }

    /**
     * Returns the the class of the activity to be registered.
     *
     * @return
     */
    @NonNull
    protected abstract Class<? extends BaseMainActivity> getActivityClass();

    private void doFirstRunChecks(final ServerConfigFactory serverConfigFactory) {

        assert activity != null;
        //will create folder:files:  "/data/user/0/ro.polak.webserver/files"  if not exist
        File externalStorageDirectory = activity.getFilesDir();

        //return a ServerConfigImpl object
        ServerConfig serverConfig = serverConfigFactory.getServerConfig(this);
        // basePath = /data/user/0/ro.polak.webserver/files/httpd/
        //String basePath = externalStorageDirectory + serverConfig.getBasePath();
        //by Derek 2024/08/06 to avoid the basePath will change to "/data/user/0/ro.polak.webserver/files/data/user/0/ro.polak.webserver/files/httpd/"
        //when run second time or later
        String basePath = externalStorageDirectory + File.separator + "httpd" + File.separator;
        //String staticDirPath = externalStorageDirectory + serverConfig.getDocumentRootPath();
        String staticDirPath = externalStorageDirectory + File.separator + "www" + File.separator; // by Derek 2024/08/06

        //create /data/user/0/ro.polak.webserver/files/httpd/
        File baseDir = new File(basePath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new ConfigurationException("Unable to create directory " + baseDir.getAbsolutePath());
        }
        //Toast.makeText(getApplicationContext(), "basePath = " + basePath, Toast.LENGTH_SHORT).show();

        //create /data/user/0/ro.polak.webserver/files/httpd/WWW/
        File staticDir = new File(staticDirPath);
        if (!staticDir.exists() && !staticDir.mkdirs()) {
            throw new ConfigurationException("Unable to create directory " + staticDir.getAbsolutePath());
        }
        //Toast.makeText(getApplicationContext(), "staticDirPath = " + staticDirPath, Toast.LENGTH_SHORT).show();

        AssetManager assetManager = this.getResources().getAssets();

        //create  /data/user/0/ro.polak.webserver/files/httpd/httpd.properties
        File config = new File(basePath + PROPERTIES_FILE_NAME);
        if (!config.exists()) {
            try {
                config.createNewFile();
                //String[] fileListConf = assetManager.list("conf");
                //String[] fileListHtml = assetManager.list("html");
                AssetUtil.copyAssetToFile(assetManager, "conf" + File.separator + PROPERTIES_FILE_NAME, config);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }


        //create  /data/user/0/ro.polak.webserver/files/httpd/mime.type
        File mimeType = new File(basePath + "mime.type");
        if (!mimeType.exists()) {
            try {
                mimeType.createNewFile();
                AssetUtil.copyAssetToFile(assetManager, "conf" + File.separator + "mime.type", mimeType);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }

        //Add by Derek 2024/08/06 to copy folder "html" to basePath(/data/user/0/ro.polak.webserver/files/httpd/www/html/)
        //https://blog.csdn.net/pbm863521/article/details/78811250
        String[] filesHtml;
        try {
            File htmlDir = new File(basePath + "www/html/");
            htmlDir.mkdirs();
            filesHtml = assetManager.list("html");
            if (filesHtml != null) {
                for (String file : filesHtml) {
                    File fileHtml = new File(basePath + "www/html/" + file);
                    fileHtml.createNewFile();
                    AssetUtil.copyAssetToFile(assetManager, "html" + File.separator + file, fileHtml);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }


    private void setNotification(final Notification notification) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @SuppressWarnings("deprecation")
    private Notification.Builder getNotificationBuilder(final PendingIntent pIntent, final String text, final int icon) {
        return new Notification.Builder(this)
                .setContentTitle("HTTPServer")
                .setContentText(text)
                .setSmallIcon(icon)
                .setContentIntent(pIntent)
                .setOngoing(true)
                .addAction(R.drawable.online, "Open", pIntent);
    }

    /**
     * Helper method returning the current IP address.
     *
     * @return String
     */
    private String getLocalIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            int ipAddress = wifiInfo.getIpAddress();
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

            InetAddress inetAddress = InetAddress.getByAddress(BigInteger.valueOf(ipAddress).toByteArray());
            return inetAddress.getHostAddress();

        } catch (Exception e) {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            } catch (SocketException ex) {
                LOGGER.log(Level.SEVERE, "Unable to obtain own IP address", e);
            }
        }

        return "127.0.0.1";
    }

    /**
     * Local binder instance.
     */
    public class LocalBinder extends Binder {

        /**
         * Returns bounded service instance.
         *
         * @return
         */
        public BaseMainService getService() {
            return BaseMainService.this;
        }
    }

    /**
     * Represents the service state.
     */
    public static final class ServiceStateDTO {
        private boolean isServiceStarted;
        private boolean isWebServerStarted;
        private String accessUrl;

        public ServiceStateDTO(final boolean isServiceStarted, final boolean isWebServerStarted, final String accessUrl) {
            this.isServiceStarted = isServiceStarted;
            this.isWebServerStarted = isWebServerStarted;
            this.accessUrl = accessUrl;
        }

        public boolean isServiceStarted() {
            return isServiceStarted;
        }

        public boolean isWebServerStarted() {
            return isWebServerStarted;
        }

        public String getAccessUrl() {
            return accessUrl;
        }
    }

    //Add by Derek 2024/08/27
    private String createNotificationChannel(String channelID, String channelName, int level) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
           NotificationChannel channel = new NotificationChannel(channelID, channelName, level);
           manager.createNotificationChannel(channel);

           return channelID;
        } else {
            return null;
        }
    }
}
