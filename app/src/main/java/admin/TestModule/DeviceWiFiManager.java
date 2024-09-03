package admin.TestModule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * if you need to use this class to check/config Wifi features in your application
 * you will need to add the following permissions in AndroidManifest.xml
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 */

public class DeviceWiFiManager {
    public static final int SECURITY_MODE_OPEN = -1;
    public static final int SECURITY_MODE_WEP = 0;
    public static final int SECURITY_MODE_WPA = 1;
    public static final int SECURITY_MODE_WPA2 = 2;
    public static final int SECURITY_MODE_WPA3 = 3;

    private final String TAG = "DEBUG_DeviceWiFiManager";
    private final String FEATURE_WIFI = "android.hardware.wifi";

    private WiFiListener mListener = null;

    private volatile static DeviceWiFiManager mInstance;
    private final WifiManager mWiFiManager;
    private final Context mContext;
    private BroadcastReceiver mWiFiScanReceiver = null;
    private BroadcastReceiver mWiFiConnectReceiver = null;
    private ArrayList<ScanResult> mScanResult;

    public interface WiFiListener {
        void onScanSuccess(List<ScanResult> scanResults);

        void onScanFailed();

        void onConnectSuccessful();

        void onConnectFailed();
    }

    private DeviceWiFiManager(Context context) {
        mContext = context;
        mWiFiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mScanResult = new ArrayList<>();
    }

    public void destroyed() {
        if (mWiFiConnectReceiver != null) {
            try {
                mContext.unregisterReceiver(mWiFiConnectReceiver);
                mWiFiConnectReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mWiFiScanReceiver != null) {
            try {
                mContext.unregisterReceiver(mWiFiScanReceiver);
                mWiFiScanReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mInstance != null) {
            mInstance = null;
        }
    }

    public static DeviceWiFiManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DeviceWiFiManager.class) {
                if (mInstance == null) {
                    mInstance = new DeviceWiFiManager(context.getApplicationContext());
                }
            }
        }

