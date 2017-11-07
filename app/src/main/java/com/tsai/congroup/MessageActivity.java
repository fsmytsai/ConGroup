package com.tsai.congroup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import MyMethod.ViewPagerAdapter;

public class MessageActivity extends MySharedActivity {
    public ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private TabLayout tabs;

    private boolean bound = false;
    private SignalrService signalrService;

    public SharedPreferences sp_lastData;
    public boolean isFromProFile = false;
    public String receiveAccount;
    public String memberName;
    public int roomId;
    public boolean me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        InitView(!getIntent().getBooleanExtra("FromNotifi", false), "聊天室");
        if (SharedService.identityView != null)
            BindSignalr();
        else
            finish();

        sp_lastData = getSharedPreferences("LastData", MODE_PRIVATE);
        receiveAccount = getIntent().getStringExtra("ReceiveAccount");
        if (receiveAccount != null) {
            memberName = getIntent().getStringExtra("MemberName");
            roomId = getIntent().getIntExtra("RoomId", -1);
            me = getIntent().getBooleanExtra("Me", false);
            isFromProFile = true;
        }
        initView();
    }

    private void initView() {
        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(new MessageFragment());
        fragments.add(new GroupChatRoomFragment());
        fragments.add(new PrivateChatRoomFragment());

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments, this);
        viewPagerAdapter.tabTitles = new String[]{"聊天訊息", "", ""};
        viewPagerAdapter.tabIcons = new int[]{1, R.drawable.chatgroup, R.drawable.chatperson};
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(0);
        viewPager.setOffscreenPageLimit(2);

        tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setTabMode(TabLayout.MODE_FIXED);
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);

        tabs.setupWithViewPager(viewPager);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    final RecyclerView rv_GroupChatRoomList = getGroupChatRoomFragment().rv_GroupChatRoomList;
                    rv_GroupChatRoomList.smoothScrollToPosition(0);
                } else if (tab.getPosition() == 0) {
                    final RecyclerView rv_MessageList = getMessageFragment().rv_MessageList;
                    rv_MessageList.smoothScrollToPosition(0);
                } else if (tab.getPosition() == 2) {
                    final RecyclerView rv_PrivateChatRoomList = getPrivateChatRoomFragment().rv_PrivateChatRoomList;
                    rv_PrivateChatRoomList.smoothScrollToPosition(0);
                }
            }
        });
    }

    public MessageFragment getMessageFragment() {
        return (MessageFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":0");
    }

    public GroupChatRoomFragment getGroupChatRoomFragment() {
        return (GroupChatRoomFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":1");
    }

    public PrivateChatRoomFragment getPrivateChatRoomFragment() {
        return (PrivateChatRoomFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":2");
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            SignalrService.LocalBinder binder = (SignalrService.LocalBinder) service;
            signalrService = binder.getService();
            bound = true;
            signalrService.setMessageActivity(MessageActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    private void BindSignalr() {
        Intent intent = new Intent(this, SignalrService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SharedService.identityView != null)
            BindSignalr();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bound) {
            signalrService.setMessageActivity(null); // unregister
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        finish();
        return null;
    }

    public void GetGMList(int GroupId, String GName) {
        viewPagerAdapter.tabTitles[0] = GName;
        tabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0);
        getMessageFragment().GRefresh(GroupId);
    }

    public void GetPMList(int ChatRoomId, String MemberName, boolean Me) {
        viewPagerAdapter.tabTitles[0] = MemberName;
        tabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0);
        getMessageFragment().PRefresh(ChatRoomId, Me);
    }
}
