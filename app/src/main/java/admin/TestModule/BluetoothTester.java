package admin.TestModule;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ro.polak.webserver.base.BaseMainService;

//https://www.android-doc.com/guide/topics/connectivity/bluetooth.html
public class BluetoothTester extends Service {
    private final String TAG = "BluetoothTester";

    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean bBTSupported = false;
    private boolean bBTEnabled = false;

    private boolean bRegisterReceiver = false;
    private final BluetoothBroadcastReceiver mReceiver = new BluetoothBroadcastReceiver();
    Map<String, String> mBTdevices = new HashMap<>();
    private boolean bIsDiscoveryFinished = false;
    private BluetoothDevice mConnectedBtDevice;
    private Map<String, BluetoothDevice> mScannedList = new HashMap<>();
    private final IBinder binder = new BluetoothTester.LocalBinder();

    public class LocalBinder extends Binder {

        /**
         * Returns bounded service instance.
         *
         * @return
         */
        public BluetoothTester getService() {
            return BluetoothTester.this;
        }
    }

    public BluetoothTester() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null)
            bBTSupported = true;
        else
            return;

        bBTEnabled = mBluetoothAdapter.isEnabled();
    }

    public boolean isBluetoothSupported() {
        return bBTSupported;
    }

    public boolean isBluetoothEnabled() {
        return bBTEnabled;
    }

    public String EnableBluetooth() {
        if (!bBTSupported)
            return "BT not supported!";

        if (bBTEnabled)
            return "BT has enabled";

        try {
            mBluetoothAdapter.enable();

            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bBTEnabled = mBluetoothAdapter.isEnabled();

            if (bBTEnabled)
                return "BT enabled successfully";
            else
                return "BT enabled failed!";
        } catch (Exception e) {
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

    private class BluetoothBroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //BluetoothDevice btDevice;
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(getApplicationContext(), "ACTION_FOUND", Toast.LENGTH_SHORT).show();
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (null != device)
                    mBTdevices.put(device.getName(), device.getAddress());

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (!mScannedList.containsKey(device.getAddress())) {
                        mScannedList.put(device.getAddress(), device);
                    }
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equalsIgnoreCase(action)) {
                Toast.makeText(getApplicationContext(), "ACTION_DISCOVERY_FINISHED", Toast.LENGTH_SHORT).show();
                bIsDiscoveryFinished = true;
                //if (mConnectedBtDevice != null) {
                //    return;
                //}
            }else if (BluetoothAdapter.ACTION_STATE_CHANGED.equalsIgnoreCase(action)) {
                Toast.makeText(getApplicationContext(), "ACTION_STATE_CHANGED", Toast.LENGTH_SHORT).show();
                int btAdapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (btAdapterState == BluetoothAdapter.STATE_ON) {
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equalsIgnoreCase(action)) {
                Toast.makeText(getApplicationContext(), "ACTION_DISCOVERY_STARTED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Discovering devices
    public void discoveringDevices() {
        if (!bBTSupported || !bBTEnabled || bRegisterReceiver)
            return;

        mBluetoothAdapter.startDiscovery();

        //Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        bRegisterReceiver = true;
    }

    public Map<String, String> getDiscoveringDevices() {
        return mBTdevices;
    }

    public boolean IsDiscoveryFinished() {
        return bIsDiscoveryFinished;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private boolean connectToBluetoothDevice(BluetoothDevice selectedDevice) {
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
                Log.d(TAG, "Can't pair with the device");
            }
        }
        if (result) {
            mConnectedBtDevice = selectedDevice;
        }

        return result;
    }
}