        return mInstance;
    }

    public void setListener(WiFiListener listener) {
        if (listener != null) {
            mListener = listener;
        }
    }

    public boolean isWiFiSupported() {
        boolean result = false;
        PackageManager pm = mContext.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null) {
                //Log.d(TAG, "available feature: " + info.name);
                if (info.name.equalsIgnoreCase(FEATURE_WIFI)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public boolean isWiFiEnabled() {
        if (isWiFiSupported()) {
            return mWiFiManager.isWifiEnabled();
        } else {
            return false;
        }
    }

    /**
     * @param enabled true for enable, false for disable
     * @return this setting is successful or not
     */
    public boolean setWifiEnabled(boolean enabled) {
        return mWiFiManager.setWifiEnabled(enabled);
    }

    public boolean startScan() {
        boolean result;
        if (mWiFiScanReceiver == null) {
            mWiFiScanReceiver = new BroadcastReceiver() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        List<ScanResult> receivedList;
                        receivedList = mWiFiManager.getScanResults();
                        mScanResult.clear();
                        mScanResult.addAll(filterScanResult(receivedList));
                        if (mListener != null) {
                            mListener.onScanSuccess(mScanResult);
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onScanFailed();
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            mContext.registerReceiver(mWiFiScanReceiver, intentFilter);
        }

        /*
         * WiFiManager.startScan() has the limit call frequency.
         * if you call this function too many times in a period, you will always receive the false return.
         * if it returns true that just means you invoke the function fine.
         * you should check the scan result in BroadcastReceiver.
         */
        result = mWiFiManager.startScan();
        return result;
    }

    private ArrayList<ScanResult> filterScanResult(List<ScanResult> receivedList) {
        HashMap<String, ScanResult> resultMap = new HashMap<>();
        String ssid, bssid;
        for (int i = 0; i < receivedList.size(); i++) {
            ScanResult result = receivedList.get(i);
            ssid = result.SSID;
            bssid = result.BSSID;
            if (ssid.equalsIgnoreCase("")) {
                continue;
            }
//            Log.d(TAG, "scanResult==SSID[" + ssid +
//                    "]\t BSSID[" + bssid + "]");
            if (!resultMap.containsKey(bssid)) {
                resultMap.put(bssid, receivedList.get(i));
            }
        }
        return new ArrayList<>(resultMap.values());
    }

    public void stopScan() {
        if (mWiFiScanReceiver == null) {
            return;
        }
        try {
            mContext.unregisterReceiver(mWiFiScanReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mWiFiScanReceiver = null;
    }

    public List<ScanResult> getScanResults() {
        return mScanResult;
    }

    /**
     * if your wifi network is enterprise capability, you'll need to use this method to access.
     *
     * @param ssid     AccessPoint ssid
     * @param username account for login
     * @param password password for login
     */
    public boolean connect(String ssid, String username, String password) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        wifiConfig.SSID = "\"" + ssid + "\"";
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);

        enterpriseConfig.setIdentity(username);
        enterpriseConfig.setPassword(password);
        enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
        enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);

        wifiConfig.enterpriseConfig = enterpriseConfig;
        return connect(wifiConfig);
    }

    public boolean connect(String ssid, String password, int securityMode) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";

        switch (securityMode) {
            case SECURITY_MODE_WEP:
                wifiConfig.wepKeys[0] = "\"" + password + "\"";
                wifiConfig.wepTxKeyIndex = 0;
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                break;

            case SECURITY_MODE_WPA:
            case SECURITY_MODE_WPA2:
            case SECURITY_MODE_WPA3:
                wifiConfig.preSharedKey = "\"" + password + "\"";
                break;
        }
        return connect(wifiConfig);
    }

    /**
     * for OPEN wifi network use.
     */
    public boolean connect(String ssid) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return connect(wifiConfig);
    }

    public boolean connect(WifiConfiguration wifiConfig) {
        boolean connected = false;
        final int netId = mWiFiManager.addNetwork(wifiConfig);
        Log.d(TAG, "addNetwork return netId: " + netId);
        if (netId != -1) {
            mWiFiManager.disconnect();
            if (mWiFiConnectReceiver == null) {
                mWiFiConnectReceiver = new BroadcastReceiver() {
                    boolean isAuthenticating = false;
                    boolean isConnecting = false;

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equalsIgnoreCase(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                            NetworkInfo.DetailedState state = networkInfo.getDetailedState();
                            Log.d(TAG, "network state: " + state.name());
                            if (state == NetworkInfo.DetailedState.CONNECTED) {
                                if (isAuthenticating) {
                                    mListener.onConnectSuccessful();
                                    mContext.unregisterReceiver(this);
                                    mWiFiConnectReceiver = null;
                                }
                            } else if (state == NetworkInfo.DetailedState.CONNECTING) {
                                isConnecting = true;
                            } else if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
                                isAuthenticating = true;
                            } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
                                if (isConnecting) {
                                    mListener.onConnectFailed();
                                    isConnecting = false;
                                    isAuthenticating = false;
                                }
                            }
                        }
                    }
                };
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                mContext.registerReceiver(mWiFiConnectReceiver, intentFilter);
            }
            connected = mWiFiManager.enableNetwork(netId, true);
            if (!connected) {
                Log.d(TAG, "enableNetwork failed.");
            }
        }

        return connected;
    }

    public String getIP() {
        int ipInt = mWiFiManager.getDhcpInfo().ipAddress;
        if (ipInt == 0) {
            return "0.0.0.0";
        }
        Long unsignedLong;
        String ipv4;
        long[] unsignedByte = new long[4];
        unsignedLong = Integer.toUnsignedLong(ipInt);
        for (int i = 0; i < 4; i++) {
            unsignedByte[i] = (unsignedLong >> (i * 8)) & 0xFF;
        }
//        ipv4 = unsignedByte[3] + "." +
//                unsignedByte[2] + "." +
//                unsignedByte[1] + "." +
//                unsignedByte[0];
        ipv4 = unsignedByte[0] + "." +
                unsignedByte[1] + "." +
                unsignedByte[2] + "." +
                unsignedByte[3];

        return ipv4;
    }

    /**
     * @return The security of a given {@link ScanResult}.
     */
    //https://baike.baidu.com/item/WPA/33467?fr=ge_ala            Wi-Fi四种安全协议 - WEP、WPA、WPA2、WPA3
    public static int getSecurityMode(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {"WEP", "WPA", "WPA2", "WPA3"};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return i;
            }
        }
        return SECURITY_MODE_OPEN;
    }

    /**
     * check the scanResult is enterprise capability or not.
     * if it is, call the function connect(ssid, username, password) to access.
     */
    public static boolean isEnterprise(ScanResult scanResult) {
        String capabilities = scanResult.capabilities;
        return (capabilities.contains("IEEE8021X")) || (capabilities.contains("-EAP-"));
    }

    //added by Derek
    //https://blog.csdn.net/zhangbijun1230/article/details/83006734
    //always return on my MI phone, can't get the reason now.
    public WifiConfiguration isExist(String SSID) {
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configurations = mWiFiManager.getConfiguredNetworks();

        for(WifiConfiguration configuration : configurations)
            if(configuration.SSID.equalsIgnoreCase("\"" + SSID + "\""))
                return configuration;

        return null;
    }

    public int addNetworkSuggestions(List<WifiNetworkSuggestion> suggestionsList){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return mWiFiManager.addNetworkSuggestions(suggestionsList);
        }
        else
            return -100;
    }
}