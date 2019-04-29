package com.ygwl.lz.lzvideoupdate.base;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.ygwl.lz.lzvideoupdate.R;

public class BaseActivity extends AppCompatActivity {

	protected Context mContext;
	private Toast mToast;

	private ProgressDialog mProgressDialog;
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		mContext = this;
		mProgressDialog = new ProgressDialog(this, R.style.wait_dialog);
		mProgressDialog.setCanceledOnTouchOutside(false);
	}

	private boolean isDestroyed = false;

	public boolean isActivityDestory() {
		return isDestroyed;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isDestroyed = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
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
		if (mToast == null){
			mToast = Toast.makeText(mContext, res, Toast.LENGTH_SHORT);
		}else{
			mToast.setText(res);
		}
		mToast.show();
	}

	protected void toast(String str) {
		if (mToast == null){
			mToast = Toast.makeText(mContext, str, Toast.LENGTH_SHORT);
		}else{
			mToast.setText(str);
		}
		mToast.show();
	}
}
