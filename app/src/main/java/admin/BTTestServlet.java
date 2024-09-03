/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package admin;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import admin.logic.HtmlReader;
import ro.polak.http.configuration.ServerConfig;
import ro.polak.http.exception.ServletException;
import ro.polak.http.servlet.HttpServlet;
import ro.polak.http.servlet.HttpServletRequest;
import ro.polak.http.servlet.HttpServletResponse;
import ro.polak.webserver.MainService;

/**
 * Admin panel front page.
 */
public class BTTestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BTTestServlet.class.getName());
    private static MainService mainService;
    private final BluetoothTester bluetoothTester = new BluetoothTester();
    final int ScanTimeout = 60;

    @Override
    public void destroy() {
        LOGGER.log(Level.INFO, "Entry BTTestServlet destroy process");
        super.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, InterruptedException {
        response.getWriter().print("BTTestServlet thread ID = " + Thread.currentThread().getId());

        LOGGER.log(Level.INFO, "Entry BTTestServlet service process");

        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        mainService = (MainService)serverConfig.getServiceContext(); //getServiceContext --> ro.polak.webserver.MainService@6103e86
        if(null == mainService){
            response.getWriter().print("MainService object is null!!");

            return;
        }

        bluetoothTester.initial();
        response.getWriter().print(outputBTInfo(mainService));
        bluetoothTester.onDestroy();

        response.getWriter().print(RenderHtmlFile());
    }

//    private void startBTTest(MainService ms){
//        Intent intent = new Intent();
//        intent.setClass(ms, BluetoothTester.class);
//        ms.startService(intent);
//    }

    private String outputBTInfo(MainService ms) throws InterruptedException {
        String ScanMsg = "Task not started";
        JSONObject btObject = new JSONObject();
        boolean bBTEnable = false;
        String strEnableStatus = "Enable BT fail";
        Map<String, String> queryingPairedDevices = new HashMap<>();
        String strConnectResult = "not test";

        if (bluetoothTester.isBTSupported) {
            bBTEnable = bluetoothTester.isBTEnabled;
            strEnableStatus = bluetoothTester.enableBluetooth();
            queryingPairedDevices = bluetoothTester.queryingPairedDevices();

            //start scan action by user defined message
            //Intent intentStartBTScan = new Intent("start.my.bt.scan.task@");
            //ms.sendBroadcast(intentStartBTScan);
            bluetoothTester.startDiscoveringDevices();

            //wait discoveringDevices process finished
            for (int i = 0; i < ScanTimeout; ++i) {
                if (bluetoothTester.isDiscoveryFinished) {
                    ScanMsg = "Scan finished in " + i + " seconds";
                    //strConnectResult = bluetoothTester.connectToBluetoothDevice("C4:DF:39:8C:08:14");
                    break;
                }

                Thread.sleep(1000);

                if (i + 1 >= ScanTimeout) {
                    ScanMsg = "Scan process time out!";
                }
            }//end of for

            strConnectResult = bluetoothTester.connectToBluetoothDevice("C4:DF:39:8C:08:14");
            for (int i = 0; i < ScanTimeout; ++i) {
                if(bluetoothTester.isBluetoothGattFuncFinished){
                    strConnectResult = "Connect test finished in " + i + " seconds";
                    break;
                }

                Thread.sleep(1000);

                if (i + 1 >= ScanTimeout) {
                    strConnectResult = "Connect test time out!";
                }
            }
        }//end of if
        else
            ScanMsg = "BT not support!";

        try {
            // 第一个键phone的值是数组，所以需要创建数组对象
            //JSONArray phone = new JSONArray();
            //phone.put("12345678").put("87654321");
            btObject.put("isBluetoothSupported", bluetoothTester.isBTSupported);
            if(bluetoothTester.isBTSupported) {
                btObject.put("isBluetoothEnabled", bBTEnable);
                btObject.put("EnableBluetooth", strEnableStatus);
                btObject.put("IsDiscoveryFinished", bluetoothTester.isDiscoveryFinished);

                if (!queryingPairedDevices.isEmpty()) {
                    // 键PairedDevices的值是对象，所以又要创建一个对象
                    JSONObject pDevices = new JSONObject();
                    for (String key : queryingPairedDevices.keySet()) {
                        pDevices.put(key, queryingPairedDevices.get(key));
                    }
                    btObject.put("pairedDevices", pDevices);
                }

                if (!bluetoothTester.mScannedList.isEmpty()) {
                    JSONObject dDevices = new JSONObject();
                    for (String key : bluetoothTester.mScannedList.keySet()) {
                        //dDevices.put(Objects.requireNonNull(bluetoothTester.mScannedList.get(key)).getName(), key);
                        dDevices.put(key, Objects.requireNonNull(bluetoothTester.mScannedList.get(key)).getName());
                    }
                    btObject.put("Scanned BT device count", bluetoothTester.mScannedList.size());
                    btObject.put("mScannedList", dDevices);
                }
            }//end of if

            btObject.put("ScanResult", ScanMsg);

            btObject.put("Connect to [C4:DF:39:8C:08:14] Status", strConnectResult);
            btObject.put("ConnectResult", bluetoothTester.bConnectResult);
        } catch (JSONException ex) {
            return  ex.toString();
        }

        return btObject.toString();
    }

    private String RenderHtmlFile()
    {
        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        HtmlReader htmlFile = new HtmlReader(serverConfig.getDocumentRootPath() + File.separator + "html/MyTest2.html");

        return htmlFile.readHtmlFile();
    }


    //=========================================== BTTester ===============================
    private static class BluetoothTester{
        private final Map<String, BluetoothDevice> mScannedList = new HashMap<>();
        private BluetoothDevice mConnectedBtDevice; //device want to connect
        private boolean isDiscoveryFinished = false;
        private BluetoothAdapter mBluetoothAdapter = null;
        private boolean isBTSupported = false;
        private boolean isBTEnabled = false;
        private BluetoothBroadcastReceiver mReceiver = null;
        private BluetoothGatt btGatt;
        private boolean isBluetoothGattFuncFinished = false;
        private boolean bConnectResult = false;

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private void initial()
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(mBluetoothAdapter != null)
                isBTSupported = true;
            else
                return;

            isBTEnabled = mBluetoothAdapter.isEnabled();
            mReceiver = new BluetoothBroadcastReceiver();

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            String startBTScanCmd = "start.my.bt.scan.task@";
            filter.addAction(startBTScanCmd);
            mainService.registerReceiver(mReceiver, filter);

            //discoveringDevices();
        }

        public void cancelBTDiscovery(){
            mBluetoothAdapter.cancelDiscovery();
            isDiscoveryFinished = false;
            mScannedList.clear();
        }

        public void onDestroy() {
            if(null != mReceiver){
                mainService.unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        }

        public String enableBluetooth(){
            if(!isBTSupported)
                return  "BT not supported!";

            if(isBTEnabled)
                return  "BT has enabled";

            try{
                mBluetoothAdapter.enable();

                Thread.sleep(3000);
                isBTEnabled = mBluetoothAdapter.isEnabled();

                if(isBTEnabled)
                    return  "BT enabled successfully";
                else
                    return  "BT enabled failed!";
            }
            catch (Exception e) {
                return e.toString();
            }
        }

        //Querying paired devices
        public Map<String, String> queryingPairedDevices() {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            Map<String, String> mapPairedDevices = new HashMap<>();

            if (!pairedDevices.isEmpty()) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    mapPairedDevices.put(device.getName(), device.getAddress());
                }
            }

            return mapPairedDevices;
        }

        //Discovering devices
        public void startDiscoveringDevices() {
            if(!isBTSupported || !isBTEnabled)
                return;

            isDiscoveryFinished = false;
            if(mBluetoothAdapter.isDiscovering()) {
                //Toast.makeText(getApplicationContext(),"BluetoothBroadcastReceiver::isDiscovering --> cancel it", Toast.LENGTH_SHORT).show();
                mBluetoothAdapter.cancelDiscovery();
            }

            mBluetoothAdapter.startDiscovery();
        }

        private  String connectToBluetoothDevice(String macAddress){
            if(!isBTEnabled)
                return  "Bluetooth not enabled!";

            if(!BluetoothAdapter.checkBluetoothAddress(macAddress.toUpperCase()))
                return "Invalid MAC address!";

            //https://blog.csdn.net/weixin_43242410/article/details/139294238
            BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(macAddress.toUpperCase());

            if(null == btDevice){
                return "Get remote bluetooth device fail!";
            }
            mConnectedBtDevice = btDevice;

            btGatt = btDevice.connectGatt(mainService.getApplicationContext(), false, btGattCallback);

            return "Success to run BluetoothGatt function";
        }

        /*
        device不存在的log：
        2024-08-22 16:04:35.457  5897-5963  BluetoothGatt           ro.polak.webserver                   D  connect() - device: C4:DF:39:8C:08:24, auto: false
        2024-08-22 16:04:35.457  5897-5963  BluetoothGatt           ro.polak.webserver                   D  registerApp()
        2024-08-22 16:04:35.458  5897-5963  BluetoothGatt           ro.polak.webserver                   D  registerApp() - UUID=5be6d653-df13-4f8d-950b-efb8f73078bf
        2024-08-22 16:04:35.463  5897-5919  BluetoothGatt           ro.polak.webserver                   D  onClientRegistered() - status=0 clientIf=6
        2024-08-22 16:05:05.482  5897-5919  BluetoothGatt           ro.polak.webserver                   D  onClientConnectionState() - status=133 clientIf=6 device=C4:DF:39:8C:08:24
        2024-08-22 16:05:05.483  5897-5919  admin.BTTestServlet     ro.polak.webserver                   I  BluetoothGattCallback.onConnectionStateChange
        2024-08-22 16:05:05.483  5897-5919  BluetoothGatt           ro.polak.webserver                   D  discoverServices() - device: C4:DF:39:8C:08:24
        2024-08-22 16:05:05.484  5897-5919  admin.BTTestServlet     ro.polak.webserver                   I  BluetoothGattCallback.STATE_DISCONNECTE

        device存在的log：
        2024-08-22 16:08:02.561  6026-6067  BluetoothGatt           ro.polak.webserver                   D  connect() - device: C4:DF:39:8C:08:14, auto: false
        2024-08-22 16:08:02.561  6026-6067  BluetoothGatt           ro.polak.webserver                   D  registerApp()
        2024-08-22 16:08:02.562  6026-6067  BluetoothGatt           ro.polak.webserver                   D  registerApp() - UUID=b5b6a3f8-ba02-45db-831e-a7859e6b03d2
        2024-08-22 16:08:02.566  6026-6044  BluetoothGatt           ro.polak.webserver                   D  onClientRegistered() - status=0 clientIf=6
        2024-08-22 16:08:05.771  6026-6044  BluetoothGatt           ro.polak.webserver                   D  onClientConnectionState() - status=0 clientIf=6 device=C4:DF:39:8C:08:14
        2024-08-22 16:08:05.772  6026-6044  admin.BTTestServlet     ro.polak.webserver                   I  BluetoothGattCallback.onConnectionStateChange
        2024-08-22 16:08:05.772  6026-6044  BluetoothGatt           ro.polak.webserver                   D  discoverServices() - device: C4:DF:39:8C:08:14
        2024-08-22 16:08:05.773  6026-6044  admin.BTTestServlet     ro.polak.webserver                   I  BluetoothGattCallback.STATE_CONNECTED
        2024-08-22 16:08:05.777  6026-6044  BluetoothGatt           ro.polak.webserver                   D  onSearchComplete() = Device=C4:DF:39:8C:08:14 Status=0
        2024-08-22 16:08:05.778  6026-6044  admin.BTTestServlet     ro.polak.webserver                   I  BluetoothGattCallback.onServicesDiscovered
        */
        private final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onConnectionStateChange");
                btGatt.discoverServices();
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    bConnectResult = true;
                    LOGGER.log(Level.INFO, "BluetoothGattCallback.STATE_CONNECTED");
                }
                else{
                    bConnectResult = false;
                    LOGGER.log(Level.INFO, "BluetoothGattCallback.STATE_DISCONNECTED");
                }

                isBluetoothGattFuncFinished = true;
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onServicesDiscovered");
                super.onServicesDiscovered(gatt, status);
            }

            /*
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onCharacteristicRead");
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onCharacteristicWrite");
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onCharacteristicChanged");
                super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onDescriptorRead");
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onDescriptorWrite");
                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                LOGGER.log(Level.INFO, "BluetoothGattCallback.onReadRemoteRssi");
                super.onReadRemoteRssi(gatt, rssi, status);
            }
            */
        };

        private boolean connectToBluetoothDevice(BluetoothDevice selectedDevice) {
            //List<BluetoothDevice> deviceList;
            //if(mScannedList.size() > 0)
            //    deviceList = new ArrayList<>(mScannedList.values());

            if (selectedDevice == null) {
                return false;
            }

            boolean result = false;

            if (selectedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                result = true;
            } else {
                try {
                    Method createBond = BluetoothDevice.class.getMethod("createBond");
                    createBond.invoke(selectedDevice);
                    result = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.log(Level.INFO, "Can't pair with the device " + selectedDevice.getName());
                }
            }
            if (result) {
                mConnectedBtDevice = selectedDevice;
            }

            return result;
        }

        public class BluetoothBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice btDevice;

                //Toast.makeText(getApplicationContext(),"BluetoothTester::BluetoothBroadcastReceiver::onReceive", Toast.LENGTH_SHORT).show();
                switch (Objects.requireNonNull(action)){
                    case BluetoothDevice.ACTION_FOUND:
                        //Toast.makeText(getApplicationContext(),"ACTION_FOUND", Toast.LENGTH_SHORT).show();
                        LOGGER.log(Level.INFO, "BluetoothDevice.ACTION_FOUND");
                        btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if ((btDevice == null) || (btDevice.getName() == null)) {
                            return;
                        }
                        //Toast.makeText(getApplicationContext(),"ACTION_FOUND --> name: " + btDevice.getName() + ", Address: " + btDevice.getAddress(), Toast.LENGTH_SHORT).show();
                        if (btDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                            if (!mScannedList.containsKey(btDevice.getAddress())) {
                                mScannedList.put(btDevice.getAddress(), btDevice);
                            }
                        }
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        //Toast.makeText(getApplicationContext(),"ACTION_DISCOVERY_FINISHED", Toast.LENGTH_SHORT).show();
                        LOGGER.log(Level.INFO, "BluetoothDevice.ACTION_DISCOVERY_FINISHED");
                        isDiscoveryFinished = true;
                        if (mConnectedBtDevice != null) {
                            return;
                        }
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        //Toast.makeText(getApplicationContext(),"ACTION_STATE_CHANGED", Toast.LENGTH_SHORT).show();
                        LOGGER.log(Level.INFO, "BluetoothDevice.ACTION_STATE_CHANGED");
                        int btAdapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                        if (btAdapterState == BluetoothAdapter.STATE_ON) {
                            //removeAllBonds();
                        }
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        LOGGER.log(Level.INFO, "BluetoothDevice.ACTION_DISCOVERY_STARTED");
                        isDiscoveryFinished = false;
                        mScannedList.clear();
                        //mBluetoothAdapter.startDiscovery();
                        //Toast.makeText(getApplicationContext(),"ACTION_DISCOVERY_STARTED", Toast.LENGTH_SHORT).show();
                        break;

                    case "start.my.bt.scan.task@":
                        //Toast.makeText(getApplicationContext(),startBTScanCmd, Toast.LENGTH_SHORT).show();
                        LOGGER.log(Level.INFO, "BluetoothDevice.start.my.bt.scan.task@");
                        isDiscoveryFinished = false;
                        mScannedList.clear();
                        //mBluetoothAdapter.startDiscovery();
                        startDiscoveringDevices();
                        break;
                } //end of switch
            }
        }//end of BluetoothBroadcastReceiver class
    }//end of BluetoothTester class
}
