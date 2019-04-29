package com.ygwl.lz.lzvideoupdate.activity;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.business.adapter.DataAdapteeImpl;
import com.android.business.adapter.DataAdapterInterface;
import com.android.business.entity.ChannelInfo;
import com.android.business.exception.BusinessException;
import com.android.dahua.playmanager.IMediaPlayListener;
import com.android.dahua.playmanager.IOperationListener;
import com.android.dahua.playmanager.IPTZListener;
import com.android.dahua.playmanager.PlayManager;
import com.example.dhcommonlib.util.FileStorageUtil;
import com.mm.Api.Camera;
import com.mm.Api.DPSRTCamera;
import com.mm.Api.DPSRTCameraParam;
import com.mm.Api.Err;
import com.mm.uc.IWindowListener;
import com.mm.uc.PlayWindow;
import com.ygwl.lz.lzvideoupdate.R;
import com.ygwl.lz.lzvideoupdate.base.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cx
 * @class describe
 * @time 2019/4/28 9:45
 */
public class PlayOnLineNewActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = PlayOnLineNewActivity.class.getSimpleName();

    public static final int KEY_Handler_Stream_Played = 1;
    public static final int KEY_Handler_First_Frame = 2;
    public static final int KEY_Handler_Net_Error = 3;
    public static final int KEY_Handler_Play_Failed = 4;

    private PlayWindow mPlayWin;
    protected PlayManager mPlayManager;
    private List<ChannelInfo> channelInfoList;
    protected Map<Integer, ChannelInfo> channelInfoMap = new HashMap<>();
    private DataAdapterInterface dataAdapterInterface;
    protected String[] recordPath;
    private boolean isFull = false;

    private RelativeLayout rlTitle;
    private LinearLayout llControlLayout;

    private Button mBtnCapture, mBtnBack;
    private CheckBox mChkPlay, mChkRecord, mChkCloud;
    private RelativeLayout mLayoutTop;
    private LinearLayout mLayoutBottom;
    private TextView mVideoName;

    //底部和顶部内容动画
    private Animation dismissTopAnim;
    private Animation showTopAnim;
    private Animation showBottomAnim;
    private Animation dismissBottomAnim;
    private Boolean isShow = true;

    private String IMGSTR = "";
    private String IMAGE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath() + "/snapshot/";

    protected Handler mPlayOnlineHander = new Handler() {
        public void handleMessage(Message msg) {
            dissmissProgressDialog();
            switch (msg.what) {
                case KEY_Handler_Stream_Played:
                    int winIndex = (Integer) msg.obj;
                    mChkPlay.setChecked(false);
                    if (winIndex != mPlayManager.getSelectedWindowIndex()) {
                        return;
                    }
                    if (channelInfoList != null && channelInfoList.size() == 1) {
                        mPlayManager.maximizeWindow(winIndex);
                        mPlayManager.setEnableElectronZoom(winIndex, true);
                    }
                    if (mPlayManager.isNeedOpenAudio(winIndex)) {
                        openAudio(winIndex);
                    }
                    refreshBtnState();
                    break;
                case KEY_Handler_First_Frame:
                    break;
                case KEY_Handler_Net_Error:
                    toast(R.string.play_net_error);
                case KEY_Handler_Play_Failed:
                    winIndex = (Integer) msg.obj;
                    stopPlay(winIndex);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_play_online_new);
        this.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initView();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayManager != null) {
            mPlayManager.uninit();
            mPlayManager = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        replay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAll();
    }

    private void initView() {
        rlTitle = (RelativeLayout) findViewById(R.id.playonline_title);
        llControlLayout = (LinearLayout) findViewById(R.id.playonline_control_layout);
        mPlayWin = (PlayWindow) findViewById(R.id.play_window);
        mBtnCapture = (Button) findViewById(R.id.btn_capture);
        mChkPlay = (CheckBox) findViewById(R.id.chk_play);
        mChkRecord = (CheckBox) findViewById(R.id.chk_record);
        mChkCloud = (CheckBox) findViewById(R.id.chk_cloud);
        mLayoutTop = (RelativeLayout) findViewById(R.id.layout_playonline_top);
        mLayoutBottom = (LinearLayout) findViewById(R.id.layout_playonline_bottom);
        mBtnBack = (Button) findViewById(R.id.btn_back);
        mVideoName = (TextView) findViewById(R.id.video_name);

        mBtnCapture.setOnClickListener(this);
        mChkPlay.setOnClickListener(this);
        mChkRecord.setOnClickListener(this);
        mChkCloud.setOnClickListener(this);
        mBtnBack.setOnClickListener(this);
    }

    private void initData() {

        //初始化动画
        dismissTopAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_top_anim_dismiss);
        showTopAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_top_anim_show);
        showBottomAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_bottom_anim_show);
        dismissBottomAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_bottom_anim_dismiss);

        dataAdapterInterface = DataAdapteeImpl.getInstance();
        //初始化playManager
        // initialize playManager.
        mPlayManager = new PlayManager();
        //初始化窗口数量，默认显示4个窗口，最多16窗口，若设置单窗口均设置为1
        // the number of initialization window, the default display is 4 Windows, up to 16 Windows, if the setting of single window is set to 1.
