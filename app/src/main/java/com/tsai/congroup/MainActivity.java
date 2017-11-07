package com.tsai.congroup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import ViewModel.IdentityView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends MySharedActivity {
    private static final int SEARCHGROUP_CODE = 99;
    public List<Integer> RecordGroupIdList = new ArrayList<>();
    private boolean IsFinishing = false;

    private boolean bound = false;
    private SignalrService signalrService;
    public ImageButton ib_Chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sp_Settings = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean IsNeverIntro = sp_Settings.getBoolean("IsNeverIntro", false);
        if (IsNeverIntro) {
            InitView(false, "");
            CheckLogon();
        } else {
            InitView(false, "ConGroup");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.MainFrameLayout, new IntroductionFragment(), "IntroductionFragment")
                    .commit();
        }
    }

    private void initView() {
        ib_Chat = new ImageButton(this);
        ib_Chat.setImageResource(R.drawable.chat);
        ib_Chat.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
        ib_Chat.setAdjustViewBounds(true);

        toolbar.post(new Runnable() {
            @Override
            public void run() {
                SharedService.toolBarHeight = toolbar.getHeight();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(SharedService.toolBarHeight, SharedService.toolBarHeight);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                ib_Chat.setLayoutParams(params);
                rl_toolBar.addView(ib_Chat);
            }
        });

        ib_Chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                startActivity(intent);
                ib_Chat.setImageResource(R.drawable.chat);
            }
        });

        EditText editText = new EditText(this);
        editText.setFocusable(false);
        editText.setHint("搜尋群組");
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchGroupActivity.class);
                startActivityForResult(intent, SEARCHGROUP_CODE);
            }
        });
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (myWidth * 0.7), RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        editText.setLayoutParams(params);
        rl_toolBar.addView(editText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCHGROUP_CODE && resultCode == RESULT_OK) {
            int GroupId = data.getIntExtra("GroupId", -1);
            if (GroupId != -1) {
                ConGroupFragment conGroupFragment = (ConGroupFragment) getSupportFragmentManager().findFragmentByTag("ConGroupFragment");
                conGroupFragment.GetGPosts(GroupId);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SharedService.identityView != null)
            BindSignalr();
    }

    public void CheckLogon() {
        //歡迎Loading畫面
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainFrameLayout, new SplashScreenFragment(), "SplashScreenFragment")
                .commit();


        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/GetIdentity")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", MainActivity.this);
                        InitView(false, "ConGroup");
                        toolbar.setVisibility(View.VISIBLE);
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.MainFrameLayout, new ReLinkFragment(), "ReLinkFragment")
                                .commit();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            InitView(false, "");
                            SharedService.identityView = new Gson().fromJson(ResMsg, IdentityView.class);
                            initView();
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.MainFrameLayout, new ConGroupFragment(), "ConGroupFragment")
                                    .commit();
                            BindSignalr();
                        } else if (StatusCode == 400) {
                            InitView(false, "登入");
                            SharedService.identityView = null;
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.MainFrameLayout, new LoginFragment(), "LoginFragment")
                                    .commit();

                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, MainActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void BindSignalr() {
        Intent intent = new Intent(this, SignalrService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            SignalrService.LocalBinder binder = (SignalrService.LocalBinder) service;
            signalrService = binder.getService();
            bound = true;
            signalrService.setMainActivity(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    public void onBackPressed() {

        ConGroupFragment conGroupFragment = (ConGroupFragment) getSupportFragmentManager().findFragmentByTag("ConGroupFragment");
        if (conGroupFragment != null) {
            if (conGroupFragment.getPostListFragment().postType) {
                //刪掉目前的
                RecordGroupIdList.remove(RecordGroupIdList.size() - 1);
                //目前是社團貼文
                if (RecordGroupIdList.size() > 0) {
                    //抓取前一個
                    conGroupFragment.GetGPosts(RecordGroupIdList.get(RecordGroupIdList.size() - 1));
                } else {
                    conGroupFragment.GetMPosts();
                }
            } else {
                //目前是會員貼文
                if (IsFinishing) {
                    finish();
                } else {
                    SharedService.ShowTextToast("再按一次退出", this);
                    IsFinishing = true;
                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        IsFinishing = false;
                    }
                }, 1500);
            }
        } else if (getSupportFragmentManager().findFragmentByTag("RegisterFragment") != null) {
            InitView(false, "登入");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.MainFrameLayout, new LoginFragment(), "LoginFragment")
                    .commit();
        } else if (getSupportFragmentManager().findFragmentByTag("IntroductionFragment") != null) {
            if (ib_Chat != null) {
                CheckLogon();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.dispatcher().cancelAll();
        if (bound) {
            signalrService.setMainActivity(null); // unregister
            unbindService(serviceConnection);
            bound = false;
        }
    }

    public void GoRegister(View v) {
        InitView(true, "註冊");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainFrameLayout, new RegisterFragment(), "RegisterFragment")
                .commit();
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        InitView(false, "登入");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainFrameLayout, new LoginFragment(), "LoginFragment")
                .commit();
        return null;
    }

    public void GotNewMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ib_Chat.setImageResource(R.drawable.chatl);
//                newMessageCount++;
//                tv_message.setText("聊天室(" + newMessageCount + ")");
//                tv_message.setTextColor(Color.RED);
            }
        });
    }

//    public void GotCall(final MemberView memberView, final int CallId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                new AlertDialog.Builder(MainActivity.this)
//                        .setTitle("來電話囉")
//                        .setMessage(memberView.Name + " 想與您視訊")
//                        .setNegativeButton("掛斷", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                RejectionCall(CallId);
//                            }
//                        })
//                        .setPositiveButton("接聽", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                AcceptCall(memberView.Account, CallId);
//                            }
//                        }).show();
//            }
//        });
//
//    }

//    private void AcceptCall(String Account, int CallId) {
//        Intent intent = new Intent(MainActivity.this, CallActivity.class);
//        intent.putExtra("IsCaller", false);
//        intent.putExtra("OtherAccount", Account);
//        intent.putExtra("CallId", CallId);
//        startActivity(intent);
//    }
//
//    private void RejectionCall(int CallId) {
//        RequestBody formBody = new FormBody.Builder()
//                .add("Context", "123")
//                .add("CallId", CallId + "")
//                .add("IsAccept", "false")
//                .build();
//
//        Request request = new Request.Builder()
//                .url(getString(R.string.BackEndPath) + "Api/CallApi/ProcessCall")
//                .put(formBody)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                SharedService.ShowTextToast("請檢察網路連線", MainActivity.this);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final int StatusCode = response.code();
//                final String ResMsg = response.body().string();
//
//                if (StatusCode == 200) {
//                } else if (StatusCode == 400) {
//                    SharedService.ShowErrorDialog(ResMsg, MainActivity.this);
//                } else {
//                    SharedService.ShowTextToast("ERROR:" + StatusCode, MainActivity.this);
//                }
//            }
//        });
//    }
}
