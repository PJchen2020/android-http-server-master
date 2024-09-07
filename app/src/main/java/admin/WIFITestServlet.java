/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package admin;

import static android.content.Context.MODE_PRIVATE;

import static api.logic.APIResponse.MEDIA_TYPE_APPLICATION_JSON;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import admin.TestModule.DeviceWiFiManager;
import admin.logic.HtmlReader;
import api.BluetoothServlet;
import ro.polak.http.configuration.ServerConfig;
import ro.polak.http.exception.ServletException;
import ro.polak.http.servlet.HttpServlet;
import ro.polak.http.servlet.HttpServletRequest;
import ro.polak.http.servlet.HttpServletResponse;
import ro.polak.webserver.MainService;

/**
 * Admin panel front page.
 */
public class WIFITestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(WIFITestServlet.class.getName());
    private static MainService mainService;
    private WIFITester wifiTester;
    final int ScanTimeout = 60;

    private static final String WIFI_ACTION = "action";
    private static final String WIFI_ADDRESS = "address";
    public static final String CONNECT = "connect";
    public static final String SCAN = "scan";
    private static final String ENABLE = "enable";
    private static final String INIT = "init";

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, InterruptedException {
//        response.getWriter().print("WIFITestServlet thread ID = " + Thread.currentThread().getId());

        LOGGER.log(Level.INFO, "Entry WIFITestServlet service process");

        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        mainService = (MainService)serverConfig.getServiceContext(); //getServiceContext --> ro.polak.webserver.MainService@6103e86
        if(null == mainService){
            response.getWriter().print("MainService object is null!!");
            return;
        }

        if(!wifiTestInit()) {
            response.getWriter().print("wifiTestInit Fail!!");
            return;
        }

        try {
            String wifi_action = request.getParameter(WIFI_ACTION);
            String wifi_address = request.getParameter(WIFI_ADDRESS);
            response.setContentType(MEDIA_TYPE_APPLICATION_JSON);

            if(wifi_action!=null && wifi_action.equals(ENABLE))
            {
                WIFITestResult wifiTestResult=new WIFITestResult();
                wifiTestResult.message = "enable WIFI fail";
                wifiTestResult.result = "Fail";
                boolean isWifiSupported = wifiTester.isWifiSupported();
                if (isWifiSupported) {
                    if(wifiTester.isWifiEnable()){
                        wifiTestResult.result = "Pass";
                        wifiTestResult.message = "enable WIFI successfully";
                    }
                    else wifiTestResult.message = "enable WIFI fail";
                } else
                    wifiTestResult.message = "WIFI not support!";
                Gson gson = new Gson();
                response.getWriter().print(gson.toJson(wifiTestResult));
            }

            if(wifi_action!=null && wifi_action.equals(INIT))
            {
                if(!wifiTestInit()) {
                    response.getWriter().print("wifiTestInit Fail!!");
                }
                response.getWriter().print(outputWIFIInfo(mainService));
            }

            if(wifi_action!=null && wifi_action.equals(SCAN))
            {
                WIFIScanTestResult wifiTestResult=new WIFIScanTestResult();
                List<WIFIScanResultInfo> wifiScanResultInfoList =new ArrayList<>();
                if(!wifiTester.isWifiSupported()){
                    wifiTestResult.result="Fail";
                    wifiTestResult.errMsg  = "WIFI not support!";
                }
                if(!wifiTester.isWifiEnable()){
                    wifiTestResult.result="Fail";
                    wifiTestResult.errMsg  = "WIFI not enable!";
                }
                wifiTester.scanWifi();
                Gson gson = new Gson();
                for (int i = 0; i < ScanTimeout; ++i) {
                    if (wifiTester.isScanFinished) {
                        wifiTestResult.scanMsg = "Scan finished in " + i + " seconds";
                        //strConnectResult = bluetoothTester.connectToBluetoothDevice("C4:DF:39:8C:08:14");
                        break;
                    }

                    Thread.sleep(1000);

                    if (i + 1 >= ScanTimeout) {
                        wifiTestResult.result="Fail";
                        wifiTestResult.errMsg  = "Scan process time out!";
                    }
                }//end of for
                if (!wifiTester.mWiFiList.isEmpty()) {
                    // 键PairedDevices的值是对象，所以又要创建一个对象
                    LOGGER.log(Level.INFO, "DeviceWiFiManager.scanResult: no null"+wifiTester.mWiFiList);

                    for (ScanResult result : wifiTester.mWiFiList) {
                        WIFIScanResultInfo wifiScanResultInfo=new WIFIScanResultInfo();
                        wifiScanResultInfo.SSID=result.SSID;
                        wifiScanResultInfo.BSSID=result.BSSID;
                        wifiScanResultInfoList.add(wifiScanResultInfo);
                    }
                    wifiTestResult.result="Pass";
                    wifiTestResult.wifiApListCount=  wifiScanResultInfoList.size();
                    wifiTestResult.wifiApList=wifiScanResultInfoList;
                }

                response.getWriter().print(gson.toJson(wifiTestResult));
            }


//            response.getWriter().print(outputWIFIInfo(mainService));

        }
        catch (Exception exception){
            response.getWriter().print(exception.toString());
        }

//        response.getWriter().print(RenderHtmlFile());
    }


    private String outputWIFIInfo(MainService ms) throws JSONException {
        WIFITestInfo wifiTestInfo=new WIFITestInfo();
        String ScanMsg = "Task not started";
        JSONObject wifiObject = new JSONObject();
        boolean isWifiEnable = false;
        boolean isWifiSupported = wifiTester.isWifiSupported();
        String strEnableStatus = "Enable WIFI fail";
        Map<String, String> scanAPlist = new HashMap<>();
        String strConnectResult = "not test";
        String errMsg = "";

        if (isWifiSupported) {
            isWifiEnable = wifiTester.isWifiEnable();
        } else
            errMsg = "WIFI not support!";

        try {
            wifiTestInfo.isWifiSupported=isWifiSupported;
//            wifiObject.put("isWifiSupported", isWifiSupported);
            if(isWifiSupported) {
                wifiTestInfo.isWifiEnable=isWifiEnable;
                wifiTestInfo.localIP=wifiTester.getLocalIP();
                wifiTestInfo.isNetConnection=wifiTester.isNetConnection(ms);
//                wifiObject.put("isWifiEnable", isWifiEnable);
//                wifiObject.put("localIP", wifiTester.getLocalIP());
//                wifiObject.put("isNetConnection", wifiTester.isNetConnection(ms));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    wifiTestInfo.isNetSystemAvailable=wifiTester.isNetSystemAvailable(ms);
//                    wifiObject.put("isNetSystemAvailable", wifiTester.isNetSystemAvailable(ms));
                }
                wifiTestInfo.webTestBaidu=wifiTester.startWebTest("https://www.baidu.com");
//                wifiObject.put("webTestBaidu", wifiTester.startWebTest("https://www.baidu.com"));
//                wifiObject.put("Web test of Google", wifiTester.startWebTest("https://www.google.com.hk/?hl=cn"));
//                wifiObject.put("Ping test of 192.168.3.1", wifiTester.startPingTest("192.168.99.188"));//home 3.1  office:99.188

                wifiTester.scanWifi();
                for (int i = 0; i < ScanTimeout; ++i) {
                    if (wifiTester.isScanFinished) {
                        ScanMsg = "Scan finished in " + i + " seconds";
                        //strConnectResult = bluetoothTester.connectToBluetoothDevice("C4:DF:39:8C:08:14");
                        break;
                    }

                    Thread.sleep(1000);

                    if (i + 1 >= ScanTimeout) {
                        ScanMsg = "Scan process time out!";
                    }
                }//end of for

                wifiTester.startConnectTest("f6:ce:e2:74:32:1b");  //my realme phone:f6:ce:e2:74:32:0b
                for (int i = 0; i < ScanTimeout; ++i) {
                    if (wifiTester.isbConnectTestFinished) {
                        strConnectResult = "Connect finished in " + i + " seconds";
                        break;
                    }

                    Thread.sleep(1000);

                    if (i + 1 >= ScanTimeout) {
                        strConnectResult = "Connect process time out!";
                    }
                }//end of for

                if (!wifiTester.mWiFiList.isEmpty()) {
                    // 键PairedDevices的值是对象，所以又要创建一个对象
                    List<WIFIScanResultInfo> wifiScanResultInfoList =new ArrayList<>();
                    for (ScanResult result : wifiTester.mWiFiList) {
                        WIFIScanResultInfo wifiScanResultInfo=new WIFIScanResultInfo();
                        wifiScanResultInfo.SSID=result.SSID;
                        wifiScanResultInfo.BSSID=result.BSSID;
                        wifiScanResultInfoList.add(wifiScanResultInfo);
                    }
                    wifiTestInfo.wifiApListCount=wifiScanResultInfoList.size();
                    wifiTestInfo.wifiApList=wifiScanResultInfoList;
//                    wifiObject.put("wifiApListCount", wifiScanResultInfoList.size());
//                    wifiObject.put("wifiApList", wifiScanResultInfoList);
                }
                wifiTestInfo.scanWifiResult=wifiTester.bScanResult;
                wifiTestInfo.scanMsg=ScanMsg;
                wifiTestInfo.connectResult=wifiTester.bSConnectResult;
                wifiTestInfo.connectMsg=strConnectResult;
//                wifiObject.put("scanWifiResult", wifiTester.bScanResult);
//                wifiObject.put("scanMsg", ScanMsg);
//                wifiObject.put("connectWifiResult", wifiTester.bSConnectResult);
//                wifiObject.put("connectMsg", strConnectResult);
            }
            wifiTestInfo.errMsg=errMsg;
//            wifiObject.put("errMsg", errMsg);
        }
        catch (Exception e){
            return e.toString();
        }

        Gson gson = new Gson();
        return gson.toJson(wifiTestInfo);
    }

    private boolean wifiTestInit() {
        wifiTester = new WIFITester();
        return  wifiTester.initial();
    }

    private String RenderHtmlFile()
    {
        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        HtmlReader htmlFile = new HtmlReader(serverConfig.getDocumentRootPath() + File.separator + "html/MyTest2.html");

        return htmlFile.readHtmlFile();
    }

    public static class WIFIScanResultInfo{
        public String SSID;
        public String BSSID;
    }

    public static class WIFITestResult{
        public String result;
        public String message;
    }
    public static class WIFIScanTestResult{
        public String result;
        public int wifiApListCount;
        public List <WIFIScanResultInfo> wifiApList;
        public String scanMsg;
        public String errMsg;
    }

    public static class WIFITestInfo{
        public boolean isWifiSupported;
        public boolean isWifiEnable;
        public String localIP;
        public String enableStatusMsg;
        public boolean isNetConnection;
        public boolean  isNetSystemAvailable;
        public boolean webTestBaidu;
        public boolean scanWifiResult;
        public int wifiApListCount;
        public List <WIFIScanResultInfo> wifiApList;
        public String scanMsg;
        public boolean connectResult;
        public String connectMsg;
        public String errMsg;
    }

    //=========================================== WIFI Tester ===============================
    private static class WIFITester{
        private DeviceWiFiManager deviceWiFiManager = null;
        private final List<ScanResult> mWiFiList = new ArrayList<>();
        private boolean bSConnectResult = false;
        private boolean bScanResult = false;;
        private boolean isScanFinished = false;
        private String mIpAddress = "0.0.0.0";
        private String getIPAddress = "NotDefined";
        private Handler mHandler;
        private HandlerThread mHandlerThread;
        private boolean bConnectTest = false;
        private boolean isbConnectTestFinished = false;

        public static final String SHARED_ACC_PW_SPLIT_SYMBOL = "\tAccPw\t";

        private boolean initial()
        {
            deviceWiFiManager = DeviceWiFiManager.getInstance(mainService.getApplicationContext());

            if(null == deviceWiFiManager)
                return false;

            //webViewBrowser = new WebView(mainService.getApplicationContext());

            deviceWiFiManager.setListener(new DeviceWiFiManager.WiFiListener() {
                @Override
                public void onScanSuccess(List<ScanResult> scanResult) {
                    LOGGER.log(Level.INFO, "DeviceWiFiManager.onScanSuccess");
                    bScanResult = true;
                    mWiFiList.clear();
                    mWiFiList.addAll(scanResult);
                    isScanFinished = true;
                }

                @Override
                public void onScanFailed() {
                    bScanResult = false;
                    LOGGER.log(Level.INFO, "DeviceWiFiManager.onScanFailed");
                    isScanFinished = true;
                }

                @Override
                public void onConnectSuccessful() {
                    LOGGER.log(Level.INFO, "DeviceWiFiManager.onConnectSuccessful");
                    Runnable runnable = new Runnable() {
                        int retryCount = 0;

                        @Override
                        public void run() {
                            mIpAddress = deviceWiFiManager.getIP();
                            LOGGER.log(Level.INFO, "IpAddress:" + mIpAddress);
                            if (mIpAddress.equalsIgnoreCase("0.0.0.0")) {
                                if (retryCount > 20) {
                                    retryCount = 0;
                                    mHandler.removeCallbacks(this);
                                    isbConnectTestFinished = true;
                                    bSConnectResult = false;
                                } else {
                                    retryCount++;
                                    mHandler.postDelayed(this, 1000);
                                }
                            } else {
                                getIPAddress = mIpAddress;
                                isbConnectTestFinished = true;
                                bSConnectResult = true;
                            }
                        }
                    };
                    mHandler.post(runnable);
                }

                @Override
                public void onConnectFailed() {
                    bSConnectResult = false;
                    LOGGER.log(Level.INFO, "DeviceWiFiManager.onConnectFailed");
                    isbConnectTestFinished = true;
                }
            });

            mHandlerThread = new HandlerThread("WiFiHandlerThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            return true;
        }

        private String getLocalIP(){
            return deviceWiFiManager.getIP();
        }

        private boolean enableWifi(boolean enable){
            if(null == deviceWiFiManager)
                return false;

            return  deviceWiFiManager.setWifiEnabled(enable);
        }

        private boolean scanWifi(){
            if(null == deviceWiFiManager || !isWifiEnable())
                return false;

            return  deviceWiFiManager.startScan();
        }

        private boolean startPingTest(String pingIpAddress) {
            if(!isValidIPAddress(pingIpAddress))
                return false;

            Log.d("ping test start", "target ip = " + pingIpAddress);
            String result = null;
            int status = 100;
            try {
                //String ip = "www.baidu.com";// ping 的地址，可以换成任何一种可靠的外网
                Process p = Runtime.getRuntime().exec("ping -c 3 -w 100 " + pingIpAddress);// ping网址3次
                // 读取ping的内容，可以不加
                InputStream input = p.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(input));
                StringBuilder stringBuffer = new StringBuilder();
                String content = "";
                while ((content = in.readLine()) != null) {
                    stringBuffer.append(content);
                }

                Log.d("------ping-----", "result content : " + stringBuffer.toString());
                // ping的状态
                status = p.waitFor();
                if (status == 0) {
                    result = "success";
                    return true;
                } else {
                    result = "failed";
                }
            } catch (IOException e) {
                result = "IOException" + e.toString();
            } catch (InterruptedException e) {
                result = "InterruptedException " + e.toString();
            } finally {
                Log.d("----result---", "result = " + result + ", status = " + status);
            }

            Log.d("ping test finished", "target ip = " + pingIpAddress);

            return false;
        }

        private boolean isWifiEnable() {
            if(null == deviceWiFiManager)
                return false;

            return deviceWiFiManager.isWiFiEnabled();
        }

        private boolean isWifiSupported() {
            if(null == deviceWiFiManager)
                return false;

            return deviceWiFiManager.isWiFiSupported();
        }

        public boolean isValidIPAddress(String ip) {
            // If the IP address is empty
            // return false
            if ((ip == null) || (ip.trim().equals(""))) {
                return false;
            }

            // Regex for digit from 0 to 255.
            String zeroTo255
                    = "(\\d{1,2}|(0|1)\\"
                    + "d{2}|2[0-4]\\d|25[0-5])";

            // Regex for a digit from 0 to 255 and
            // followed by a dot, repeat 4 times.
            // this is the regex to validate an IP address.
            String regex
                    = zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255;

            // Compile the ReGex
            Pattern p = Pattern.compile(regex);

            // Pattern class contains matcher() method
            // to find matching between given IP address
            // and regular expression.
            Matcher m = p.matcher(ip);

            // Return if the IP address
            // matched the ReGex
            return m.matches();
        }

        //https://blog.csdn.net/pzkj0079/article/details/127360842
        private boolean isNetConnection(Context context) {
            if (null == context)
                return false;

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if(null == networkInfo)
                return false;

            boolean connected = networkInfo.isConnected();

            if(connected) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                    return true;
            }
            else
                return false;

            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private boolean isNetSystemAvailable(Context context) {
            boolean isAvailable = false;

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities networkCapabilities = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            }

            if(null != networkCapabilities) {
                isAvailable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }

            return isAvailable;
        }

        private boolean startWebTest(String testUrl) {
            HttpURLConnection httpURLConnection;
            int connectState = -1;
            int retryCounts = 0;
            boolean isNetOnline = false;

            while (retryCounts < 2) {
                try{
                    URL url = new URL(testUrl);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    connectState = httpURLConnection.getResponseCode();

                    if (connectState == 200) {
                        isNetOnline = true;
                        break;
                    }
                }
                catch (Exception e) {
                    retryCounts++;
                    LOGGER.log(Level.INFO, "isNetOnline URL(" + testUrl + ")不可用，连接第 " + retryCounts + " 次");
                }
                finally {
                    LOGGER.log(Level.INFO, "isNetOnline counts: " + retryCounts + ", state: " + connectState);
                }
            }//end of while loop

            return isNetOnline;
        }//end of startWebTest

        private boolean startConnectTest(final String BSSID) {
            if(null == deviceWiFiManager || !isWifiEnable())
                return false;

//            mWiFiList.clear();
            isScanFinished = false;
            bSConnectResult =false;
            scanWifi();
            for (int i = 0; i < 6; ++i) {
                if(isScanFinished){
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            ScanResult selectedAp = getSelectFromScanList(BSSID);

            if(null == selectedAp) {
                isbConnectTestFinished = true;
                bSConnectResult = false;
                LOGGER.log(Level.INFO, "Could not found this AP --> " + BSSID);

                return false;
            }

            return startConnectTest(selectedAp, null, null);
        }

        private ScanResult getSelectFromScanList(String BSSID) {
            if (mWiFiList.isEmpty())
                return null;

            for (ScanResult result: mWiFiList) {
                if(result.BSSID.equalsIgnoreCase(BSSID))
                    return result;
            }

            return null;
        }

        private boolean startConnectTest(final ScanResult selectedAp, final String account, final String password){
            if(null == deviceWiFiManager || !isWifiEnable())
                return false;

            LOGGER.log(Level.INFO,"Ap Name: " + selectedAp.SSID);
            LOGGER.log(Level.INFO,"Capabilities: " + selectedAp.capabilities);

            //check if this AP is exist
            WifiConfiguration wifiConfiguration = deviceWiFiManager.isExist(selectedAp.SSID);
            if(null != wifiConfiguration) {
                LOGGER.log(Level.INFO,"Ap [" + selectedAp.SSID + "] has existed!");
                return deviceWiFiManager.connect(wifiConfiguration);
            }

            final SharedPreferences sp = mainService.getSharedPreferences("WiFiAp", MODE_PRIVATE);
            String storedAccount = "";
            String storedPassword = "";

            final boolean isEnterprise = DeviceWiFiManager.isEnterprise(selectedAp);
            final int securityMode = DeviceWiFiManager.getSecurityMode(selectedAp);
            final String ssid = selectedAp.SSID;

            //开放模式连接
            if (securityMode == DeviceWiFiManager.SECURITY_MODE_OPEN) {
                //Toast.makeText(mainService.getApplicationContext(), "connect_failed", Toast.LENGTH_LONG).show();
                bConnectTest = deviceWiFiManager.connect(ssid);
            }

            if (sp != null) {
                String accPwString = sp.getString(ssid, "");
                if ((accPwString != null) && (!accPwString.equalsIgnoreCase(""))) {
                    storedAccount = accPwString.split(SHARED_ACC_PW_SPLIT_SYMBOL)[0];
                    storedPassword = accPwString.split(SHARED_ACC_PW_SPLIT_SYMBOL)[1];

                    LOGGER.log(Level.INFO,"storedAccount: " + storedAccount);
                    LOGGER.log(Level.INFO,"storedPassword: " + storedPassword);
                }
            }

            if(null != account && null != password && null != sp) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(ssid, account + SHARED_ACC_PW_SPLIT_SYMBOL + password);
                editor.apply();
            }

            if (isEnterprise) {
                bConnectTest = deviceWiFiManager.connect(ssid, account, password);
            } else {
                //bConnectTest = deviceWiFiManager.connect(ssid, "12345678", securityMode);
                connectWifi(ssid, "12345678");
            }

            return bConnectTest;
        }

        //https://juejin.cn/post/7195742069997322301
        private void connectWifi(String ssid, String pwd) {
            // android 10以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //step1-创建建议列表
                WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase((pwd))
                        .setIsAppInteractionRequired(true)
                        .build();

                List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
                suggestionsList.add(suggestion);
                //WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int status = deviceWiFiManager.addNetworkSuggestions(suggestionsList);
                // step2-添加建议成功
                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(pwd)
                            .build();

                    NetworkRequest request = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(wifiNetworkSpecifier)
                            .build();

                    ConnectivityManager connectivityManager = (ConnectivityManager) mainService.getSystemService(Context.CONNECTIVITY_SERVICE);

                    ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            LOGGER.log(Level.INFO, "connectWifi.onAvailable");
                            isbConnectTestFinished = true;
                            bSConnectResult = true;
                            super.onAvailable(network);
                        }

                        @Override
                        public void onUnavailable() {
                            LOGGER.log(Level.INFO, "connectWifi.onUnavailable");
                            isbConnectTestFinished = true;
                            bSConnectResult = false;
                            super.onUnavailable();
                        }
                    };
                    // step3-连接wifi
                    connectivityManager.requestNetwork(request, mNetworkCallback);
                } else {
                    LOGGER.log(Level.INFO, "addNetworkSuggestions error, status = " + status);
                }
            }//end of if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
        }
    }//end of WIFITester class
}

