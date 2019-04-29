package com.ygwl.lz.lzvideoupdate.devices.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class DeviceNodeAdapter extends RecyclerView.Adapter{
    private Context context;
    private List<GroupInfo> mDataSet;
    private OnItemClickLinstener onItemClickLinstener;

    public DeviceNodeAdapter(Context context){
        this.context = context;
    }

    public void setDataSet(List<GroupInfo> mDataSet){
        this.mDataSet = mDataSet;
    }

    public void setOnItemClickLinstener(OnItemClickLinstener onItemClickLinstener){
        this.onItemClickLinstener = onItemClickLinstener;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.device_node_list_item, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(lp);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        String node = mDataSet.get(position).getGroupName();
        viewHolder.node.setText(node);
        viewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickLinstener.onItemClick(position);
            }
        });

        if(position == mDataSet.size() - 1){
            viewHolder.img.setVisibility(View.VISIBLE);
        }else{
            viewHolder.img.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDataSet == null ? 0 : mDataSet.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private LinearLayout layout;
        private TextView node;
        private ImageView img;

        public ViewHolder(View itemView) {
            super(itemView);
            layout = (LinearLayout) itemView.findViewById(R.id.device_node_layout);
            node = (TextView) itemView.findViewById(R.id.device_node_text);
            img = (ImageView) itemView.findViewById(R.id.device_node_image);
        }
    }

    public interface OnItemClickLinstener{
        public void onItemClick(int position);
    }
}
