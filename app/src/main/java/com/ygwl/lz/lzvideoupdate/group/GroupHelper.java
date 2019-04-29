package com.ygwl.lz.lzvideoupdate.group;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.android.business.adapter.DataAdapteeImpl;
import com.android.business.adapter.DataAdapterInterface;
import com.android.business.adapter.DeviceWithChannelList;
import com.android.business.adapter.DeviceWithChannelListBean;
import com.android.business.common.BaseHandler;
import com.android.business.common.BroadCase;
import com.android.business.common.HandleMessageCode;
import com.android.business.entity.ChannelInfo;
import com.android.business.entity.DataInfo;
import com.android.business.entity.DeviceInfo;
import com.android.business.entity.GroupInfo;
import com.android.business.exception.BusinessException;
import com.example.dhcommonlib.util.PreferencesHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by 26499 on 2018/7/19.
 */

public class GroupHelper {
    private  static ExecutorService executorService = Executors.newFixedThreadPool(3);
    private  static int fixLoadCount = 100;
    private DataAdapterInterface dataAdapterInterface;
    private Context context;
    private List<String> idList;
    private  byte[] taskHandlerLock = new byte[]{};
    private  byte[] failedTaskLock = new byte[]{};
    private HashSet<Integer> taskPages = new HashSet<>();
    private List<Integer> failedPages = new ArrayList<>();
    private static class Instance {
        static GroupHelper instance = new GroupHelper();
    }

    public static GroupHelper getInstance() {
        return GroupHelper.Instance.instance;
    }