/*
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.sensor.proximity
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.sensor.accelerometer
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.controls
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.faketouch
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.usb.accessory
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.telephony.cdma
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.backup
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.touchscreen
2024-08-27 15:25:24.432  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.touchscreen.multitouch
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.print
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.ethernet
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.activities_on_secondary_displays
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.voice_recognizers
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.picture_in_picture
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.audio.low_latency
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.vulkan.deqp.level
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.cant_save_state
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.opengles.aep
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.bluetooth
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.autofocus
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.telephony.gsm
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.telephony.ims
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.incremental_delivery
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.usb.host
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.audio.output
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.verified_boot
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.flash
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.front
2024-08-27 15:25:24.433  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.screen.portrait
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.sensor.stepdetector
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.home_screen
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.microphone
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.autofill
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.securely_removes_users
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.bluetooth_le
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.touchscreen.multitouch.jazzhand
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.app_widgets
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.input_methods
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.sensor.light
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.vulkan.version
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.companion_device_setup
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.device_admin
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.wifi.passpoint
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.screen.landscape
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.ram.normal
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.managed_users
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.webview
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.sensor.stepcounter
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.capability.manual_post_processing
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.any
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.capability.raw
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.vulkan.compute
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.connectionservice
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.touchscreen.multitouch.distinct
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.location.network
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.cts
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.capability.manual_sensor
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.app_enumeration
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.camera.level.full
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.wifi.direct
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.live_wallpaper
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.ipsec_tunnels
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.location.gps
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.software.midi
2024-08-27 15:25:24.434  7011-7048  DEBUG_DeviceWiFiManager ro.polak.webserver                   D  available feature: android.hardware.wifi
* */