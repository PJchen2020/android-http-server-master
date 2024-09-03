package admin.TestModule;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class NetworkPingViewModel extends AndroidViewModel {
    private final String TAG = "NetworkPingViewModel";

    private final int MESSAGE_RESULT = 1;
    private final int MESSAGE_CONTENT = 2;

    private final Handler mUiHandler;

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    protected MutableLiveData<Pair<Boolean, String>> mPingResultResponse;
    protected MutableLiveData<String> mPingContentResponse;

    public NetworkPingViewModel(@NonNull Application application) {
        super(application);
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MESSAGE_RESULT:
                        mPingResultResponse.setValue((Pair<Boolean, String>) msg.obj);
                        break;

                    case MESSAGE_CONTENT:
                        mPingContentResponse.setValue(String.valueOf(msg.obj));
                        break;
                }
            }
        };
        mHandlerThread = new HandlerThread("PingThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public MutableLiveData<Pair<Boolean, String>> getPingResultResponse() {
        if (mPingResultResponse == null) {
            mPingResultResponse = new MutableLiveData<>();
        }
        return mPingResultResponse;
    }

    public MutableLiveData<String> getPingContentResponse() {
        if (mPingContentResponse == null) {
            mPingContentResponse = new MutableLiveData<>();
        }
        return mPingContentResponse;
    }

    public void pingIp(String ipAddress, int packetAmount) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Pair<Boolean, String> resultPair = null;
                try {
                    Log.d(TAG, "ping ip: " + ipAddress);
                    Process process = new ProcessBuilder().command("ping", "-w" + packetAmount, ipAddress)
                            .redirectErrorStream(true)
                            .start();
                    InputStream in = process.getInputStream();
                    InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    BufferedReader buf = new BufferedReader(reader);
                    String line;
                    while ((line = buf.readLine()) != null) {
                        Log.d(TAG, "ping content: " + line);
                        if (line.toLowerCase().startsWith("ping")) {
                            Message msg = new Message();
                            msg.what = MESSAGE_CONTENT;
                            msg.obj = line;
                            mUiHandler.sendMessage(msg);
                        } else if (line.toLowerCase().contains("bytes from")) {
                            Message msg = new Message();
                            msg.what = MESSAGE_CONTENT;
                            msg.obj = line;
                            mUiHandler.sendMessage(msg);
                        } else if (line.toLowerCase().contains("packets transmitted")) {
                            for (String str : line.split(",")) {
                                if (str.toLowerCase().contains("received")) {
                                    String receivedCount = str.toLowerCase()
                                            .replace("received", "").trim();
                                    if (Integer.parseInt(receivedCount) > 0) {
                                        resultPair = new Pair<>(true, line);
                                    } else {
                                        resultPair = new Pair<>(false, line);
                                    }
                                }
                            }
                        } else if (line.toLowerCase().contains("unreachable")) {
                            resultPair = new Pair<>(false, line);
                            break;
                        }
                    }
                    buf.close();
                    reader.close();
                    in.close();
                    process.destroy();
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    resultPair = new Pair<>(false, e.toString());
                }
                if (resultPair != null) {
                    Message msg = new Message();
                    msg.what = MESSAGE_RESULT;
                    msg.obj = resultPair;
                    mUiHandler.sendMessage(msg);
                }
            }
        });
    }
}
