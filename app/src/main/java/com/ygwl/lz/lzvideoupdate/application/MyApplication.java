package com.ygwl.lz.lzvideoupdate.application;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * @author cx
 * @class describe
 * @time 2019/4/28 9:16
 */
public class MyApplication extends Application {
    private static MyApplication instance;

    public static synchronized MyApplication getApplication() {
        if (instance == null) {
            throw new NullPointerException("app not create or be terminated!");
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        loadLibrary();
//        try {
//            init();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private void loadLibrary(){
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("dsl");
        System.loadLibrary("dslalien");
        System.loadLibrary("DPRTSPSDK");
        System.loadLibrary("PlatformRestSDK");
        System.loadLibrary("PlatformSDK");
        System.loadLibrary("netsdk");
        System.loadLibrary("CommonSDK");
    }
    //限制字体尺寸，避免受系统字体大小影响导致界面不适配
    // limit the font size to avoid the interface mismatch caused by system font size.
    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        resources.updateConfiguration(configuration,resources.getDisplayMetrics());
        return resources;
    }

    //估计为推送的配置，当前暂不需要，
//    private void init() throws Exception{
//
//        DSSPush mDSSPush = new DSSPush();
//        boolean bDSSOk = mDSSPush.init(this.getApplicationContext());
//    }

    @Override
    public String getPackageName() {
        return super.getPackageName();
    }
}
