package com.ygwl.lz.lzvideoupdate.devices.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.business.entity.ChannelInfo;
import com.android.business.entity.DataInfo;
import com.android.business.entity.LogicalInfo;
import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.activity.PlayBackActivity;

import java.io.Serializable;
import java.util.ArrayList;
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

public class DeviceChannelAdapter extends BaseAdapter {
    private Context context;
    private List<ChannelInfo> mDataSet;

    public DeviceChannelAdapter(Context context){
        this.context = context;
    }

    public void setDataSet(List<ChannelInfo> mDataSet){
        this.mDataSet = mDataSet;
    }

    @Override
    public int getCount() {
        return mDataSet == null ? 0 : mDataSet.size();
    }

    @Override
    public DataInfo getItem(int position) {
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
            convertView = LayoutInflater.from(context).inflate(R.layout.fragment_channel_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        final DataInfo dataInfo = getItem(position);
        ChannelInfo info = null;
        if(dataInfo != null) {
            if(dataInfo instanceof ChannelInfo) {
                info = (ChannelInfo) dataInfo;
            } else if(dataInfo instanceof LogicalInfo){
                info = (ChannelInfo) ((LogicalInfo) dataInfo).getDataInfo();
            }
        }
        final ChannelInfo channelInfo = info;
        if(channelInfo != null) {
            if (channelInfo.getVideoInputInfo() != null && channelInfo.getVideoInputInfo().getCameraType() == ChannelInfo.CameraType.CameraPtz) {
                holder.channelThumbnail.setImageResource(R.drawable.common_channel_icon_ptz);
            } else {
                holder.channelThumbnail.setImageResource(R.drawable.common_channel_icon_normal);
            }
            holder.channelThumbnail.setSelected(channelInfo.getState() == ChannelInfo.ChannelState.Online);
            holder.channelName.setText(channelInfo.getName());

            holder.channelPlayback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<ChannelInfo> channelInfos = new ArrayList<>();
                    channelInfos.add(channelInfo);
                    Intent intent = new Intent(context, PlayBackActivity.class);
                    intent.putExtra("channel_info_list", (Serializable) channelInfos);
                    context.startActivity(intent);
                }
            });
            holder.channelPlayback.setSelected(true);
            if (channelInfo.getState() == ChannelInfo.ChannelState.Online) {
                holder.channelName.setTextColor(Color.BLACK);
                holder.channelInfo.setTextColor(Color.GRAY);

            } else {
                holder.channelName.setTextColor(Color.GRAY);
                holder.channelInfo.setTextColor(Color.GRAY);
            }
        }
        return convertView;
    }

    class ViewHolder{
        private ImageView channelThumbnail;
        private TextView channelName;
        private TextView channelInfo;
        private TextView channelPlayback;

        public ViewHolder(View view){
            channelThumbnail = (ImageView) view.findViewById(R.id.channelListChannel);
            channelName = (TextView) view.findViewById(R.id.channelListChannelName);
            channelInfo = (TextView) view.findViewById(R.id.channelListChannelInfo);
            channelPlayback = (TextView) view.findViewById(R.id.channelListPlayback);
        }
    }
}
