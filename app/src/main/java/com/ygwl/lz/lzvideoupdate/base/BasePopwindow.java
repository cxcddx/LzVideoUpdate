package com.ygwl.lz.lzvideoupdate.base;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ygwl.lz.lzvideoupdate.R;


public class BasePopwindow extends PopupWindow {
	private ProgressDialog mProgressDialog;
	private Context context;
	private Toast mToast;

	public BasePopwindow(Context context) {
		super(context);
		this.context = context;
		mProgressDialog = new ProgressDialog(context, R.style.wait_dialog);
		mProgressDialog.setCanceledOnTouchOutside(false);
	}

	protected void showProgressDialog() {
		if(mProgressDialog != null && !mProgressDialog.isShowing()){
			mProgressDialog.show();
			mProgressDialog.setContentView(R.layout.common_request_layout);
		}
	}

	protected void dissmissProgressDialog() {
		if(mProgressDialog != null && mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}
	}

	protected void toast(int res) {
		if (context != null) {
			if (mToast == null){
				mToast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
			}else{
				mToast.setText(res);
			}
			mToast.show();
		}
	}

	protected void toast(String res) {
		if (context != null) {
			if (mToast == null){
				mToast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
			}else{
				mToast.setText(res);
			}
			mToast.show();
		}
	}

	@Override
	public void showAsDropDown(View view) {
		if (Build.VERSION.SDK_INT == 24){
			int[] location = new int[2];
			view.getLocationOnScreen(location);
			int x = location[0];
			int y = location[1];
			super.showAtLocation(view, Gravity.NO_GRAVITY, 0, y + getHeight());
		}else{
			super.showAsDropDown(view);
		}
	}
}
