/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package ro.polak.webserver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ro.polak.webserver.base.BaseMainActivity;
import ro.polak.webserver.base.BaseMainService;
import ro.polak.webserver.webserver.R;

/**
 * The main server Android activity.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201008
 */
public class MainActivity extends BaseMainActivity {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private TextView status;
    private TextView ipText;
    private TextView consoleText;
    private Button actionButton;
    private Button backgroundButton;
    private Button requestPermissionsButton;
    private Button quitButton;
    private ImageView imgView;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOnCreate() {

        setContentView(R.layout.activity_main);

        imgView = findViewById(R.id.ImageView01);
        status = findViewById(R.id.TextView01);
        ipText = findViewById(R.id.TextView02);
        consoleText = findViewById(R.id.textView1);
        actionButton = findViewById(R.id.Button01);
        actionButton.setOnClickListener(new ButtonListener(this));

        backgroundButton = findViewById(R.id.Button02);
        backgroundButton.setOnClickListener(new ButtonListener(this));

        quitButton = findViewById(R.id.Button03);
        quitButton.setOnClickListener(new ButtonListener(this));

        requestPermissionsButton = findViewById(R.id.Button04);
        requestPermissionsButton.setOnClickListener(new ButtonListener(this));

        status.setText("Initializing");

        //LOGGER.log(Level.INFO, "MainActivity.doOnCreate is running." + " thread ID = " + Thread.currentThread().getId());
        //Toast.makeText(getApplicationContext(), "Initializing doOnCreate", Toast.LENGTH_SHORT).show();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected Class<MainService> getServiceClass() {
        return MainService.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRequestPermissions() {
        status.setText("Requesting permissions");
        actionButton.setVisibility(View.GONE);
        backgroundButton.setVisibility(View.GONE);
        ipText.setVisibility(View.GONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doShowMustAcceptPermissions() {
        status.setText("Unable to initialize. Missing permissions.");
        requestPermissionsButton.setVisibility(View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doNotifyStateChangedToOffline() {
        imgView.setImageResource(R.drawable.offline);
        status.setText("Server offline");
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText("Start HTTPD");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doNotifyStateChangedToOnline(final BaseMainService.ServiceStateDTO serviceStateDTO) {
        ipText.setText(serviceStateDTO.getAccessUrl());

        imgView.setImageResource(R.drawable.online);
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText("Stop HTTPD");
        status.setText("Server online");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void println(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    consoleText.setText(text + "\n" + consoleText.getText());
                } catch (Exception e) {
                    Log.i("HTTP", text);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOnPermissionsAccepted() {
        backgroundButton.setVisibility(View.VISIBLE);
        ipText.setVisibility(View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    protected Set<String> getRequiredPermissions() {
        Set<String> permissions = super.getRequiredPermissions();
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        return permissions;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK )
        {
            // 创建退出对话框
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            // 设置对话框标题
            isExit.setTitle("系统提示");
            // 设置对话框消息
            isExit.setMessage("确定要退出吗");
            // 添加选择按钮并注册监听
            isExit.setButton("确定", listener);
            isExit.setButton2("取消", listener);
            // 显示对话框
            isExit.show();

        }

        return false;
    }

    /**监听对话框里面的button点击事件*/
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            switch (which)
            {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                    requestServiceStop();
                    //quitButton.callOnClick();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Button listener for the move to background and exit action.
     */
    private class ButtonListener implements View.OnClickListener {

        private BaseMainActivity activity;

        ButtonListener(final BaseMainActivity activity) {
            this.activity = activity;
        }

        public void onClick(final View v) {
            int id = v.getId();

            if (id == requestPermissionsButton.getId()) {
                requestPermissions();
                return;
            } else if (id == backgroundButton.getId()) {
                moveTaskToBack(true);
                //registerMyBroadcast(); //Derek 2024/08/26
                return;
            } else if (id == quitButton.getId()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage("Are you sure you want to exit?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                if (isMainServiceBound()) {
                                    requestServiceStop();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Background service not bound!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();

            } else if (id == actionButton.getId()) {
                if (isMainServiceBound()) {
                    if (getMainService().getServiceState().isWebServerStarted()) {
                        getMainService().getController().stop();
                    } else {
                        getMainService().getController().start();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Background service not bound!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
