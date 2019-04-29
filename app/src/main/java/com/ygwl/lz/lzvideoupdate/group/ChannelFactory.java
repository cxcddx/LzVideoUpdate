package com.ygwl.lz.lzvideoupdate.group;

import android.text.TextUtils;

import com.android.business.entity.ChannelInfo;
import com.android.business.entity.GroupInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by 26499 on 2018/5/29.
 */

public class ChannelFactory {
    private HashMap<String, List<String>> mGroupChildMap = new HashMap<>();
    private LinkedHashMap<String, ChannelInfo> mChannelInfo = new LinkedHashMap<>();
    byte[] threadLock = new byte[] {};
    // 单例对象
    private static class Instance {
        static ChannelFactory instance = new ChannelFactory();
    }

    public static ChannelFactory getInstance() {
        return Instance.instance;
    }

    private ChannelFactory() {

    }

    public void clearAll() {
        mChannelInfo.clear();
    }

    public void putChannelInfo(ChannelInfo info) {
        mChannelInfo.put(info.getChnSncode(), info);
    }

    public ChannelInfo getChannelInfo(String id) {
        return mChannelInfo.get(id);
    }

    public boolean isLoadChannelInfo(String id) {
        return mChannelInfo.containsKey(id);
    }

    public List<ChannelInfo> getChannelInfoListById(List<String> channelIds) {
        List<ChannelInfo> channelInfos = new ArrayList<>();
        if(channelIds != null) {
            for(String id : channelIds) {
                if(mChannelInfo.containsKey(id)) channelInfos.add(mChannelInfo.get(id));
            }
        }

        return channelInfos;
    }

    public List<ChannelInfo> getAllChannelInfo() {
        ArrayList<ChannelInfo> array = new ArrayList<ChannelInfo>();

        Iterator<Map.Entry<String, ChannelInfo>> iterator = mChannelInfo.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ChannelInfo> mapEntry = iterator.next();
            array.add(mapEntry.getValue());
        }

        return array;
    }

    public List<ChannelInfo> getAllChannelInfo(GroupInfo groupInfo) {
        ArrayList<ChannelInfo> array = new ArrayList<>();
        array.addAll(getChannelInfoListById(groupInfo.getChannelList()));
        if(groupInfo.isHasChild()) array.addAll(getChildChannelInfo(groupInfo));

        return array;
    }

    public List<ChannelInfo> getChildChannelInfo(GroupInfo groupInfo) {
        ArrayList<ChannelInfo> array = new ArrayList<>();
        List<GroupInfo> childGroupInfos = GroupFactory.getInstance().getChildGroupInfos(groupInfo.getGroupId());
        if(childGroupInfos != null && childGroupInfos.size() > 0) {
            for(GroupInfo info : childGroupInfos) {
                array.addAll(getChannelInfoListById(info.getChannelList()));
                if(info.isHasChild()) {
                    array.addAll(getChildChannelInfo(info));
                }
            }
        }

        return array;
    }

    public boolean put(String groupId, List<String> nodeList) {
        if (TextUtils.isEmpty(groupId) || nodeList == null || nodeList.size() == 0) {
            return false;
        }
        synchronized (threadLock) {
            mGroupChildMap.put(groupId, nodeList);
        }
        return true;
    }

    public List<String> get(String groupId) {
        List<String> childList = new ArrayList<>();
        List<String> temp;
        synchronized (threadLock) {
            temp = mGroupChildMap.get(groupId);
        }
        if (temp != null) {
            childList.addAll(temp);
        }
        return childList;
    }

    public List<String> getAllValues() {
        LinkedList<String> values = new LinkedList<>();
        synchronized (threadLock) {
            Iterator<Map.Entry<String, List<String>>> it= mGroupChildMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, List<String>> entry = it.next();
                List<String> elements = entry.getValue();
                values.addAll(elements);
            }
        }
        return values;
    }

    public boolean setChannelInfoState(String id, int state) {
        if(mChannelInfo.containsKey(id)) {
            ChannelInfo info = mChannelInfo.get(id);
            info.setState(state);
            return true;
        }

        return false;
    }
}
