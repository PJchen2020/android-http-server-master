/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package ro.polak.webserver.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import ro.polak.http.WebServer;

/**
 * The main server Android activity. This class is designed to be extended.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201008
 */
public abstract class BaseMainActivity extends AppCompatActivity {

    private static final Logger LOGGER = Logger.getLogger(BaseMainActivity.class.getName());

    private static final int PERMISSIONS_REQUEST_CODE = 5543;
    private BaseMainService mainService;
    private boolean isMainServiceBound = false;

    //private final String serviceKilledByServiceBroadcast = "serviceKilledByServiceBroadcast";
    //private MyDefineBroadcastReceiver mReceiver = null;
    //private Timer timer;

    /**
     * Returns the main service.
     *
     * @return
     */
    protected BaseMainService getMainService() {
        return mainService;
    }

    /**
     * Tells whether the main service is bound.
     *
     * @return
     */
    protected boolean isMainServiceBound() {
        return isMainServiceBound;
    }

    /**
     * Returns the class of the main service.
     * This information is used for activity-service communication.
     *
     * @return
     */
    @NonNull
    protected abstract Class<? extends BaseMainService> getServiceClass();

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
            //Toast.makeText(getApplicationContext(), "onServiceConnected", Toast.LENGTH_SHORT).show();
            BaseMainService.LocalBinder binder = (BaseMainService.LocalBinder) iBinder;
            mainService = binder.getService();
            mainService.registerClient(BaseMainActivity.this);
            isMainServiceBound = true;

            boolean hasAllPermissionsApproved = getMissingPermissions().length == 0;

