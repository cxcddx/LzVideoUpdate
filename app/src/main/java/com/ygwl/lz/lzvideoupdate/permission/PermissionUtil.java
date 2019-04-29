package com.ygwl.lz.lzvideoupdate.permission;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.permission.constant.PermissionConstant;
import com.ygwl.lz.lzvideoupdate.permission.core.PermissionChecker;
import com.ygwl.lz.lzvideoupdate.permission.core.PermissionResultCallBack;

import java.util.Arrays;
import java.util.List;

/**
 * 通用的请求权限流程
 * Created by 28973 on 2018/5/8.
 */

public class PermissionUtil {

    private OnPermissionRequestListener mListener;
    private Context mApplicationContext;

    public PermissionUtil(@NonNull OnPermissionRequestListener listener) {
        this.mListener = listener;
    }

    public void requestPermissions(final Context context, String[] permissions) {
        mApplicationContext = context.getApplicationContext();
        PermissionChecker.getInstance().request(context, permissions, PermissionConstant.PERMISSION_REQUEST_CODE, new PermissionResultCallBack() {
            @Override
            public void onPermissionGranted() {
                mListener.onPermissionGranted();
            }

            @Override
            public void onPermissionDenied(String[] denyPermissions, String[] neverAskDenyPermissions) {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(R.string.permission_title_setting)
                        .setMessage(String.format(context.getString(R.string.permission_request_due_to_reject_never),getDenyPermissionNames(neverAskDenyPermissions)))
                        .setPositiveButton(R.string.permission_apply_setting, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                boolean b = PermissionChecker.getInstance().startAppSettings(context);
                                if(!b){
                                    Toast.makeText(context,R.string.permission_failed_by_rom,Toast.LENGTH_LONG).show();
                                }
                                mListener.onPermissionSetting(b);
                            }
                        }).setNegativeButton(R.string.permission_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mListener.onPermissionDenied();
                            }
                        }).show();
            }

            @Override
            public void onRationalShowDenied(final String[] denyPermissions) {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(R.string.permission_title_description)
                        .setMessage(String.format(context.getString(R.string.permission_request_due_to_reject),getDenyPermissionNames(denyPermissions)))
                        .setPositiveButton(R.string.permission_apply_request, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestPermissions(context,denyPermissions);
                            }
                        }).setNegativeButton(R.string.permission_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mListener.onPermissionDenied();
                            }
                        }).show();
            }
        });
    }

    public interface OnPermissionRequestListener{
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionSetting(boolean isStartSetting);
    }

    private String getDenyPermissionNames(String[] denyPermissions){
        List<String> denyPermissionList = Arrays.asList(denyPermissions);
        StringBuilder permissionNames = new StringBuilder();
        if(denyPermissionList.contains(Manifest.permission.READ_CALENDAR)
                || denyPermissionList.contains(Manifest.permission.WRITE_CALENDAR)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_calendar));
        }
        if(denyPermissionList.contains(Manifest.permission.CAMERA)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_camera));
        }
        if(denyPermissionList.contains(Manifest.permission.READ_CONTACTS)
                || denyPermissionList.contains(Manifest.permission.WRITE_CONTACTS)
                || denyPermissionList.contains(Manifest.permission.GET_ACCOUNTS)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_contacts));
        }
        if(denyPermissionList.contains(Manifest.permission.ACCESS_FINE_LOCATION)
                || denyPermissionList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_location));
        }
        if(denyPermissionList.contains(Manifest.permission.RECORD_AUDIO)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_microphone));
        }
        if(denyPermissionList.contains(Manifest.permission.READ_PHONE_STATE)
                || denyPermissionList.contains(Manifest.permission.CALL_PHONE)
                || denyPermissionList.contains(Manifest.permission.READ_CALL_LOG)
                || denyPermissionList.contains(Manifest.permission.WRITE_CALL_LOG)
                || denyPermissionList.contains(Manifest.permission.USE_SIP)
                || denyPermissionList.contains(Manifest.permission.PROCESS_OUTGOING_CALLS)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_phone));
        }
        if(denyPermissionList.contains(Manifest.permission.BODY_SENSORS)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_sensors));
        }
        if(denyPermissionList.contains(Manifest.permission.SEND_SMS)
                || denyPermissionList.contains(Manifest.permission.RECEIVE_SMS)
                || denyPermissionList.contains(Manifest.permission.READ_SMS)
                || denyPermissionList.contains(Manifest.permission.RECEIVE_WAP_PUSH)
                || denyPermissionList.contains(Manifest.permission.RECEIVE_MMS)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_sms));
        }
        if(denyPermissionList.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                || denyPermissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            permissionNames.append(mApplicationContext.getString(R.string.permission_storage));
        }
        permissionNames.deleteCharAt(permissionNames.lastIndexOf("、"));
        return permissionNames.toString();
    }

}