    private GroupHelper() {
        if(dataAdapterInterface == null) dataAdapterInterface = DataAdapteeImpl.getInstance();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean LoadAllGroup() {
        startLoadGroup();
        startLoadDevice();
        return true;
    }

    private void startLoadGroup() {
        GroupFactory.getInstance().clearAll();
        String rootGroupId = PreferencesHelper.getInstance(context).getString("USER_GROUPID");
        List<GroupInfo> list = null;
        try {
            list = dataAdapterInterface.queryGroup(rootGroupId);
        }catch (BusinessException e) {
            e.printStackTrace();
        }
        if(list !=null && !list.isEmpty()){
            GroupFactory.getInstance().setRootGroupInfo(list.get(0));
            for(GroupInfo info : list) {
                GroupFactory.getInstance().putGroupInfo(info);
                ChannelFactory.getInstance().put(info.getGroupId(), info.getChannelList());
            }
        }

    }

    private void startLoadDevice() {
        idList = ChannelFactory.getInstance().getAllValues();
        int pageNo = 0;
        boolean isEnd = false;

        while (!isEnd /*&& handler.canRun()*/) {
            List<String> devIdList = getPageIdList(pageNo);
            if (devIdList != null && devIdList.size() != 0) {
                start1task(pageNo, devIdList);
            }

            if (devIdList == null || devIdList.size() < fixLoadCount){
                isEnd = true;
            }
            pageNo++;
        }
    }

    private List<String> getPageIdList(int pageNo) {
        int startIndex = pageNo * fixLoadCount;
        if (startIndex >= idList.size()) {
            return null;
        }
        int endIndex = startIndex + fixLoadCount;
        if (endIndex > idList.size()) {
            endIndex = idList.size();
        }
        return idList.subList(startIndex, endIndex);
    }

    private void start1task(final int pageNo, List<String>deviceList) {
        synchronized (taskHandlerLock) {
            taskPages.add(pageNo);
        }

        LoadRunnable loadRunnable = new LoadRunnable(deviceList, pageNo,new BaseHandler(Looper.getMainLooper()) {
            @Override
            public void handleBusiness(Message msg) {
                synchronized (taskHandlerLock) {
                    if (msg.what != HandleMessageCode.HMC_SUCCESS) {
                        synchronized (failedTaskLock) {
                            failedPages.add(pageNo);
                        }
                    } else {

                    }
                    taskPages.remove(pageNo);
                    try {
                        if(taskPages.size() <= 0) BroadCase.send(BroadCase.Action.DEVICE_ACTION_PUSH_MODIFY_DEVICE, null, context);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    class LoadRunnable implements Runnable {

        private List<String> mLoadIdList;
        BaseHandler mHandler;
        int pageNo;

        LoadRunnable(List<String> devIdList, int pageNo, BaseHandler handler) {
            this.pageNo = pageNo;
            mLoadIdList = devIdList;
            mHandler = handler;
            executorService.submit(this);
        }

        public void doBusiness() throws BusinessException {
            //Log.e(TAG, "doBusiness page " + pageNo);
            if (mLoadIdList != null && mLoadIdList.size() > 0) {
                loadChannelList(mLoadIdList);
                if (mHandler != null) {
                    mHandler.obtainMessage(HandleMessageCode.HMC_SUCCESS).sendToTarget();
                }
            }
        }

        @Override
        public void run() {
            try {
                doBusiness();
            } catch (BusinessException e) {
                mHandler.obtainMessage(HandleMessageCode.HMC_EXCEPTION, e.errorCode)
                        .sendToTarget();
            } catch (Exception e){
                mHandler.obtainMessage(HandleMessageCode.HMC_EXCEPTION)
                        .sendToTarget();
            }
        }
    }

    private void loadChannelList(List<String> channelIdList) {
        List<String> loadChannels = new ArrayList<>();
        for(String chnId : channelIdList) {
            if(!ChannelFactory.getInstance().isLoadChannelInfo(chnId)) {
                loadChannels.add(chnId);
            }
        }
        DeviceWithChannelList mDeviceWithChannelList = null;
        try {
            mDeviceWithChannelList = dataAdapterInterface.getDeviceListByChannelList(loadChannels);
        } catch (BusinessException e) {
            e.printStackTrace();
        }

        if(mDeviceWithChannelList != null) {
            resultListToFactory(channelIdList, mDeviceWithChannelList);
        }
    }

    private void resultListToFactory(List<String> channelIdList, DeviceWithChannelList mDeviceWithChannelList) {
        if(mDeviceWithChannelList != null) {
            if (mDeviceWithChannelList.getDevWithChannelList() != null) {
                for (DeviceWithChannelListBean deviceWithChannelListBean : mDeviceWithChannelList.getDevWithChannelList()) {
                    DeviceInfo deviceInfo = deviceWithChannelListBean.getDeviceInfo();
                    if(deviceWithChannelListBean.getChannelList() != null && deviceWithChannelListBean.getChannelList().size() > 0) {
                        for (DataInfo dataInfo : deviceWithChannelListBean.getChannelList()) {
                            ChannelInfo channelInfo = (ChannelInfo) dataInfo;
                            if(checkInList(channelIdList, channelInfo)){
                                channelInfo.setDeviceUuid(deviceInfo.getSnCode());
                                //此处获取视频通道，接口直接返回所有类型的通道，如报警通道，会产生重复数据，所以需要按需取通道，一般都是获取视频通道
                                if (channelInfo.getCategory() == ChannelInfo.ChannelCategory.videoInputChannel
                                        && (channelInfo.getRights() & ChannelInfo.Rights.REAL_MONITOR) != 0) {
                                    ChannelFactory.getInstance().putChannelInfo(channelInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkInList(List<String> chnIdList, ChannelInfo info) {
        if(chnIdList != null) {
            for(String id : chnIdList) {
                if(TextUtils.equals(id, info.getChnSncode())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void checkNullGroup() {
        GroupInfo rootInfo = GroupFactory.getInstance().getRootGroupInfo();
        List<GroupInfo> groupInfos = GroupFactory.getInstance().getAllGroupInfo();
        if(groupInfos != null) {
            for(GroupInfo info : groupInfos) {
                if(!TextUtils.equals(info.getGroupId(), rootInfo.getGroupId()) && ChannelFactory.getInstance().getAllChannelInfo(info).size() == 0) {
                    GroupFactory.getInstance().deleteGroupInfo(info);
                }
            }
        }
    }

    public DeviceWithChannelList getDeviceWithChannelList(List<ChannelInfo> channelInfos) {
        DeviceWithChannelList deviceWithChannelList = null;
        List<String> chnIdList = new ArrayList<>();
        for(ChannelInfo info : channelInfos) {
            chnIdList.add(info.getChnSncode());
        }
        try {
            deviceWithChannelList = dataAdapterInterface.getDeviceListByChannelList(chnIdList);
        } catch (BusinessException e) {
            e.printStackTrace();
        }

        return deviceWithChannelList;
    }

}
