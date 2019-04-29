package com.ygwl.lz.lzvideoupdate.permission.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.ygwl.lz.lzvideoupdate.permission.constant.PermissionConstant;

/**
 * 真正申请权限处理权限的Activity,透明的
 * Created by 27469 on 2018/4/26.
 */

public class PermissionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    // 权限申请
    private void handleIntent(Intent intent) {
        String[] permissions = intent.getStringArrayExtra(PermissionConstant.KEY_PERMISSIONS);
        int requestCode = intent.getIntExtra(PermissionConstant.KEY_REQUEST_CODE, 42);
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 返回结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionChecker.getInstance().onRequestPermissionResult(this, requestCode, permissions, grantResults);
        finish();
    }
}
