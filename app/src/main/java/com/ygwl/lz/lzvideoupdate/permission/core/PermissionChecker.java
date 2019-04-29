package com.ygwl.lz.lzvideoupdate.permission.core;


import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.ygwl.lz.lzvideoupdate.permission.constant.PermissionConstant;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限校验申请类
 * Created by 27469 on 2018/4/26.
 */

public class PermissionChecker {

    /**
     * 请求码
     */
    private int mRequestCode;

    /**
     * 请求权限回调接口
     */
    private PermissionResultCallBack mPermissionResultCallBack;

    private static final String PACKAGE_URL_SCHEME = "package:";

    private PermissionChecker() {

    }

    private static class PermissionHolder {
        private static final PermissionChecker INSTANCE = new PermissionChecker();
    }

    public static PermissionChecker getInstance() {
        return PermissionHolder.INSTANCE;
    }

    /**
     * @param fragment                 申请权限所在的Fragment实例
     * @param permissions              申请的权限列表
     * @param requestCode              申请码
     * @param permissionResultCallBack 申请结果回调
     */
    public void request(Fragment fragment, String[] permissions, int requestCode, PermissionResultCallBack permissionResultCallBack) {
        if (fragment == null) {
            throw new RuntimeException("the fragment that request permissions can't be null");
        }
        if (permissions == null || permissions.length == 0) {
            throw new RuntimeException("permissions that requested can't be null or empty");
        }

        this.request(fragment.getActivity(), permissions, requestCode, permissionResultCallBack);

    }

    /**
     * @param context                  上下文，必须是Activity实例
     * @param permissions              申请的权限
     * @param requestCode              申请码
     * @param permissionResultCallBack 申请结果回调
     */

    public void request(Context context, String[] permissions, int requestCode, PermissionResultCallBack permissionResultCallBack) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("you must invoke this method on main thread");
        }
        if (context != null && context instanceof Activity) {
            this.mRequestCode = requestCode;
            this.mPermissionResultCallBack = permissionResultCallBack;
            List<String> permissionList = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission);
                }
            }
            if (permissionList.isEmpty()) {
                if (permissionResultCallBack != null) {
                    permissionResultCallBack.onPermissionGranted();
                }
                return;
            }

            String[] denyPermissions = new String[permissionList.size()];
            permissionList.toArray(denyPermissions);
            requestPermission(context, denyPermissions, requestCode);

        } else {
            throw new RuntimeException("Context must be an Activity");
        }
    }


    private void requestPermission(Context context, String[] permissions, int requestCode) {
        Intent intent = new Intent(context, PermissionActivity.class);
        intent.putExtra(PermissionConstant.KEY_PERMISSIONS, permissions);
        intent.putExtra(PermissionConstant.KEY_REQUEST_CODE, requestCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    protected void onRequestPermissionResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        if (mRequestCode == requestCode) {
            boolean isAllGranted = true;
            List<String> deniedPermissionList = new ArrayList<>();
            List<String> neverAskPermissionList = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) { //用于判断该权限是否勾选"不再询问",true代表该权限没有勾选，false代表勾选。
                        deniedPermissionList.add(permissions[i]);
                    } else {
                        neverAskPermissionList.add(permissions[i]);
                    }
                    isAllGranted = false;
                }
            }
            if (mPermissionResultCallBack != null) {
                if (isAllGranted) {
                    mPermissionResultCallBack.onPermissionGranted();
                } else {
                    String[] deniedPermissions = new String[deniedPermissionList.size()];
                    deniedPermissionList.toArray(deniedPermissions);

                    String[] neverAskPermissions = new String[neverAskPermissionList.size()];
                    neverAskPermissionList.toArray(neverAskPermissions);

                    if (neverAskPermissions.length > 0) { //至少有一个权限选择了"不再询问",这种情况下必须提示用户相关权限的必要性,并且跳到对应权限设置界面，或者提示用户跳到权限设置界面开启权限，
                        // 因为对于"不再询问"的权限,发起权限申请不会弹系统的权限申请弹窗。
                        mPermissionResultCallBack.onPermissionDenied(deniedPermissions, neverAskPermissions);
                    } else { //所有被拒绝的权限都没有选择"不再询问"，这种情况下建议提示用户相关权限的必要性,一般可以弹窗说明，点击确认后重新发起权限申请，按照当前流程设计会依然弹出系统的权限申请弹窗，
                        // 当然回调后什么也不做也问题不大，因为这种情况下，重新申请权限时依然弹出系统的权限申请弹窗。
                        mPermissionResultCallBack.onRationalShowDenied(deniedPermissions);
                    }

                }
            }
        }

    }

    /**
     * 跳转应用详情界面
     *
     * @param context 上下文
     * @return true 跳转成功，false 跳转失败
     */
    public boolean startAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
    /********************************上述主流权限校验对于API 23（6.0）以下设备永远返回权限已授权，但是用户在6.0以下部分rom下可以去设置或者第三方管理工具里关闭权限，故不能作为判断依据，
     * 或者对于部分非android6.0动态权限体系内的权限（比如悬浮窗权限）可以采用以下两个方法。
     * **************************************/


    /**
     * 检查某个权限对应op是否拥有权限（API 19及以上版本可用，内部采用反射）,针对部分国产rom机器6.0以下的情况，（例如360、酷派管家等等）,
     * 但是具体的OP需要对应你APP支持的MinSdkVersion,去查对应的每一个源码库查找是否含有该OP，否则也无效的。
     *
     * @param context 上下文
     * @param op      权限对应操作op,目前都hide了，详见{@Link AppOpsManager}
     * @return true代表对应权限已授权，false代表未授权
     */
    public boolean checkOp(@NonNull Context context, int op) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager != null) {
                try {
                    Method checkOpMethod = AppOpsManager.class.getDeclaredMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                    int checkOp = (Integer) checkOpMethod.invoke(appOpsManager, op, Binder.getCallingUid(), context.getPackageName());
                    return (checkOp == AppOpsManager.MODE_ALLOWED);
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 检查某个权限对应op是否拥有权限（API 23及以上版本可用）,针对部分国产rom机器6.0以下的情况，（例如360、酷派管家等等）,
     * 但是具体的OP需要对应你APP支持的MinSdkVersion,去查对应的每一个源码库查找是否含有该OP，否则也无效的。
     *
     * @param context 上下文
     * @param op      The Operation to check.One of the OPSTR * constants,详见{@Link AppOpsManager}
     * @return true代表对应权限已授权，false代表未授权
     */
    public boolean checkOp(@NonNull Context context, String op) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager != null) {
                try {
                    int checkOp = appOpsManager.checkOp(op, Binder.getCallingUid(), context.getPackageName());
                    return (checkOp == AppOpsManager.MODE_ALLOWED);
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }
}
