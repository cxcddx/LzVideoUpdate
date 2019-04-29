package com.ygwl.lz.lzvideoupdate.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.business.entity.ChannelInfo;
import com.ygwl.lz.lzvideoupdate.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cx
 * @class describe
 * @time 2019/4/28 13:14
 */
public class TestActivity extends Activity {

    private Button mBtn;
    private List<ChannelInfo> channelInfos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        mBtn = findViewById(R.id.login);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
                Intent intent = new Intent(TestActivity.this, PlayOnLineActivity.class);
                intent.putExtra("channel_info_list", (Serializable) channelInfos);
                startActivity(intent);
            }
        });
    }

    private void initData() {
        ChannelInfo info = new ChannelInfo();
        info.setChnSncode("1000013$1$0$1");
        info.setStreamType(ChannelInfo.ChannelStreamType.Main);
        info.setName("垃圾处理厂_2");

        ChannelInfo info2 = new ChannelInfo();
        info2.setChnSncode("1000013$1$0$2");
        info2.setStreamType(ChannelInfo.ChannelStreamType.Main);
        info2.setName("垃圾处理厂_3");

        channelInfos.add(info);
        channelInfos.add(info2);
    }
}
