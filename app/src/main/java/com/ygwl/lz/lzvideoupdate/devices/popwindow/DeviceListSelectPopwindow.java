package com.ygwl.lz.lzvideoupdate.devices.popwindow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.android.business.adapter.DataAdapteeImpl;
import com.android.business.adapter.DataAdapterInterface;
import com.android.business.entity.GroupInfo;
import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.base.BasePopwindow;
import com.ygwl.lz.lzvideoupdate.devices.adapter.DeviceAreaAdapter;
import com.ygwl.lz.lzvideoupdate.devices.adapter.DeviceNodeAdapter;
import com.ygwl.lz.lzvideoupdate.group.GroupFactory;

import java.util.ArrayList;
import java.util.List;

public class DeviceListSelectPopwindow extends BasePopwindow implements OnClickListener {
	private View popwindowView;
	private RecyclerView rvDeviceNodeList;
	private ListView lDeviceAreaList;
	private LinearLayout llNullLayout;

	private Context context;
	private DeviceNodeAdapter mDeviceNodeAdapter;
	private DeviceAreaAdapter mDeviceAreaAdapter;

	private List<GroupInfo> groupInfos = new ArrayList<>();
	private List<GroupInfo> childInfos = new ArrayList<>();

	private int currentPosition = 0;

	private onDeviceListSelectPopupWindowListener deviceListSelectPopupWindowListener;
	private DataAdapterInterface dataAdapterInterface;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case 0:
					dissmissProgressDialog();
					List<GroupInfo> childInfos = (List<GroupInfo>) msg.obj;
					if(childInfos != null) {
						mDeviceAreaAdapter.setDataSet(childInfos);
						mDeviceAreaAdapter.notifyDataSetChanged();
					} else {
						toast(R.string.device_group_is_null);
					}
					break;
			}
		}
	};

	public static DeviceListSelectPopwindow newInstance(Context context, onDeviceListSelectPopupWindowListener listener)
	{
		DeviceListSelectPopwindow mStreamModeVerPopwindow = new DeviceListSelectPopwindow(context, listener);
		return mStreamModeVerPopwindow;
	}

	private DeviceListSelectPopwindow(Context context, onDeviceListSelectPopupWindowListener listener)
	{
		super(context);
		dataAdapterInterface = DataAdapteeImpl.getInstance();
		this.deviceListSelectPopupWindowListener = listener;
		this.context = context;
		initPopWindow();
		initPopWindowContent();
		initData();
	}

	private void initPopWindow() {
		popwindowView = LayoutInflater.from(context).inflate(R.layout.device_list_select_layout, null);
		setContentView(popwindowView);
		setHeight(LayoutParams.MATCH_PARENT);
		setWidth(LayoutParams.MATCH_PARENT);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		setFocusable(true);
		setOutsideTouchable(false);
		setTouchable(true);

		popwindowView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return false;
			}
		});
	}

	private void initPopWindowContent() {
		initDeviceSelectView();
	}

	private void initDeviceSelectView(){
		rvDeviceNodeList = (RecyclerView) popwindowView.findViewById(R.id.device_node_list);
		lDeviceAreaList = (ListView) popwindowView.findViewById(R.id.device_area_list);
		llNullLayout = (LinearLayout) popwindowView.findViewById(R.id.device_null_layout);
		initAreaHeaderView();
	}

	private void initAreaHeaderView(){
		LinearLayout header = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.device_area_list_header_item, null);
		TextView text = (TextView) header.findViewById(R.id.device_area_header_text);
		text.setText(R.string.device_all_device);

		lDeviceAreaList.addHeaderView(header);
	}

	private void initData(){
		initNodeData();
		initAreaData();
		initEvent();
	}

	private void initGroupData(){
//		GroupInfo groupInfo = null;
//		try {
//			groupInfo = dataAdapterInterface.queryGroupInfo(null);
//		} catch (BusinessException e) {
//			e.printStackTrace();
//		}
//
//		try {
//			List<GroupInfo> groupInfos = dataAdapterInterface.queryGroup(groupInfo == null ? null : groupInfo.getGroupId());
//			if(groupInfos != null && groupInfos.size() > 0){
//				GroupInfo firstInfo = groupInfos.get(0);
//				if(!TextUtils.equals(groupInfo.getGroupId(), firstInfo.getGroupId()) && TextUtils.isEmpty(firstInfo.getGroupParentId())){
//					groupInfo = firstInfo;
//				}
//			}
//		} catch (BusinessException e) {
//			e.printStackTrace();
//		}

		groupInfos.add(GroupFactory.getInstance().getRootGroupInfo());
		getChildInfo(0);
	}

	private void getChildInfo(final int position){
		showProgressDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<GroupInfo> childInfos = null;
				GroupInfo preGroupInfo = groupInfos.get(groupInfos.size() - 1);
				childInfos = GroupFactory.getInstance().getChildGroupInfos(preGroupInfo.getGroupId()); //GroupHelper.getInstance().getChildGroup(preGroupInfo.getGroupId(), true);
				currentPosition = position;
				Message msg = new Message();
				msg.what = 0;
				msg.obj = childInfos;
				handler.sendMessage(msg);
			}
		}).start();
	}

	private void initNodeData(){
		mDeviceNodeAdapter = new DeviceNodeAdapter(context);
		mDeviceNodeAdapter.setDataSet(groupInfos);
		rvDeviceNodeList.setAdapter(mDeviceNodeAdapter);

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		rvDeviceNodeList.setLayoutManager(linearLayoutManager);
	}

	private void initAreaData(){
		mDeviceAreaAdapter = new DeviceAreaAdapter(context);
		lDeviceAreaList.setAdapter(mDeviceAreaAdapter);
	}

	private void initEvent(){
		mDeviceNodeAdapter.setOnItemClickLinstener(new DeviceNodeAdapter.OnItemClickLinstener() {
			@Override
			public void onItemClick(int position) {
				if(position == currentPosition){
					return;
				}

				for(int i = currentPosition; i > position; i--){
					groupInfos.remove(i);
				}

				mDeviceNodeAdapter.notifyDataSetChanged();
				getChildInfo(position);
			}
		});

		lDeviceAreaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0){
					deviceListSelectPopupWindowListener.onGroupSelectedListener(groupInfos.size() == 0
							? null : groupInfos.get(groupInfos.size() - 1));
					dismissPopWindow();
					return;
				}
				GroupInfo groupInfo = mDeviceAreaAdapter.getItem(position - 1);
				if(groupInfo.isHasChild()){
					currentPosition++;
					groupInfos.add(groupInfo);
					mDeviceNodeAdapter.notifyDataSetChanged();
					rvDeviceNodeList.scrollToPosition(currentPosition);
					getChildInfo(currentPosition);
				}else{
					deviceListSelectPopupWindowListener.onGroupSelectedListener(groupInfo);
					dismissPopWindow();
				}
			}
		});
		llNullLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissPopWindow();
			}
		});
	}

	public void refreshData(){
		currentPosition = 0;
		groupInfos.clear();
		childInfos.clear();
		initGroupData();
	}

	private void dismissPopWindow(){
		this.dismiss();
	}

	@Override
	public void onClick(View view) {

	}
	
	public interface onDeviceListSelectPopupWindowListener {
		public void onGroupSelectedListener(GroupInfo groupInfo);
		public void onGroupFailedListener();
	}

}
