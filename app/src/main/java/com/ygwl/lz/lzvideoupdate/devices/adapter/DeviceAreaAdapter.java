package com.ygwl.lz.lzvideoupdate.devices.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.business.entity.GroupInfo;
import com.ygwl.lz.lzvideoupdate.R;

import java.util.List;

/**
 * Copyright © 2017 Dahua Technology. All rights reserved.
 *
 * @Prject: Trunk_workspace
 * @Package: com.mm.dss.device
 * @Description: ${TODO}
 * @Author 26499
 * @DateTime 2017/2/20 19:06
 */

public class DeviceAreaAdapter extends BaseAdapter {
    private Context context;
    private List<GroupInfo> mDataSet;

    public DeviceAreaAdapter(Context context){
        this.context = context;
    }

    public void setDataSet(List<GroupInfo> mDataSet){
        this.mDataSet = mDataSet;
    }

    @Override
    public int getCount() {
        return mDataSet == null ? 0 : mDataSet.size();
    }

    @Override
    public GroupInfo getItem(int position) {
        return mDataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.device_area_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        holder.area.setText(getItem(position).getGroupName());
        return convertView;
    }

    class ViewHolder{
        private TextView area;

        public ViewHolder(View itemView) {
            area = (TextView) itemView.findViewById(R.id.device_area_text);
        }
    }
}
