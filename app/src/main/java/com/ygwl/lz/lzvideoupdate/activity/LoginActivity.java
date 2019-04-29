package com.ygwl.lz.lzvideoupdate.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.business.adapter.DataAdapteeImpl;
import com.android.business.adapter.DataAdapterInterface;
import com.android.business.entity.ChannelInfo;
import com.android.business.entity.UserInfo;
import com.android.business.exception.BusinessException;
import com.example.dhcommonlib.util.PreferencesHelper;
import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.base.BaseActivity;
import com.ygwl.lz.lzvideoupdate.permission.PermissionUtil;
import com.ygwl.lz.lzvideoupdate.permission.constant.PermissionConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by 26499 on 2017/11/14.
 */

public class LoginActivity extends BaseActivity implements View.OnClickListener{
    private EditText etIP;
    private EditText etPort;
    private EditText etUsername;
    private EditText etPassword;
    private Button bLogin;
    private DataAdapterInterface dataAdapterInterface;
    private Context mContext;
    private List<ChannelInfo> channelInfos = new ArrayList<>();
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    dissmissProgressDialog();
                    boolean ret = (Boolean) msg.obj;
                    if(ret) {
                        Intent intent = new Intent(mContext, MainActivity.class);
                        mContext.startActivity(intent);
                        finish();
                    } else {
                        toast(R.string.login_failed);
                    }
                    break;
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        initData();
    }

    private void initView() {
        etIP = (EditText) findViewById(R.id.ip);
        etPort = (EditText) findViewById(R.id.port);
        etUsername = (EditText) findViewById(R.id.username);
        etPassword = (EditText) findViewById(R.id.password);
        bLogin = (Button) findViewById(R.id.login);
        bLogin.setOnClickListener(this);
        etIP.setText("172.20.90.48");
        etPort.setText("9000");
        etUsername.setText("system");
        etPassword.setText("admin123");
    }

    private void initData() {
        dataAdapterInterface = DataAdapteeImpl.getInstance();
        mContext = this;
    }
    private boolean isPermissionsRequest = false;
    @Override
    protected void onResume() {
        super.onResume();
        if(!isPermissionsRequest) {
            requestAllPermissions();
            isPermissionsRequest = true;
        }
    }

    private void requestAllPermissions(){
        PermissionUtil permissionUtil = new PermissionUtil(new PermissionUtil.OnPermissionRequestListener() {
            @Override
            public void onPermissionGranted() {

            }

            @Override
            public void onPermissionDenied() {
                isPermissionsRequest = false;
            }

            @Override
            public void onPermissionSetting(boolean b) {
                isPermissionsRequest = false;
            }
        });
        List<String> permissionList = new ArrayList<>();
        permissionList.addAll(Arrays.asList(PermissionConstant.MICROPHONE));
        permissionList.addAll(Arrays.asList(PermissionConstant.STORAGE));
        permissionList.addAll(Arrays.asList(Manifest.permission.READ_PHONE_STATE));
        permissionUtil.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]));
    }

    //进行登录操作
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                doLogin();
                break;
        }
    }

    private void doLogin() {
        String ip = etIP.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if(TextUtils.isEmpty(ip) || TextUtils.isEmpty(port) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            toast(R.string.login_param_null);
            return;
        }
        setIPPort(ip, port);
        login(username, password);
    }
    private void setIPPort(String ip, String port) {
        try {
            dataAdapterInterface.createDataAdapter("com.android.business.adapter.DataAdapteeImpl");
            dataAdapterInterface.initServer(ip, Integer.parseInt(port));
        } catch (BusinessException e) {
            e.printStackTrace();
        }
    }

    private void login(final String username, final String password) {
        showProgressDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                String clientMac = tm.getDeviceId();
                UserInfo info = null;
                try {
                    info = dataAdapterInterface.login(username, password, "1", clientMac, 2);
                } catch (BusinessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(info != null) {
                    Object groupid=  info.getExtandAttributeValue("groupId");
                    String strGroupID = "";
                    if (groupid instanceof String) {
                        strGroupID = (String) groupid;
                    } else if(groupid instanceof Long) {
                        strGroupID = ((Long)groupid).toString();
                    }
                    PreferencesHelper.getInstance(getApplicationContext()).set("USER_GROUPID", strGroupID);
                }
                Message msg = new Message();
                msg.what = 0;
                msg.obj = (info != null);
                handler.sendMessage(msg);
            }
        }).start();
    }
}
