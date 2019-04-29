package com.ygwl.lz.lzvideoupdate.permission.core;

/**
 * 权限处理结果回调接口
 * Created by 27469 on 2018/4/26.
 */

public interface PermissionResultCallBack {

    /**
     * 所有申请权限都通过
     */
    void onPermissionGranted();

    /**
     * 被拒绝的权限回调列表,至少有一个权限选择了"不再询问",这种情况下必须提示用户相关权限的必要性,并且跳到对应权限设置界面，或者提示用户跳到权限设置界面开启权限，
     * 因为对于"不再询问"的权限,发起权限申请不会弹系统的权限申请弹窗。
     *
     * @param denyPermissions         被拒绝的权限，但是没有勾选"不再询问"
     * @param neverAskDenyPermissions 被拒绝的权限，勾选了"不再询问"
     */
    void onPermissionDenied(String[] denyPermissions, String[] neverAskDenyPermissions);

    /**
     * 被拒绝的权限回调列表,所有被拒绝的权限都没有选择"不再询问"，这种情况下建议提示用户相关权限的必要性,一般可以弹窗说明，点击确认后重新发起权限申请，按照当前流程设计会依然弹出系统的权限申请弹窗，
     * 当然回调后什么也不做也问题不大，因为这种情况下，重新申请权限时依然弹出系统的权限申请弹窗。
     *
     * @param denyPermissions 被拒绝的权限，没有勾选"不再询问"
     */
    void onRationalShowDenied(String[] denyPermissions);
}
