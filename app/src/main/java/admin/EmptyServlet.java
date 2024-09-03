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
public class EmptyServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(EmptyServlet.class.getName());
    private static MainService mainService;
    private final BluetoothTester bluetoothTester = new BluetoothTester();
    final int ScanTimeout = 60;
    private static final Map<String, BluetoothDevice> mScannedList = new HashMap<>();

    @Override
    public void destroy() {
        LOGGER.log(Level.INFO, "Entry MyTestServlet destroy process");
        super.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, InterruptedException {
        //response.getWriter().print("Web thread ID = " + Thread.currentThread().getId());

        LOGGER.log(Level.INFO, "Entry MyTestServlet service process");

        ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());
        mainService = (MainService)serverConfig.getServiceContext(); //getServiceContext --> ro.polak.webserver.MainService@6103e86
        if(null == mainService){
            response.getWriter().print("MainService object is null!!");

            return;
        }
        bluetoothTester.initial();
        if(request.getParameter("EnableBluetooth") !=null && request.getParameter("EnableBluetooth").equals("true"))
        {
            response.getWriter().print(bluetoothTester.enableBluetooth());
        }
        if(request.getQueryString() == null|| request.getQueryString().isEmpty())
        {
            response.getWriter().print(outputBTInfo(mainService));
        }
        if(request.getParameter("Address") !=null )
        {
            String address = request.getParameter("Address");
            BluetoothDevice needConnectDevice =  mScannedList.get(address);
            response.getWriter().print(bluetoothTester.connectToBluetoothDevice(needConnectDevice));
        }

        bluetoothTester.onDestroy();

        response.getWriter().print(RenderHtmlFile());
    }

//    private void startBTTest(MainService ms){
//        Intent intent = new Intent();
//        intent.setClass(ms, BluetoothTester.class);
//        ms.startService(intent);
//    }

    private String outputBTInfo(MainService ms) throws InterruptedException {
        String errMsg = "No any error occur";
        JSONObject btObject = new JSONObject();
        boolean bBTEnable = false;
        String strEnableStatus = "Enable BT fail";
        Map<String, String> queryingPairedDevices = new HashMap<>();

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
                    errMsg = "Scan finished in " + i + " seconds";
                    break;
                }

                Thread.sleep(1000);

                if (i + 1 >= ScanTimeout) {
                    errMsg = "Scan process time out!";
                }
            }//end of for
        }//end of if
        else
            errMsg = "BT not support!";

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

                if (!mScannedList.isEmpty()) {
                    JSONObject dDevices = new JSONObject();
//                    List<BluetoothDeviceInfo> bluetoothDevicesInfo =new ArrayList<>();
                    for (String key : mScannedList.keySet()) {
//                        BluetoothDeviceInfo bluetoothDeviceInfo = new BluetoothDeviceInfo();
//                        bluetoothDeviceInfo.address = key;
//                        bluetoothDeviceInfo.deviceName = Objects.requireNonNull(bluetoothTester.mScannedList.get(key)).getName();
//                        bluetoothDevicesInfo.add(bluetoothDeviceInfo);
//                        dDevices.put(key,Objects.requireNonNull(bluetoothTester.mScannedList.get(key)).getName());
                        btObject.put(Objects.requireNonNull(mScannedList.get(key)).getName(),key);
                    }

                    btObject.put("Scanned BT device count", mScannedList.size());
                    btObject.put("mScannedList", dDevices);
                }
            }//end of if

            btObject.put("errMsg", errMsg);
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

    public static class BluetoothDeviceInfo{
        public String deviceName;
        public String address;
    }
    //=========================================== BTTester ===============================
    private static class BluetoothTester{

        private BluetoothDevice mConnectedBtDevice;
        private boolean isDiscoveryFinished = false;
        private BluetoothAdapter mBluetoothAdapter = null;
        private boolean isBTSupported = false;
        private boolean isBTEnabled = false;
        private BluetoothBroadcastReceiver mReceiver;

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        public void initial()
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

        public boolean connectToBluetoothDevice(BluetoothDevice selectedDevice){
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
                    LOGGER.log(Level.INFO, "Can't pair with the device");
                }
            }
            if (result) {
                mConnectedBtDevice = selectedDevice;
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
