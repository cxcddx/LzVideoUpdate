package com.ygwl.lz.lzvideoupdate.group;

import android.text.TextUtils;

import com.android.business.entity.GroupInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 26499 on 2018/5/29.
 */

public class GroupFactory {
    private LinkedHashMap<String, LinkedHashMap<String, GroupInfo>> mGroupInfos = new LinkedHashMap<>();
    private LinkedHashMap<String, GroupInfo> mGroupInfo = new LinkedHashMap<>();
    private GroupInfo rootGroupInfo;
    // 单例对象
    private static class Instance {
        static GroupFactory instance = new GroupFactory();
    }

    public static GroupFactory getInstance() {
        return Instance.instance;
    }

    private GroupFactory() {

    }

    public void setRootGroupInfo(GroupInfo info) {
        rootGroupInfo = info;
    }

    public GroupInfo getRootGroupInfo() {
        return rootGroupInfo;
    }

    public void clearAll() {
        mGroupInfos.clear();
        mGroupInfo.clear();
    }

    public void putGroupInfo(GroupInfo info) {
        mGroupInfo.put(info.getGroupId(), info);
        if(!TextUtils.isEmpty(info.getGroupParentId())) {
            LinkedHashMap<String, GroupInfo> groupInfoList = new LinkedHashMap<>();
            if(mGroupInfos.containsKey(info.getGroupParentId())) groupInfoList = mGroupInfos.get(info.getGroupParentId());
            groupInfoList.put(info.getGroupId(), info);
            mGroupInfos.put(info.getGroupParentId(), groupInfoList);
        }
    }

    public GroupInfo getGroupInfo(String id) {
        return mGroupInfo.get(id);
    }

    public void deleteGroupInfo(GroupInfo info) {
        String id = info.getGroupId();
        String parentId = info.getGroupParentId();
        if(mGroupInfo.containsKey(id)) {
            mGroupInfo.remove(id);
        }
        if(mGroupInfos.containsKey(id)) {
            mGroupInfos.remove(id);
        }

        if(!TextUtils.isEmpty(parentId) && mGroupInfos.containsKey(parentId)) {
            LinkedHashMap<String, GroupInfo> groupInfoLinkedHashMap = mGroupInfos.get(parentId);
            if(groupInfoLinkedHashMap.containsKey(id)) {
                groupInfoLinkedHashMap.remove(id);
            }
        }
    }

    public List<GroupInfo> getAllGroupInfo() {
        ArrayList<GroupInfo> array = new ArrayList<>();
        Iterator<Map.Entry<String, LinkedHashMap<String, GroupInfo>>> iterator = mGroupInfos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, GroupInfo>> mapEntry = iterator.next();
            array.addAll(getGroupList(mapEntry));
        }
        return array;
    }

    public List<GroupInfo> getChildGroupInfos(String groupId) {
        ArrayList<GroupInfo> array = new ArrayList<GroupInfo>();
        if(mGroupInfos != null && mGroupInfos.get(groupId) != null) {
            LinkedHashMap<String, GroupInfo> hashMap = mGroupInfos.get(groupId);
            Iterator<Map.Entry<String, GroupInfo>> iterator = hashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, GroupInfo> mapEntry = iterator.next();
                array.add(mapEntry.getValue());
            }
        }

        return array;
    }

    private List<GroupInfo> getGroupList(Map.Entry<String, LinkedHashMap<String, GroupInfo>> entry) {
        ArrayList<GroupInfo> array = new ArrayList<GroupInfo>();
        Iterator<Map.Entry<String, GroupInfo>> iterator = entry.getValue().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, GroupInfo> mapEntry = iterator.next();
            array.add(mapEntry.getValue());
        }

        return array;
    }
}
