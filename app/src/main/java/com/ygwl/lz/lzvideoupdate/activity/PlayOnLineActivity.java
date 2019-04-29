package com.ygwl.lz.lzvideoupdate.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.android.dahua.playmanager.ITalkListener;
import com.android.dahua.playmanager.PlayManager;
import com.example.dhcommonlib.util.FileStorageUtil;
import com.example.dhcommonlib.util.MicHelper;
import com.mm.Api.Camera;
import com.mm.Api.DPSRTCamera;
import com.mm.Api.DPSRTCameraParam;
import com.mm.Api.Err;
import com.mm.audiotalk.target.DPSTalkTarget;
import com.mm.audiotalk.target.DpsTalk;
import com.mm.audiotalk.target.ITalkTarget;
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
public class PlayOnLineActivity extends BaseActivity  implements View.OnClickListener{
    private static final String TAG = PlayOnLineActivity.class.getSimpleName();
    public static final int Stream_Main_Type = 1;		//主码流 // the main stream
    public static final int Stream_Assist_Type = 2;		//辅码流 // auxiliary stream
    public static final int Stream_Third_Type = 3;		//三码流 // three stream

    public static final int KEY_Handler_Stream_Played = 1;
    public static final int KEY_Handler_First_Frame = 2;
    public static final int KEY_Handler_Net_Error = 3;
    public static final int KEY_Handler_Play_Failed = 4;
    public static final int KEY_Handler_Talk_Success = 7;
    public static final int KEY_Handler_Talk_failed = 8;

    private PlayWindow mPlayWin;
    protected PlayManager mPlayManager;
    private List<ChannelInfo> channelInfoList;
    protected Map<Integer, ChannelInfo> channelInfoMap = new HashMap<>();
    private DataAdapterInterface dataAdapterInterface;
    protected String[] recordPath;
    private boolean isFull = false;

    private RelativeLayout rlTitle;
    private LinearLayout llControlLayout;

    private TextView tvCapture;
    private TextView tvRecord;
    private TextView tvTalk;
    private TextView tvSound;
    private TextView tvPlay;
    private TextView tvCloud;
    private TextView tvStreamMain;
    private TextView tvStreamAssist;
    private TextView tvStreamThird;
    private TextView tvRemove;
    private TextView tvFull;

    private String IMGSTR =  "";
    private String IMAGE_PATH =  Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath() + "/snapshot/";

