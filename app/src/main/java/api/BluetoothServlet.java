/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package api;

import static api.logic.APIResponse.MEDIA_TYPE_APPLICATION_JSON;

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

import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class BluetoothServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BluetoothServlet.class.getName());
    private static MainService mainService;
    private final BluetoothTester bluetoothTester = new BluetoothTester();
    final int ScanTimeout = 60;

    private static final String BT_ACTION = "action";
    private static final String BT_ADDRESS = "address";
    public static final String CONNECT = "connect";
    public static final String SEARCH = "search";
    private static final String ENABLE = "enable";
    private static final String INIT = "init";

    @Override
    public void destroy() {
        LOGGER.log(Level.INFO, "BluetoothServlet Destroy ");
        super.destroy();

    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, InterruptedException {

        LOGGER.log(Level.INFO, "Entry BluetoothServlet service process");
        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        mainService = (MainService)serverConfig.getServiceContext(); //getServiceContext --> ro.polak.webserver.MainService@6103e86
        if(null == mainService){
            response.getWriter().print("MainService object is null!!");
            return;
        }

        String bt_action = request.getParameter(BT_ACTION);
        String bt_address = request.getParameter(BT_ADDRESS);
        response.setContentType(MEDIA_TYPE_APPLICATION_JSON);
        bluetoothTester.initial();
        if(bt_action!=null && bt_action.equals(ENABLE))
        {
            BluetoothResult testResult = new BluetoothResult();
            testResult.result = "Fail";
            testResult.message = "enable bluetooth fail";
            if(bluetoothTester.enableBluetooth())
            {
                testResult.result = "Pass";
                testResult.message = "enable bluetooth successfully";
            }
            Gson gson = new Gson();
            response.getWriter().print(gson.toJson(testResult));
        }

        if(bt_action!=null && bt_action.equals(INIT))
        {

            response.getWriter().print(outputBTInfo(mainService));
        }

        if(bt_action !=null && bt_action.equals(CONNECT)&& bt_address !=null )
        {
            BluetoothResult testResult = new BluetoothResult();
            Gson gson = new Gson();
            LOGGER.log(Level.INFO, "start connect bluetooth");
            boolean btEnable = bluetoothTester.enableBluetooth();
            if(!btEnable)
            {
                testResult.result = "Fail";
                response.getWriter().print(gson.toJson(testResult));
            }
            else{
                String strConnectResult = "not test";
                String address = bt_address.toUpperCase();
                BluetoothDevice selectDevice = bluetoothTester.getBtDeviceByMacAddress(address);
                boolean createBond = bluetoothTester.createBond(selectDevice);

                for(int i = 0; i < 30; ++i){
                    LOGGER.log(Level.INFO, "wait bonded bluetooth : "+i);
                    if (selectDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                        LOGGER.log(Level.INFO, " bonded successfully ");
                        testResult.result = "Pass";
                        testResult.message = "bonded successfully";
                        strConnectResult = "bonded test finished in " + i + " seconds";
                        break;
                        //  connect test
//                        Thread.sleep(3000);
//                        bluetoothTester.connectToBluetoothDevice(selectDevice);
//                        for (int j = 0; j < ScanTimeout; ++j) {
//                            if(bluetoothTester.isBluetoothGattFuncFinished){
//                                LOGGER.log(Level.INFO, "connectToBluetoothDevice " + j + "seconds");
//                                strConnectResult = "Connect test finished in " + j + " seconds";
//                                break;
//                            }
//
//                            Thread.sleep(1000);
//                            if (j + 1 >= ScanTimeout) {
//                                strConnectResult = "Connect test time out!";
//                            }
                    }
                    Thread.sleep(1000);
                    if (i + 1 >= 30) {
                        testResult.result = "Fail";
                        testResult.message = "createBond test time out!";
                    }
                }
                response.getWriter().print(gson.toJson(testResult));
            }

        }

        if(bt_action !=null && bt_action.equals(SEARCH))
        {
            bluetoothTester.enableReceiver();
            response.getWriter().print(searchBTInfo(mainService));
            bluetoothTester.onDestroy();
        }


//response.getWriter().print(RenderHtmlFile());
    }

//    private void startBTTest(MainService ms){
//        Intent intent = new Intent();
//        intent.setClass(ms, BluetoothTester.class);
//        ms.startService(intent);
//    }
    private String outputBTInfo(MainService ms) throws InterruptedException {
        LOGGER.log(Level.INFO, "start outputBTInfo");
        boolean bBTEnable = false;
        InitOutPutInfo btTestInfo = new InitOutPutInfo();
        if (bluetoothTester.isBTSupported) {
//            bBTEnable = bluetoothTester.enableBluetooth();
            bBTEnable = bluetoothTester.isBTEnabled;
            if(!bBTEnable)
            {
                btTestInfo.isBluetoothSupported = bluetoothTester.isBTSupported;
                btTestInfo.isBluetoothEnabled = bBTEnable;
                btTestInfo.message = "Bluetooth didn't enable";
            }
            else{
                btTestInfo.isBluetoothSupported = bluetoothTester.isBTSupported;
                btTestInfo.isBluetoothEnabled = bBTEnable;
                btTestInfo.message = "Bluetooth already enable";
            }
        }
        else{
            btTestInfo.isBluetoothSupported = bluetoothTester.isBTSupported;
            btTestInfo.isBluetoothEnabled = bBTEnable;
            btTestInfo.message = "BT not support!";
        }
        Gson gson = new Gson();
        return gson.toJson(btTestInfo);
    }


    private String searchBTInfo(MainService ms) throws InterruptedException {
        LOGGER.log(Level.INFO, "start outputBTInfo");
        String message = "No any error occur";
        boolean bBTEnable = false;
        List<BluetoothDeviceInfo> queryingPairedDevices =new ArrayList<>();
        BluetoothTestInfo btTestInfo = new BluetoothTestInfo();
        List<BluetoothDeviceInfo> bluetoothDevicesInfo =new ArrayList<>();
        if (bluetoothTester.isBTSupported) {

            bBTEnable = bluetoothTester.enableBluetooth();
            if(!bBTEnable)
            {
                btTestInfo.isBluetoothSupported = bluetoothTester.isBTSupported;
                btTestInfo.isBluetoothEnabled = bBTEnable;
                btTestInfo.BtDeviceCount = 0;
                btTestInfo.BtDeviceList = null;
                btTestInfo.message = "Bluetooth enable Fail";
                Gson gson = new Gson();
                return gson.toJson(btTestInfo);

            }
            queryingPairedDevices = bluetoothTester.queryingPairedDevices();

            //start scan action by user defined message
            //Intent intentStartBTScan = new Intent("start.my.bt.scan.task@");
            //ms.sendBroadcast(intentStartBTScan);
            bluetoothTester.startDiscoveringDevices();

            //wait discoveringDevices process finished
            for (int i = 0; i < ScanTimeout; ++i) {
                if (bluetoothTester.isDiscoveryFinished) {
                    message = "Scan finished in " + i + " seconds";
                    break;
                }

                Thread.sleep(1000);

                if (i + 1 >= ScanTimeout) {
                    message = "Scan process time out!";
                }
            }//end of for
        }//end of if
        else
            message = "BT not support!";

        if(bluetoothTester.isBTSupported) {
            if (!queryingPairedDevices.isEmpty()) {
                bluetoothDevicesInfo = queryingPairedDevices;
            }

            if (!bluetoothTester.mScannedList.isEmpty()) {
                for (String key : bluetoothTester.mScannedList.keySet()) {
                    BluetoothDeviceInfo bluetoothDeviceInfo = new BluetoothDeviceInfo();
                    bluetoothDeviceInfo.address = key;
                    bluetoothDeviceInfo.deviceName = Objects.requireNonNull(bluetoothTester.mScannedList.get(key)).getName();
                    bluetoothDevicesInfo.add(bluetoothDeviceInfo);
                }
            }
        }//end of if

        btTestInfo.isBluetoothSupported = bluetoothTester.isBTSupported;
        btTestInfo.isBluetoothEnabled = bBTEnable;
        btTestInfo.BtDeviceList = bluetoothDevicesInfo;
        btTestInfo.BtDeviceCount=bluetoothTester.mScannedList.size();
        btTestInfo.message = message;
        Gson gson = new Gson();
        return gson.toJson(btTestInfo);
    }

    private String RenderHtmlFile()
    {
        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        HtmlReader htmlFile = new HtmlReader(serverConfig.getDocumentRootPath() + File.separator + "html/MyTest2.html");

        return htmlFile.readHtmlFile();
    }

    public static class BluetoothDeviceInfo{
        public String deviceName;
        public String address;
    }

    public static class BluetoothResult{
        public String result;
        public String message;
    }

    public static class BluetoothTestInfo{
        public boolean isBluetoothSupported;
        public boolean isBluetoothEnabled;
        public List <BluetoothDeviceInfo>  BtDeviceList;
        public int BtDeviceCount;
        public String message;

    }

    public static class InitOutPutInfo{
        public boolean isBluetoothSupported;
        public boolean isBluetoothEnabled;
        public String message;

    }
    //=========================================== BTTester ===============================
    private static class BluetoothTester{

        private BluetoothDevice mConnectedBtDevice;
        private boolean isDiscoveryFinished = false;
        private BluetoothAdapter mBluetoothAdapter = null;
        private boolean isBTSupported = false;
        private boolean isBTEnabled = false;
        private BluetoothBroadcastReceiver mReceiver;
        private final Map<String, BluetoothDevice> mScannedList = new HashMap<>();
        private BluetoothGatt btGatt;
        private boolean bConnectResult = false;
        private boolean isBluetoothGattFuncFinished = false;

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        public void initial()
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(mBluetoothAdapter != null)
                isBTSupported = true;
            else
                return;

            isBTEnabled = mBluetoothAdapter.isEnabled();

            //discoveringDevices();
        }
        public void enableReceiver(){
            mReceiver = new BluetoothBroadcastReceiver();

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//            String startBTScanCmd = "start.my.bt.scan.task@";
//            filter.addAction(startBTScanCmd);
            mainService.registerReceiver(mReceiver, filter);
        }

        public void connectToBluetoothDevice(BluetoothDevice selectDevice){

            btGatt = selectDevice.connectGatt(mainService.getApplicationContext(), false, btGattCallback);

        }

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

        private BluetoothDevice getBtDeviceByMacAddress(String macAddress)
        {
            if (macAddress == null) {
                return null;
            }
            if(!isBTEnabled)
            {
                LOGGER.log(Level.INFO, "Bluetooth not enabled!");
                return null;
            }

            if(!BluetoothAdapter.checkBluetoothAddress(macAddress.toUpperCase()))
            {
                LOGGER.log(Level.INFO, "Invalid MAC address!");
                return null;
            }
            BluetoothDevice selectedDevice = mBluetoothAdapter.getRemoteDevice(macAddress.toUpperCase());
            if(null == selectedDevice)
            {
                LOGGER.log(Level.INFO, "Get remote bluetooth device fail!");
                return null;
            }
            return selectedDevice;
        }

        private boolean createBond(BluetoothDevice selectedDevice) {
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
            return result;
        }

        public void cancelBTDiscovery(){
            mBluetoothAdapter.cancelDiscovery();
            isDiscoveryFinished = false;
            mScannedList.clear();
        }

        public void onDestroy() {
            mainService.unregisterReceiver(mReceiver);
        }

        public boolean enableBluetooth(){
            if(!isBTSupported)
                return  false;

            if(isBTEnabled)
                return  true;

            try{
                mBluetoothAdapter.enable();

                Thread.sleep(3000);
                isBTEnabled = mBluetoothAdapter.isEnabled();

                if(isBTEnabled)
                    return  true;
                else
                    return  false;
            }
            catch (Exception e) {
                return false;
            }
        }

        //Querying paired devices
        public List<BluetoothDeviceInfo> queryingPairedDevices() {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            List<BluetoothDeviceInfo> mapPairedDevices =new ArrayList<>();

            if (!pairedDevices.isEmpty()) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo();
                    btDeviceInfo.deviceName =device.getName();
                    btDeviceInfo.address = device.getAddress();
                    mapPairedDevices.add(btDeviceInfo);
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

        public class BluetoothBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice btDevice;


                switch (Objects.requireNonNull(action)){
                    case BluetoothDevice.ACTION_FOUND:
                        LOGGER.log(Level.INFO, "BluetoothDevice.ACTION_FOUND");
                        btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if ((btDevice == null) || (btDevice.getName() == null)) {
                            return;
                        }
                        if (btDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                            if (!mScannedList.containsKey(btDevice.getAddress())) {
                                mScannedList.put(btDevice.getAddress(), btDevice);
                                LOGGER.log(Level.INFO, String.format("address:%s,btDevice:%s",btDevice.getAddress(), btDevice));
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
        }
    }
}
