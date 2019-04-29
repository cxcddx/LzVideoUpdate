package com.ygwl.lz.lzvideoupdate.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ygwl.lz.lzvideoupdate.R;


public class BaseFragment extends Fragment {
	private ProgressDialog mProgressDialog;
	private Toast mToast;
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(getClass().getSimpleName(), "onResume");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mProgressDialog = new ProgressDialog(getActivity(), R.style.wait_dialog);
		mProgressDialog.setCanceledOnTouchOutside(false);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);

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

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}
	    
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	protected void toast(int res) {
		if (getActivity() != null && !getActivity().isFinishing()) {
			if (mToast == null){
				mToast = Toast.makeText(getActivity(), res, Toast.LENGTH_SHORT);
			}else{
				mToast.setText(res);
			}
			mToast.show();
		}
	}

	protected void toast(String res) {
		if (getActivity() != null && !getActivity().isFinishing()) {
			if (mToast == null){
				mToast = Toast.makeText(getActivity(), res, Toast.LENGTH_SHORT);
			}else{
				mToast.setText(res);
			}
			mToast.show();
		}
	}
}