    protected Handler mPlayOnlineHander = new Handler() {
        public void handleMessage(Message msg) {
            dissmissProgressDialog();
            switch (msg.what){
                case KEY_Handler_Stream_Played:
                    int winIndex = (Integer) msg.obj;
                    tvPlay.setText(R.string.play_stop);
                    if(winIndex != mPlayManager.getSelectedWindowIndex()) {
                        return;
                    }
                    if(channelInfoList != null && channelInfoList.size() == 1) {
                        mPlayManager.maximizeWindow(winIndex);
                        mPlayManager.setEnableElectronZoom(winIndex, true);
                    }
                    if(mPlayManager.isNeedOpenAudio(winIndex)) {
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
                case KEY_Handler_Talk_Success:
                    dissmissProgressDialog();
                    toast(R.string.play_talk_start);
                    tvTalk.setSelected(true);
                    break;
                case KEY_Handler_Talk_failed:
                    dissmissProgressDialog();
                    closeTalk(mPlayManager.getSelectedWindowIndex());
                    toast(R.string.play_talk_failed);
                    tvTalk.setSelected(false);
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_play_online);
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
        tvCapture = (TextView) findViewById(R.id.capture);
        tvRecord = (TextView) findViewById(R.id.record);
        tvTalk = (TextView) findViewById(R.id.talk);
        tvPlay = (TextView) findViewById(R.id.play);
        tvSound = (TextView) findViewById(R.id.sound);
        tvCloud = (TextView) findViewById(R.id.cloud);
        tvStreamMain = (TextView) findViewById(R.id.stream_main);
        tvStreamAssist = (TextView) findViewById(R.id.stream_assist);
        tvStreamThird = (TextView) findViewById(R.id.stream_third);
        tvRemove = (TextView) findViewById(R.id.remove);
        tvFull = (TextView) findViewById(R.id.full);

        tvCapture.setOnClickListener(this);
        tvRecord.setOnClickListener(this);
        tvTalk.setOnClickListener(this);
        tvSound.setOnClickListener(this);
        tvPlay.setOnClickListener(this);
        tvCloud.setOnClickListener(this);
        tvStreamMain.setOnClickListener(this);
        tvStreamAssist.setOnClickListener(this);
        tvStreamThird.setOnClickListener(this);
        tvRemove.setOnClickListener(this);
        tvFull.setOnClickListener(this);
    }

    private void initData() {
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
        //设置对讲监听
        // set the intercom monitor.
        mPlayManager.setOnTalkListener(iTalkListener);
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
        initCommonWindow();
        channelInfoList = (List<ChannelInfo>) getIntent().getSerializableExtra("channel_info_list");
        int index = 0;
        for(ChannelInfo channelInfo : channelInfoList) {
            channelInfoMap.put(index, channelInfo);
            index ++;
        }

        playBatch();
    }

    /**
     * 初始化视频窗口
     *
     */
    public void initCommonWindow() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay()
                .getMetrics(metric);
        int mScreenWidth = metric.widthPixels; // 屏幕宽度（像素）
        int mScreenHeight = metric.heightPixels; // 屏幕高度（像素）
        if (!isFull) { // 竖屏
            mScreenHeight = mScreenWidth * 3 / 4;
        }
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mPlayWin.getLayoutParams();
        lp.width = mScreenWidth;
        lp.height = mScreenHeight;
        mPlayWin.setLayoutParams(lp);
        mPlayWin.forceLayout(mScreenWidth, mScreenHeight);
    }

    /**
     * 批量播放
     */
    private void playBatch(){
        mPlayManager.playBatch(getCameras());
    }

    private List<Camera> getCameras(){
        List<Camera> cameras = new ArrayList<>();
        for(ChannelInfo channelInfo : channelInfoList){
            cameras.add(getCamera(channelInfo));
        }
        return cameras;
    }

    /**
     * 设置播放camera的参数
     * @param channelInfo
     * @return
     */
    private DPSRTCamera getCamera(ChannelInfo channelInfo){
        //创建播放Camera参数
        // create playback Camera parameters.
        DPSRTCameraParam dpsrtCameraParam=new DPSRTCameraParam();
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
        if(mStreamType > Stream_Assist_Type) {
            mStreamType = Stream_Assist_Type;
        }
        dpsrtCameraParam.setStreamType(mStreamType);
        dpsrtCameraParam.setMediaType(3);
        dpsrtCameraParam.setCheckPermission(true);
        dpsrtCameraParam.setStartChannleIndex(-1);
        dpsrtCameraParam.setSeparateNum(0);
        dpsrtCameraParam.setTrackID("601");//h265视频播放

        DPSRTCamera dpsrtCamera=new DPSRTCamera(dpsrtCameraParam);
        return dpsrtCamera;
    }

    private void startPlay(int winIndex){
        mPlayManager.play(winIndex);
    }

    private void replay(){
        mPlayManager.replay();
    }

    private void stopPlay(int winIndex){
        mPlayManager.stop(winIndex);
        tvPlay.setText(R.string.play_play);
    }

    private void stopAll(){
        mPlayManager.stopAll(true);
    }

    private ITalkListener iTalkListener = new ITalkListener() {
        @Override
        public void onTalkResult(int winIndex, TalkResultType type) {
            if(type == TalkResultType.eTalkSuccess){
                if(mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendEmptyMessage(KEY_Handler_Talk_Success);
                }
            }else if(type == TalkResultType.eTalkFailed){
                if(mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendEmptyMessage(KEY_Handler_Talk_failed);
                }
            }
        }
    };

    private IMediaPlayListener iMediaPlayListener = new IMediaPlayListener() {
        @Override
        public void onPlayeStatusCallback(int winIndex, PlayStatusType type) {
            Log.d(TAG, "onPlayeStatusCallback:" + type + " winIndex: " + winIndex);
            Message msg = Message.obtain();
            msg.obj = winIndex;
            if(type == PlayStatusType.eStreamPlayed){
                msg.what = KEY_Handler_Stream_Played;
                if(mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            }else if(type == PlayStatusType.ePlayFirstFrame){
                msg.what = KEY_Handler_First_Frame;
                if(mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            }else if(type == PlayStatusType.eNetworkaAbort){
                msg.what = KEY_Handler_Net_Error;
                if(mPlayOnlineHander != null) {
                    mPlayOnlineHander.sendMessage(msg);
                }
            }else if(type == PlayStatusType.ePlayFailed){
                msg.what = KEY_Handler_Play_Failed;
                if(mPlayOnlineHander != null) {
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
            System.out.println("11111111111111 position= " +  position);
            refreshBtnState();
        }

        @Override
        public void onPageChange(int newPage, int prePage, int type) {
            Log.d(TAG, "onPageChange" + newPage + prePage + type);
            System.out.println("22222222222222");
            if(type == 0){
                if(mPlayManager.getPageCellNumber() == 1){
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
            }else if(type == IWindowListener.ControlType.Control_Reflash){
                startPlay(nWinIndex);
            }
        }

        @Override
        public void onSelectWinIndexChange(int newWinIndex, int oldWinIndex) {
            System.out.println("555555555555555newWinIndex =" + newWinIndex + "oldWinIndex = " + oldWinIndex);
            Log.d(TAG, "onSelectWinIndexChange:" + newWinIndex + ":" + oldWinIndex);
            if(!mPlayManager.hasTalking()) {
                if(mPlayManager.isOpenAudio(oldWinIndex)){
                    mPlayManager.closeAudio(oldWinIndex);
                    mPlayManager.setNeedOpenAudio(oldWinIndex, true);
                }

                if(mPlayManager.isPlaying(newWinIndex) && mPlayManager.isNeedOpenAudio(newWinIndex)) {
                    mPlayManager.openAudio(newWinIndex);
                }
                refreshBtnState();
            }
        }

        @Override
        public void onWindowDBClick(int winIndex, int type) {
            System.out.println("66666666666666");
            Log.d(TAG, "onWindowDBClick" + type + " winIndex:" + winIndex + " isWindowMax:" + mPlayManager.isWindowMax());
            if(mPlayManager.isOpenPtz(winIndex)){
                if(mPlayManager.setEnablePtz(winIndex, false) == Err.OK){
                    tvCloud.setSelected(false);
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

    private void sendPTZOperation(final ChannelInfo.PtzOperation operation, final boolean isStop){
        if(operation == null) {
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

    public static ChannelInfo.PtzOperation getPtzOperation(IPTZListener.PtzOperation oprType){
        ChannelInfo.PtzOperation operation              = ChannelInfo.PtzOperation.stop;
        if(oprType == IPTZListener.PtzOperation.up) {
            operation = ChannelInfo.PtzOperation.up;
        }
        if(oprType == IPTZListener.PtzOperation.down) {
            operation = ChannelInfo.PtzOperation.down;
        }
        if(oprType == IPTZListener.PtzOperation.left) {
            operation = ChannelInfo.PtzOperation.left;
        }
        if(oprType == IPTZListener.PtzOperation.right) {
            operation = ChannelInfo.PtzOperation.right;
        }
        if(oprType == IPTZListener.PtzOperation.leftUp) {
            operation = ChannelInfo.PtzOperation.leftUp;
        }
        if(oprType == IPTZListener.PtzOperation.leftDown) {
            operation = ChannelInfo.PtzOperation.leftDown;
        }
        if(oprType == IPTZListener.PtzOperation.rightUp) {
            operation = ChannelInfo.PtzOperation.rightUp;
        }
        if(oprType == IPTZListener.PtzOperation.rightDown) {
            operation = ChannelInfo.PtzOperation.rightDown;
        }

        return operation;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.capture:
                onClickCapture();
                break;
            case R.id.record:
                onClickRecord();
                break;
            case R.id.talk:
                onClickTalk();
                break;
            case R.id.sound:
                onClickSound();
                break;
            case R.id.play:
                int winIndex = mPlayManager.getSelectedWindowIndex();
                if(mPlayManager.isPlaying(winIndex)) {
                    stopPlay(winIndex);
                } else if(mPlayManager.getWindowChannelInfo(winIndex) != null){
                    startPlay(winIndex);
                }
                break;
            case R.id.cloud:
                onClickCloud();
                break;
            case R.id.stream_main:
                changeModeStream(Stream_Main_Type);
                break;
            case R.id.stream_assist:
                changeModeStream(Stream_Assist_Type);
                break;
            case R.id.stream_third:
                changeModeStream(Stream_Third_Type);
                break;
            case R.id.remove:
                removePlay(mPlayManager.getSelectedWindowIndex());
                break;
            case R.id.full:
                isFull = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                rlTitle.setVisibility(View.GONE);
                llControlLayout.setVisibility(View.GONE);
                initCommonWindow();
                setFullScreen(this);
                break;
        }
    }

    private void onClickCapture(){


//        IMGSTR =  new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
//        String path =  IMAGE_PATH + IMGSTR;
//        //先创建一个文件夹
//        File dir = new File(IMAGE_PATH);
//        File file = new File(IMAGE_PATH, IMGSTR);
//        if (!dir.exists()) {
//            dir.mkdir();
//        } else {
//            if (file.exists()) {
//                file.delete();
//            }
//        }



        if(!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        int currentWindowIndex = mPlayManager.getSelectedWindowIndex();
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).getPath() + "/snapshot/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        int ret = mPlayManager.snapShot(currentWindowIndex, path, true);
        if (ret == Err.OK) {
            toast(getText(R.string.play_capture_success) + path);
            MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
//            saveIntoMediaCore();
        } else {
            toast(R.string.play_capture_failed);
        }
    }

//    private void saveIntoMediaCore() {
//        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        //intent.setAction(MEDIA_ROUTER_SERVICE);
//        String a = IMAGE_PATH + IMGSTR;
//        Uri uri = Uri.parse(IMAGE_PATH + IMGSTR);
//        intent.setData(uri);
//        PlayOnlineActivity.this.setIntent(intent);
//    }

    private void onClickRecord(){
        if(mPlayManager.isRecording(mPlayManager.getSelectedWindowIndex())){
            stopRecord();
        }else{
            if(mPlayManager.hasRecording()){
                toast(R.string.play_back_recording);
            }
            startRecord();
        }
    }

    private void startRecord(){
        if(!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
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
        if(ret == Err.OK){
            toast(R.string.play_record_start);
            MediaScannerConnection.scanFile(this, recordPath, null, null);
            tvRecord.setSelected(true);
        }
    }

    protected void stopRecord(){
        int ret = mPlayManager.stopRecord(mPlayManager.getSelectedWindowIndex());
        if(ret == Err.OK){
            toast(getText(R.string.play_record_stop) + recordPath[0]);
            MediaScannerConnection.scanFile(this, recordPath, null, null);
            tvRecord.setSelected(false);
        }
    }

    private void onClickSound(){
        int currentWindowIndex = mPlayManager.getSelectedWindowIndex();
        if(!mPlayManager.isPlaying(currentWindowIndex)) {
            return;
        }
        tvTalk.setSelected(false);
        if(mPlayManager.isOpenAudio(currentWindowIndex) && closeAudio(currentWindowIndex)){
            tvSound.setSelected(false);
        }else {
            if(mPlayManager.hasTalking()) {
                toast(R.string.play_talk_close);
            }
            if(openAudio(currentWindowIndex)){
                tvSound.setSelected(true);
            }
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

    private void onClickTalk(){
        int winIndex = mPlayManager.getSelectedWindowIndex();
        if(!mPlayManager.isPlaying(winIndex)) {
            return;
        }
        tvSound.setSelected(false);
        if(mPlayManager.isTalking(winIndex)){
            closeTalk(winIndex);
        }else{
            if(!MicHelper.isVoicePermission()) {
                toast(R.string.play_talk_no_permission);
                return;
            }
            openTalk(winIndex);
        }
    }

    private void openTalk(int winIndex){
        if(!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        if(channelInfoMap.containsKey(winIndex)) {
            try {
                DpsTalk dpsTalk = new DpsTalk();
                dpsTalk.setCameraID(channelInfoMap.get(mPlayManager.getSelectedWindowIndex()).getDeviceUuid());
                dpsTalk.setDpHandle(String.valueOf(dataAdapterInterface.getDPSDKEntityHandle()));
                dpsTalk.setTalkType(1);
                dpsTalk.setTransMode(1);
                dpsTalk.setSampleRate(ITalkTarget.AUDIO_SAMPLE_RATE_8000);
                dpsTalk.setSampleDepth(ITalkTarget.AUDIO_SAMPLE_DEPTH_16);
                dpsTalk.setEncodeType(ITalkTarget.AUDIO_ENCODE_G711A);
                DPSTalkTarget dpsTalkTarget=new DPSTalkTarget(dpsTalk);
                mPlayManager.startTalk(winIndex, dpsTalkTarget);
            } catch (BusinessException e) {
                e.printStackTrace();
            }
            showProgressDialog();
        }
    }

    private void closeTalk(int winIndex){
        if(mPlayManager.stopTalk(winIndex) == Err.OK){
            toast(R.string.play_talk_close);
            tvTalk.setSelected(false);
        }
    }

    private void onClickCloud(){
        if(!mPlayManager.isPlayed(mPlayManager.getSelectedWindowIndex())) {
            return;
        }
        int windowIndex = mPlayManager.getSelectedWindowIndex();
        if(!mPlayManager.isPlaying(windowIndex)) {
            return;
        }
        if(mPlayManager.isOpenPtz(windowIndex)){
            if(mPlayManager.setEnablePtz(windowIndex, false) == Err.OK){
                tvCloud.setSelected(false);
                if(mPlayManager.isResumeFlag(windowIndex)){
                    mPlayManager.setResumeFlag(windowIndex, false);
                    mPlayManager.resumeWindow(windowIndex);
                }
            }
        }else{
            if(mPlayManager.setEnablePtz(windowIndex, true) == Err.OK){
                tvCloud.setSelected(true);
                if(!mPlayManager.isWindowMax()){
                    mPlayManager.setResumeFlag(windowIndex, true);
                    mPlayManager.maximizeWindow(windowIndex);
                }
            }
        }
    }

    private void changeModeStream(int streamType){
        if(mPlayManager.isOpenPtz(mPlayManager.getSelectedWindowIndex())) {
            tvCloud.setSelected(false);
        }
        if(mPlayManager.hasRecording()) {
            toast(R.string.play_record_stop_tips);
            tvRecord.setSelected(false);
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

        DPSRTCamera dpsrtCamera=new DPSRTCamera(dpsrtCameraParam);

        mPlayManager.playSingle(index, dpsrtCamera);
    }

    private void removePlay(int winIndex){
        if(channelInfoMap.containsKey(winIndex)) {
            channelInfoMap.remove(winIndex);
            channelInfoList.remove(winIndex);
            mPlayManager.removeStop(winIndex);
            refreshBtnState();
        }
    }

    private void refreshBtnState() {
        int winIndex = mPlayManager.getSelectedWindowIndex();
        tvRecord.setSelected(mPlayManager.isRecording(winIndex));
        tvTalk.setSelected(mPlayManager.isTalking(winIndex));
        tvCloud.setSelected(mPlayManager.isOpenPtz(winIndex));
        tvPlay.setText(mPlayManager.isPlaying(winIndex) ? R.string.play_stop : R.string.play_play);
        tvSound.setSelected(mPlayManager.isOpenAudio(winIndex));
        if(channelInfoMap.containsKey(winIndex)) {
            ChannelInfo channelInfo = channelInfoMap.get(winIndex);
            setSupportStreamTag(ChannelInfo.ChannelStreamType.getValue(channelInfo.getStreamType()));
        } else {
            setSupportStreamTag(-1);
        }
        if(mPlayManager.getWindowChannelInfo(winIndex) == null
                || mPlayManager.getWindowChannelInfo(winIndex).getCameraParam() == null
                || ((DPSRTCamera) mPlayManager.getWindowChannelInfo(winIndex).getCameraParam()).getCameraParam() == null) {
            tvStreamMain.setSelected(false);
            tvStreamAssist.setSelected(false);
            tvStreamThird.setSelected(false);
            return;
        }

        switch (((DPSRTCamera) mPlayManager.getWindowChannelInfo(winIndex).getCameraParam()).getCameraParam().getStreamType()) {
            case Stream_Main_Type:
                tvStreamMain.setSelected(true);
                tvStreamAssist.setSelected(false);
                tvStreamThird.setSelected(false);
                break;
            case Stream_Assist_Type:
                tvStreamMain.setSelected(false);
                tvStreamAssist.setSelected(true);
                tvStreamThird.setSelected(false);
                break;
            case Stream_Third_Type:
                tvStreamMain.setSelected(false);
                tvStreamAssist.setSelected(false);
                tvStreamThird.setSelected(true);
                break;
        }
    }

    public void setSupportStreamTag(int supportStreamTag){
        switch (supportStreamTag) {
            case Stream_Main_Type:
                tvStreamMain.setEnabled(true);
                tvStreamAssist.setEnabled(false);
                tvStreamThird.setEnabled(false);
                break;
            case Stream_Assist_Type:
                tvStreamMain.setEnabled(true);
                tvStreamAssist.setEnabled(true);
                tvStreamThird.setEnabled(false);
                break;
            case Stream_Third_Type:
                tvStreamMain.setEnabled(true);
                tvStreamAssist.setEnabled(true);
                tvStreamThird.setEnabled(true);
                break;

            default:
                tvStreamMain.setEnabled(false);
                tvStreamAssist.setEnabled(false);
                tvStreamThird.setEnabled(false);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(isFull) {
            isFull = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            rlTitle.setVisibility(View.VISIBLE);
            llControlLayout.setVisibility(View.VISIBLE);
            initCommonWindow();
            quitFullScreen(this);
        } else {
            this.finish();
        }
    }

    public static void setFullScreen(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }

    public static void quitFullScreen(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        activity.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
