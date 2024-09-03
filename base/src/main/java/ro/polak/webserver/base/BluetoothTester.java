package ro.polak.webserver.base;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//https://www.android-doc.com/guide/topics/connectivity/bluetooth.html
public class BluetoothTester extends Service {
    private final String TAG = "BluetoothTester";

    static private BluetoothAdapter mBluetoothAdapter = null;
    private boolean bBTSupported = false;
    private boolean bBTEnabled = false;

    private BluetoothBroadcastReceiver mReceiver;
    Map<String, String> mBTdevices = new HashMap<>();
    private Map<String, BluetoothDevice> mScannedList = new HashMap<>();
    private BluetoothDevice mConnectedBtDevice;
    private boolean bIsDiscoveryFinished = false;
    private final String startBTScanCmd = "start.my.bt.scan.task@";
    private final IBinder binder = new LocalBinder();

    public void initial()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter != null)
            bBTSupported = true;
        else
            return;

        bBTEnabled = mBluetoothAdapter.isEnabled();
        mReceiver = new BluetoothBroadcastReceiver();

        //Toast.makeText(getApplicationContext(),"BluetoothTester::initial", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        //Toast.makeText(getApplicationContext(),"BluetoothTester::onCreate", Toast.LENGTH_SHORT).show();

        initial();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //filter.addAction(startBTScanCmd);
        registerReceiver(mReceiver, filter);

        //discoveringDevices();

        super.onCreate();
    }

    public void cancelBTDiscovery(){
        mBluetoothAdapter.cancelDiscovery();
        bIsDiscoveryFinished = false;
        mScannedList.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(),"BluetoothTester::onStartCommand", Toast.LENGTH_SHORT).show();

        //discoveringDevices();

        //Intent intentStartBTScan = new Intent(startBTScanCmd);
        //sendBroadcast(intentStartBTScan);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        Toast.makeText(getApplicationContext(),"BluetoothTester::onDestroy", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    public boolean isBluetoothSupported(){
        return bBTSupported;
    }

    public boolean isBluetoothEnabled(){
        return  bBTEnabled;
    }

    public boolean isDiscoveryFinished(){return bIsDiscoveryFinished;}

    public void setDiscoveryFinished(boolean finished){
        bIsDiscoveryFinished = finished;
    }

    public String enableBluetooth(){
        if(!bBTSupported)
            return  "BT not supported!";

        if(bBTEnabled)
            return  "BT has enabled";

        try{
            //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivity(intent);
            mBluetoothAdapter.enable();

            Thread.sleep(3000);
            bBTEnabled = mBluetoothAdapter.isEnabled();

            if(bBTEnabled)
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
        Map<String, String> mPairedDevices = new HashMap<>();

        if (!pairedDevices.isEmpty()) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mPairedDevices.put(device.getName(), device.getAddress());
            }
        }

        return mPairedDevices;
    }

    //Discovering devices
    public void discoveringDevices() {
        if(!bBTSupported || !bBTEnabled)
            return;

        if(mBluetoothAdapter.isDiscovering()) {
            //Toast.makeText(getApplicationContext(),"BluetoothBroadcastReceiver::isDiscovering --> cancel it", Toast.LENGTH_SHORT).show();
            mBluetoothAdapter.cancelDiscovery();
            //return;
        }

        mBluetoothAdapter.startDiscovery();
        //Toast.makeText(getApplicationContext(),"BluetoothBroadcastReceiver::startDiscovery", Toast.LENGTH_SHORT).show();
        //Register the BroadcastReceiver
        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        //bRegisterReceiver = true;
    }

    public class BluetoothBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice btDevice;

            //Toast.makeText(getApplicationContext(),"BluetoothTester::BluetoothBroadcastReceiver::onReceive", Toast.LENGTH_SHORT).show();
            //Log.d(TAG, "onReceive(), action = " + action);
            switch (Objects.requireNonNull(action)){
                case BluetoothDevice.ACTION_FOUND:
                    //Toast.makeText(getApplicationContext(),"ACTION_FOUND", Toast.LENGTH_SHORT).show();
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
                    bIsDiscoveryFinished = true;
                    if (mConnectedBtDevice != null) {
                        return;
                    }
                    break;

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    //Toast.makeText(getApplicationContext(),"ACTION_STATE_CHANGED", Toast.LENGTH_SHORT).show();
                    int btAdapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (btAdapterState == BluetoothAdapter.STATE_ON) {
                        //removeAllBonds();
                    }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    //bIsDiscoveryFinished = false;
                    //mScannedList.clear();
                    //mBluetoothAdapter.startDiscovery();
                    //Toast.makeText(getApplicationContext(),"ACTION_DISCOVERY_STARTED", Toast.LENGTH_SHORT).show();
                    break;

                case "start.my.bt.scan.task@":
                    Toast.makeText(getApplicationContext(),startBTScanCmd, Toast.LENGTH_SHORT).show();
                    bIsDiscoveryFinished = false;
                    mScannedList.clear();
                    //mBluetoothAdapter.startDiscovery();
                    discoveringDevices();
                    break;
            } //end of switch
        }
    }

    public Map<String, String> getDiscoveringDevices(){
        return mBTdevices;
    }

    public Map<String, BluetoothDevice> getScannedList(){
        return mScannedList;
    }

    public boolean IsDiscoveryFinished(){
        return bIsDiscoveryFinished;
    }

    public class LocalBinder extends Binder {
        public BluetoothTester getService() {
            return BluetoothTester.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