//        mPlayManager.init(this, 16, 4, mPlayWin);
//        mPlayManager.init(this, 16, 1, mPlayWin);
        mPlayManager.init(this, 4, 4, mPlayWin);
        //设置视频拉流超时时间
        mPlayManager.setVideoTimeOut(10);
        //设置播放监听
        // set play monitor.
        mPlayManager.setOnMediaPlayListener(iMediaPlayListener);
        //设置云台监听
        // set the cloud monitor.
        mPlayManager.setOnPTZListener(iptzListener);
        //设置窗口操作监听
        // set window operation to listen.
        mPlayManager.setOnOperationListener(iOperationListener);
        //初始化窗口大小
        // initialization window size.
//        initCommonWindow();
        channelInfoList = (List<ChannelInfo>) getIntent().getSerializableExtra("channel_info_list");
        int index = 0;
        for (ChannelInfo channelInfo : channelInfoList) {
            channelInfoMap.put(index, channelInfo);
            index++;
        }

        playBatch();
    }

    /**
     * 初始化视频窗口
     */
//    public void initCommonWindow() {
//        DisplayMetrics metric = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay()
//                .getMetrics(metric);
//        int mScreenWidth = metric.widthPixels; // 屏幕宽度（像素）
//        int mScreenHeight = metric.heightPixels; // 屏幕高度（像素）
//        if (!isFull) { // 竖屏
//            mScreenHeight = mScreenWidth * 3 / 4;
//        }
//        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mPlayWin.getLayoutParams();
//        lp.width = mScreenWidth;
//        lp.height = mScreenHeight;
//        mPlayWin.setLayoutParams(lp);
//        mPlayWin.forceLayout(mScreenWidth, mScreenHeight);
//    }

    /**
     * 批量播放
     */
    private void playBatch() {
        mPlayManager.playBatch(getCameras());
        if (channelInfoList != null && channelInfoList.size()>0) {
            setViewName(0);
        }
    }

    private List<Camera> getCameras() {
        List<Camera> cameras = new ArrayList<>();
        for (ChannelInfo channelInfo : channelInfoList) {
            cameras.add(getCamera(channelInfo));
        }
        return cameras;
    }

    /**
     * 设置播放camera的参数
     *
     * @param channelInfo
     * @return
     */
    private DPSRTCamera getCamera(ChannelInfo channelInfo) {
        //创建播放Camera参数
        // create playback Camera parameters.
        DPSRTCameraParam dpsrtCameraParam = new DPSRTCameraParam();
        //设置窗口要播放的通道ID
        // set the channel ID to play in the window.
        dpsrtCameraParam.setCameraID(channelInfo.getChnSncode());
        try {
            dpsrtCameraParam.setDpHandle(String.valueOf(dataAdapterInterface.getDPSDKEntityHandle()));
        } catch (BusinessException e) {
            e.printStackTrace();
        }
        //获取码流类型
        // access code flow type.
        int mStreamType = ChannelInfo.ChannelStreamType.getValue(channelInfo.getStreamType());
        dpsrtCameraParam.setStreamType(mStreamType);
        dpsrtCameraParam.setMediaType(3);
        dpsrtCameraParam.setCheckPermission(true);
        dpsrtCameraParam.setStartChannleIndex(-1);
        dpsrtCameraParam.setSeparateNum(0);
        dpsrtCameraParam.setTrackID("601");//h265视频播放

        DPSRTCamera dpsrtCamera = new DPSRTCamera(dpsrtCameraParam);
        return dpsrtCamera;
    }

    private void startPlay(int winIndex) {
        mPlayManager.play(winIndex);
    }

    private void replay() {
        mPlayManager.replay();
    }

    private void stopPlay(int winIndex) {
        mPlayManager.stop(winIndex);
        mChkPlay.setChecked(true);
    }

    private void stopAll() {
        mPlayManager.stopAll(true);
    }


    private IMediaPlayListener iMediaPlayListener = new IMediaPlayListener() {
        @Override
        public void onPlayeStatusCallback(int winIndex, PlayStatusType type) {
            Log.d(TAG, "onPlayeStatusCallback:" + type + " winIndex: " + winIndex);
            Message msg = Message.obtain();
            msg.obj = winIndex;
            if (type == PlayStatusType.eStreamPlayed) {
                msg.what = KEY_Handler_Stream_Played;
                if (mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            } else if (type == PlayStatusType.ePlayFirstFrame) {
                msg.what = KEY_Handler_First_Frame;
                if (mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            } else if (type == PlayStatusType.eNetworkaAbort) {
                msg.what = KEY_Handler_Net_Error;
                if (mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            } else if (type == PlayStatusType.ePlayFailed) {
                msg.what = KEY_Handler_Play_Failed;
                if (mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            }
        }
    };

    private IOperationListener iOperationListener = new IOperationListener() {
        @Override
        public void onWindowSelected(int position) {
            //当前选择视频的序号，可在此处进行修改当前查看视频名称等操作
            Log.d(TAG, "onWindowSelected" + position);
            System.out.println("11111111111111 position= " + position);
            //控制底部可顶部组件的隐藏和显示
            if(isShow) {
                hiddenView();
            } else {
                showView();
            }
            setViewName(position);

            refreshBtnState();
        }

        @Override
        public void onPageChange(int newPage, int prePage, int type) {
            Log.d(TAG, "onPageChange" + newPage + prePage + type);
            System.out.println("22222222222222");
            if (type == 0) {
                if (mPlayManager.getPageCellNumber() == 1) {
                    mPlayManager.setEnableElectronZoom(prePage, false);
                    mPlayManager.setEnableElectronZoom(newPage, true);
                }
                refreshBtnState();
            }
        }

        @Override
        public void onSplitNumber(int nCurCellNumber, int nCurPage, int nPreCellNumber, int nPrePage) {
            System.out.println("33333333333333333");
            Log.d(TAG, "onSplitNumber" + nCurCellNumber);
        }

        @Override
        public void onControlClick(int nWinIndex, IWindowListener.ControlType type) {
            System.out.println("444444444444444");
            Log.d(TAG, "onControlClick" + type);
            if (type == IWindowListener.ControlType.Control_Open) {
                //add channel
            } else if (type == IWindowListener.ControlType.Control_Reflash) {
                startPlay(nWinIndex);
            }
        }

        @Override
        public void onSelectWinIndexChange(int newWinIndex, int oldWinIndex) {
            System.out.println("555555555555555newWinIndex =" + newWinIndex + "oldWinIndex = " + oldWinIndex);
            Log.d(TAG, "onSelectWinIndexChange:" + newWinIndex + ":" + oldWinIndex);
//            if(!mPlayManager.hasTalking()) {
            if (mPlayManager.isOpenAudio(oldWinIndex)) {
                mPlayManager.closeAudio(oldWinIndex);
                mPlayManager.setNeedOpenAudio(oldWinIndex, true);
            }

            if (mPlayManager.isPlaying(newWinIndex) && mPlayManager.isNeedOpenAudio(newWinIndex)) {
                mPlayManager.openAudio(newWinIndex);
            }
            refreshBtnState();
//            }
        }

        @Override
        public void onWindowDBClick(int winIndex, int type) {
            System.out.println("66666666666666");
            Log.d(TAG, "onWindowDBClick" + type + " winIndex:" + winIndex + " isWindowMax:" + mPlayManager.isWindowMax());
            if (mPlayManager.isOpenPtz(winIndex)) {
                if (mPlayManager.setEnablePtz(winIndex, false) == Err.OK) {
                    mChkCloud.setChecked(false);
                    mPlayManager.setResumeFlag(winIndex, false);
                }
            }
            mPlayManager.setEnableElectronZoom(winIndex, type == 0);
        }

        @Override
        public void onMoveWindowBegin(int winIndex) {
            System.out.println("7777777777777777");
            Log.d(TAG, "onMoveWindowBegin");
        }

        @Override
        public void onMovingWindow(int winIndex, float x, float y) {
            System.out.println("888888888888888");
            Log.d(TAG, "onMovingWindow x:" + x + " y:" + y);
        }

        @Override
        public boolean onMoveWindowEnd(int winIndex, float x, float y) {
            System.out.println("99999999999999999");
            Log.d(TAG, "onMoveWindowEnd x:" + x + " y:" + y);
            return false;
        }
    };

    private void setViewName(int position) {
        //设置顶部视频名称
        if(channelInfoList != null && channelInfoList.size() > position) {
            mVideoName.setText(channelInfoList.get(position).getName());
        } else {
            mVideoName.setText("暂无视频");
        }
    }

    private IPTZListener iptzListener = new IPTZListener() {
        @Override
        public void onPTZControl(int winIndex, PtzOperation oprType, boolean isStop, boolean isLongPress) {
            Log.d(TAG, "onPTZControl oprType:" + oprType.toString());
            sendPTZOperation(getPtzOperation(oprType), isStop);
        }

        @Override
        public void onPTZZooming(int winIndex, float scale, PtzOperation oprType, PtzZoomState state) {
            Log.d(TAG, "onPTZZooming oprType:" + oprType == null ? "nul" : oprType.toString()
                    + " state:" + state == null ? "nul" : state.toString() + " scale:" + scale);
        }
    };

    private void sendPTZOperation(final ChannelInfo.PtzOperation operation, final boolean isStop) {
        if (operation == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dataAdapterInterface.operatePTZ(operation, channelInfoList.get(mPlayManager.getSelectedWindowIndex()).getUuid(), 4, isStop);
                } catch (BusinessException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static ChannelInfo.PtzOperation getPtzOperation(IPTZListener.PtzOperation oprType) {
        ChannelInfo.PtzOperation operation = ChannelInfo.PtzOperation.stop;
        if (oprType == IPTZListener.PtzOperation.up) {
            operation = ChannelInfo.PtzOperation.up;
        }
        if (oprType == IPTZListener.PtzOperation.down) {
            operation = ChannelInfo.PtzOperation.down;
        }
        if (oprType == IPTZListener.PtzOperation.left) {
            operation = ChannelInfo.PtzOperation.left;
        }
        if (oprType == IPTZListener.PtzOperation.right) {
            operation = ChannelInfo.PtzOperation.right;
        }
        if (oprType == IPTZListener.PtzOperation.leftUp) {
            operation = ChannelInfo.PtzOperation.leftUp;
        }
        if (oprType == IPTZListener.PtzOperation.leftDown) {
            operation = ChannelInfo.PtzOperation.leftDown;
        }
        if (oprType == IPTZListener.PtzOperation.rightUp) {
            operation = ChannelInfo.PtzOperation.rightUp;
        }
        if (oprType == IPTZListener.PtzOperation.rightDown) {
            operation = ChannelInfo.PtzOperation.rightDown;
        }

        return operation;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture:
                onClickCapture();
                break;
            case R.id.chk_record:
                onClickRecord();
                break;
            case R.id.chk_play:
                int winIndex = mPlayManager.getSelectedWindowIndex();
                if (mPlayManager.isPlaying(winIndex)) {
                    stopPlay(winIndex);
                } else if (mPlayManager.getWindowChannelInfo(winIndex) != null) {
                    startPlay(winIndex);
                }
                break;
            case R.id.chk_cloud:
                onClickCloud();
                break;
            case R.id.btn_back:
                //点击返回键
                this.finish();
                break;
        }
    }

    private void onClickCapture() {
        if (!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        int currentWindowIndex = mPlayManager.getSelectedWindowIndex();
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).getPath() + "/snapshot/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        int ret = mPlayManager.snapShot(currentWindowIndex, path, true);
        if (ret == Err.OK) {
            toast(getText(R.string.play_capture_success) + path);
            MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
        } else {
            toast(R.string.play_capture_failed);
        }
    }

    private void onClickRecord() {
        if (mPlayManager.isRecording(mPlayManager.getSelectedWindowIndex())) {
            stopRecord();
        } else {
            if (mPlayManager.hasRecording()) {
                toast(R.string.play_back_recording);
            }
            startRecord();
        }
    }

    private void startRecord() {
        if (!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        int currentWindowIndex = mPlayManager.getSelectedWindowIndex();
        recordPath = new String[2];
        recordPath[0] = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).getPath() + "/Records/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".dav";
        recordPath[1] = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).getPath() + "/Pictures/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        FileStorageUtil.createFilePath(null, recordPath[0]);
        FileStorageUtil.createFilePath(null, recordPath[1]);
        int ret = mPlayManager.startRecord(currentWindowIndex, recordPath, PlayManager.RECORDER_TYPE_DAV);
        if (ret == Err.OK) {
            toast(R.string.play_record_start);
            MediaScannerConnection.scanFile(this, recordPath, null, null);
            mChkRecord.setChecked(true);
        }
    }

    protected void stopRecord() {
        int ret = mPlayManager.stopRecord(mPlayManager.getSelectedWindowIndex());
        if (ret == Err.OK) {
            toast(getText(R.string.play_record_stop) + recordPath[0]);
            MediaScannerConnection.scanFile(this, recordPath, null, null);
            mChkRecord.setChecked(false);
        }
    }

    public boolean openAudio(int winIndex) {
        // TODO:check state
        return mPlayManager.openAudio(winIndex) == Err.OK;
    }

    public boolean closeAudio(int winIndex) {
        // TODO:check state
        return mPlayManager.closeAudio(winIndex) == Err.OK;
    }


    private void onClickCloud() {
        if (!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        int windowIndex = mPlayManager.getSelectedWindowIndex();
        if (!mPlayManager.isPlaying(windowIndex)) {
            return;
        }
        if (mPlayManager.isOpenPtz(windowIndex)) {
            if (mPlayManager.setEnablePtz(windowIndex, false) == Err.OK) {
                mChkCloud.setChecked(false);
                if (mPlayManager.isResumeFlag(windowIndex)) {
                    mPlayManager.setResumeFlag(windowIndex, false);
                    mPlayManager.resumeWindow(windowIndex);
                }
            }
        } else {
            if (mPlayManager.setEnablePtz(windowIndex, true) == Err.OK) {
                mChkCloud.setChecked(true);
                if (!mPlayManager.isWindowMax()) {
                    mPlayManager.setResumeFlag(windowIndex, true);
                    mPlayManager.maximizeWindow(windowIndex);
                }
            }
        }
    }

    private void changeModeStream(int streamType) {
        if (mPlayManager.isOpenPtz(mPlayManager.getSelectedWindowIndex())) {
            mChkCloud.setChecked(false);
        }
        if (mPlayManager.hasRecording()) {
            toast(R.string.play_record_stop_tips);
            mChkRecord.setChecked(false);
        }
        int index = mPlayManager.getSelectedWindowIndex();
        mPlayManager.stop(index);
        DPSRTCamera mDSSRTCamera = (DPSRTCamera) mPlayManager.getWindowChannelInfo(index).getCameraParam();

        DPSRTCameraParam dpsrtCameraParam = new DPSRTCameraParam();
        dpsrtCameraParam.setCameraID(mDSSRTCamera.getCameraParam().getCameraID());
        try {
            dpsrtCameraParam.setDpHandle(String.valueOf(dataAdapterInterface.getDPSDKEntityHandle()));
        } catch (BusinessException e) {
            e.printStackTrace();
        }
        dpsrtCameraParam.setStreamType(streamType);
        dpsrtCameraParam.setMediaType(mDSSRTCamera.getCameraParam().getMediaType());
        dpsrtCameraParam.setCheckPermission(true);
        dpsrtCameraParam.setStartChannleIndex(-1);
        dpsrtCameraParam.setSeparateNum(0);
        dpsrtCameraParam.setTrackID("601");//h265视频播放

        DPSRTCamera dpsrtCamera = new DPSRTCamera(dpsrtCameraParam);

        mPlayManager.playSingle(index, dpsrtCamera);
    }

    private void refreshBtnState() {
        int winIndex = mPlayManager.getSelectedWindowIndex();
        mChkRecord.setChecked(mPlayManager.isRecording(winIndex));
        mChkCloud.setChecked(mPlayManager.isOpenPtz(winIndex));
        mChkPlay.setChecked(mPlayManager.isPlaying(winIndex) ? false : true);
        if (mPlayManager.getWindowChannelInfo(winIndex) == null
                || mPlayManager.getWindowChannelInfo(winIndex).getCameraParam() == null
                || ((DPSRTCamera) mPlayManager.getWindowChannelInfo(winIndex).getCameraParam()).getCameraParam() == null) {
            return;
        }
    }

    //隐藏顶部和底部控件
    private void hiddenView() {
        isShow = false;
        mLayoutTop.startAnimation(dismissTopAnim);
        mLayoutTop.setVisibility(View.INVISIBLE);
        mLayoutBottom.startAnimation(dismissBottomAnim);
        mLayoutBottom.setVisibility(View.INVISIBLE);
    }
    //显示顶部和底部控件
    private void showView() {
        isShow = true;
        mLayoutBottom.setVisibility(View.VISIBLE);
        mLayoutBottom.startAnimation(showBottomAnim);
        mLayoutTop.setVisibility(View.VISIBLE);
        mLayoutTop.startAnimation(showTopAnim);
    }

}