            if (!mainService.getServiceState().isServiceStarted() && hasAllPermissionsApproved) {
                //Toast.makeText(getApplicationContext(), "onServiceConnected --> startBackgroundService", Toast.LENGTH_SHORT).show();
                startBackgroundService();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onServiceDisconnected(final ComponentName arg0) {
            isMainServiceBound = false;
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    if (isAnyPermissionMissing(grantResults)) {
                        showMustAcceptPermissions();
                    } else {
                        doOnPermissionsAccepted();
                        Toast.makeText(getApplicationContext(), "onRequestPermissionsResult::handleServiceStart", Toast.LENGTH_SHORT).show();
                        handleServiceStart();
                    }
                } else {
                    showMustAcceptPermissions();
                }
                break;
            default:
                throw new IllegalStateException("Unknown permission request " + requestCode);
        }
    }

    /**
     * Call this method to force update the state of the service.
     */
    public void notifyStateChanged() {
        if (isMainServiceBound) {
            BaseMainService.ServiceStateDTO serviceStateDTO = mainService.getServiceState();
            if (serviceStateDTO.isWebServerStarted()) {
                println("Starging HTTPD");
                doNotifyStateChangedToOnline(serviceStateDTO);
            } else {
                println("Stopping HTTPD");
                doNotifyStateChangedToOffline();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Toast.makeText(getApplicationContext(), "onStart", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, getServiceClass());
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (isMainServiceBound) {
            unbindService(serviceConnection);
            isMainServiceBound = false;
        }

//        if(null != mReceiver) {
//            unregisterReceiver(mReceiver);
//            mReceiver = null;
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        doOnCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            requestPermissions();
        }

        //timer = new Timer(true);
        //timer.schedule(timerTask, 1000 * 60, 1000 * 10);
    }

    /**
     * Returns a set of required permissions.
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    protected Set<String> getRequiredPermissions() {
        return new HashSet<>(Arrays.asList(
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
                //Manifest.permission.ACCESS_BACKGROUND_LOCATION //添加这不会主动要权限  -- why？
        ));
    }

    /**
     * Calling this method triggers stopping the service for proper shutdown.
     */
    protected void requestServiceStop() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mainService.getServiceState().isServiceStarted()) {
                    Intent serviceIntent = new Intent(BaseMainActivity.this, getServiceClass());
                    stopService(serviceIntent);
                }
                BaseMainActivity.this.finish();
            }
        });
    }

    /**
     * GUI print debug method.
     *
     * @param text
     */
    protected void println(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("HTTP", text);
            }
        });
    }


    /**
     * Requests missing permissions.
     */
    protected void requestPermissions() {
        String[] permissionsNotGrantedYet = getMissingPermissions();

        if (permissionsNotGrantedYet.length > 0) {
            doRequestPermissions();

            // TODO Implement displaying rationale
            ActivityCompat.requestPermissions(this, permissionsNotGrantedYet, PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback executed when state changed to offline.
     */
    protected void doNotifyStateChangedToOffline() {
        //
    }

    /**
     * Callback executed when state changed to online.
     *
     * @param serviceStateDTO
     */
    protected void doNotifyStateChangedToOnline(final BaseMainService.ServiceStateDTO serviceStateDTO) {
        //
    }

    /**
     * Callback executed upon requesting permissions.
     */
    protected void doRequestPermissions() {
        //
    }

    /**
     * Callback executed upon creating the activity.
     */
    protected void doOnCreate() {
        //
    }

    /**
     * Callback executed upon all required permissions accepted.
     */
    protected void doOnPermissionsAccepted() {
        //
    }

    /**
     * Callback executed upon requesting all permissions.
     */
    protected void doShowMustAcceptPermissions() {
        //
    }

    private void handleServiceStart() {
        if (isMainServiceBound) {
            Toast.makeText(getApplicationContext(), "isMainServiceBound is true", Toast.LENGTH_SHORT).show();
            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mainService.getServiceState().isServiceStarted()) {
                        Toast.makeText(getApplicationContext(), "handleServiceStart::startBackgroundService", Toast.LENGTH_SHORT).show();
                        startBackgroundService();
                    } else {
                        Toast.makeText(getApplicationContext(), "Service already started!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Background service not bound!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAnyPermissionMissing(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public void startBackgroundService() {
        Log.d("BaseMainActivity", "startBackgroundService");
        Intent serviceIntent = new Intent(BaseMainActivity.this, getServiceClass());
        //startService(serviceIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);  //Derek 2024/08/26
        }

        //Toast.makeText(getApplicationContext(), "Starting background service", Toast.LENGTH_SHORT).show();
        //Intent serviceBTTester = new Intent(BaseMainActivity.this, BluetoothTester.class);
        //startService(serviceBTTester);
    }

    private void showMustAcceptPermissions() {
        doShowMustAcceptPermissions();
        Toast.makeText(getApplicationContext(),
                "You must grant all permissions to run the server", Toast.LENGTH_SHORT).show();
    }

    /**
     * To revoke user permissions please execute adb shell pm reset-permissions.
     *
     * @return
     */
    @NonNull
    private String[] getMissingPermissions() {
        Set<String> permissionsNotGrantedYet = new HashSet<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : getRequiredPermissions()) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNotGrantedYet.add(permission);
                }
            }
        }
        String[] permissionsNotGrantedYetArray = new String[permissionsNotGrantedYet.size()];
        permissionsNotGrantedYet.toArray(permissionsNotGrantedYetArray);
        return permissionsNotGrantedYetArray;
    }

    //add by Derek 2024/08/26
//    private class MyDefineBroadcastReceiver extends BroadcastReceiver
//    {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if (serviceKilledByServiceBroadcast.equalsIgnoreCase(action)) {
//                LOGGER.log(Level.INFO, "re-start main service killed by system!");
//                startBackgroundService();
//            }
//        }
//    }
//
//    TimerTask timerTask = new TimerTask() {
//        @Override
//        public void run() {
//            Message message = new Message();
//            message.what = 1;
//            handler.sendMessage(message);
//        }
//    };
//
//    @SuppressLint(value = "HandlerLeak")
//    Handler handler = new Handler(){
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            if(msg.what == 1) {
//                LOGGER.log(Level.INFO, "my message");
//                //if(!getMainService().getServiceState().isWebServerStarted())
//                    //startBackgroundService();
//            }
//            super.handleMessage(msg);
//        }
//    };

//    @SuppressLint("UnspecifiedRegisterReceiverFlag")
//    public void  registerMyBroadcast() {
//        //Register the BroadcastReceiver
//        mReceiver = new MyDefineBroadcastReceiver();
//        LOGGER.log(Level.INFO, "Register the BroadcastReceiver for serviceKilledByServiceBroadcast");
//        IntentFilter filter = new IntentFilter(serviceKilledByServiceBroadcast);
//        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
//    }
}
