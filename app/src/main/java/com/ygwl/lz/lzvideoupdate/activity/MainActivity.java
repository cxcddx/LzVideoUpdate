package com.ygwl.lz.lzvideoupdate.activity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.android.business.adapter.DataAdapteeImpl;
import com.android.business.adapter.DataAdapterInterface;
import com.android.business.exception.BusinessException;
import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.base.BaseActivity;
import com.ygwl.lz.lzvideoupdate.devices.fragments.DeviceFragment;

public class MainActivity extends BaseActivity {
    DeviceFragment mFragment;
    private DataAdapterInterface dataAdapterInterface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataAdapterInterface = DataAdapteeImpl.getInstance();
        mFragment = DeviceFragment.newInstance("", "");

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        ft.replace(R.id.content, mFragment);
        ft.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    dataAdapterInterface.logout();
                } catch (BusinessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    long lastBack = 0;
    @Override
    public void onBackPressed() {
        long currentBack = System.currentTimeMillis();
        if (currentBack - lastBack > 2000) {
            lastBack = currentBack;
            toast(R.string.main_exit_tips);
        } else {
            exitApp();
        }
    }

    private void exitApp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        System.exit(0);
        Process.killProcess(Process.myPid());
    }
}
