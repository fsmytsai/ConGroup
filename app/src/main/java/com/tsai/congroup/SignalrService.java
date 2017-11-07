package com.tsai.congroup;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import MyMethod.MyCookieCredentials;
import MyMethod.SharedService;
import ViewModel.GroupMessageView;
import ViewModel.IdentityView;
import ViewModel.MemberView;
import ViewModel.NotificationView;
import ViewModel.PrivateMessageView;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignalrService extends Service {
    private HubConnection connection = null;
    private HubProxy mHub = null;
    private final IBinder mBinder = new LocalBinder();
    private MainActivity mainActivity;
    private MessageActivity messageActivity;
    private OkHttpClient client;
    private Vibrator vibrator;

    public SignalrService() {

    }

    private BroadcastReceiver NetWorkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("test : ", "NetWorkReceiver");
            Intent nIntent = new Intent();
            nIntent.setClass(context, SignalrService.class);

            if (context != null) {
                if (SharedService.CheckNetWork(context)) {
                    if (connection == null) {
                        client = SharedService.GetClient(context);
                        //取最新通知
                        GetNewNList();
                    }
                } else {
                    if (connection != null) {
                        connection.stop();
                        connection = null;
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        Log.d("test : ", "onStartCommand");
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(NetWorkReceiver, mFilter);

        if (SharedService.CheckNetWork(this)) {
            client = SharedService.GetClient(this);

            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MemberApi/GetIdentity")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    final String ResMsg = response.body().string();
                    if (StatusCode == 200) {
                        SharedService.identityView = new Gson().fromJson(ResMsg, IdentityView.class);
                        UseSignalR();
                    }
                }
            });
        }
        return result;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (connection != null) {
            connection.stop();
            connection = null;
        }
        unregisterReceiver(NetWorkReceiver);
        Intent intent = new Intent(this, BootBroadCastReceiver.class);
        sendBroadcast(intent);
        Log.d("test : ", "onTaskRemoved");
    }

    @Override
    public void onDestroy() {
        if (connection != null) {
            connection.stop();
            connection = null;
        }
        unregisterReceiver(NetWorkReceiver);
        Intent intent = new Intent(this, BootBroadCastReceiver.class);
        sendBroadcast(intent);
        Log.d("test : ", "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        Log.d("test : ", "onBind");
        if (connection == null) {
            UseSignalR();
            Log.d("test : ", "onBindUseSignalR");
        }
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SignalrService getService() {
            // Return this instance of SignalRService so clients can call public methods
            return SignalrService.this;
        }
    }

    private void UseSignalR() {
        if (SharedService.identityView == null)
            return;
        Log.d("test : ", "UseSignalR");
        final String HUB_URL = getString(R.string.BackEndPath) + "signalr";
        final String HUB_NAME = "conGroupHub";
        SignalRFuture<Void> mSignalRFuture;
        if (connection != null) {
            connection.stop();
            connection = null;
            mHub = null;
        }
        connection = new HubConnection(HUB_URL);
        mHub = connection.createHubProxy(HUB_NAME);
        connection.setCredentials(new MyCookieCredentials());
        mSignalRFuture = connection.start(new ServerSentEventsTransport(connection.getLogger()));

        mHub.on("RequestNotification", new SubscriptionHandler1<String>() {
            @Override
            public void run(String NotifiId) {
                GetNotifiByNotifiId(NotifiId);
            }
        }, String.class);

        mHub.on("GotCall", new SubscriptionHandler2<MemberView, Integer>() {
            @Override
            public void run(final MemberView memberView, final Integer CallId) {
                Intent intent = new Intent(getApplicationContext(), CallActivity.class);
                intent.putExtra("IsCaller", false);
                intent.putExtra("OtherAccount", memberView.Account);
                intent.putExtra("CallId", CallId);
                String JsonMemberView = new Gson().toJson(memberView);
                intent.putExtra("MemberView", JsonMemberView);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(SignalrService.this);
//                builder.setSmallIcon(R.mipmap.ic_launcher);
//                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.callnotification);
//                builder.setContent(remoteViews);
//                builder.setAutoCancel(true);
//                Intent intent = new Intent(getApplicationContext(), CallActivity.class);
//                intent.putExtra("IsCaller", false);
//                intent.putExtra("OtherAccount", memberView.Account);
//                intent.putExtra("CallId", CallId);
//                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
//                        999,
//                        intent,
//                        PendingIntent.FLAG_CANCEL_CURRENT);
//
//                remoteViews.setTextViewText(R.id.tv_Message, memberView.Name + " 想與您視訊");
//                remoteViews.setOnClickPendingIntent(R.id.bt_AcceptCall, pendingIntent);
//                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//                manager.notify(999, builder.build());
//                if (mainActivity != null)
//                    mainActivity.GotCall(memberView, CallId);
            }
        }, MemberView.class, Integer.class);

        mHub.on("addGMessage", new SubscriptionHandler1<GroupMessageView>() {
            @Override
            public void run(GroupMessageView groupMessageView) {
                if (messageActivity != null) {
                    messageActivity.getMessageFragment().GetGMFromSignalr(groupMessageView);
                } else if (mainActivity != null) {
                    mainActivity.GotNewMessage();
                } else {
                    vibrator.vibrate(1000);
                    Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                    intent.putExtra("FromNotifi", true);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                            groupMessageView.GroupMessageList.get(0).GroupId,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                    String Content = groupMessageView.GroupMessageList.get(0).Content;
                    if (Content.length() > 12)
                        Content = Content.substring(0, 12) + "...";
                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("群組訊息")
                            .setContentText(groupMessageView.MemberNameList.get(0) + " : " + Content)
                            .setContentIntent(pendingIntent)
                            .setSound(soundUri)
                            .setAutoCancel(true)
                            .setWhen(System.currentTimeMillis())
                            .build();
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(groupMessageView.GroupMessageList.get(0).GroupId, notification);
                }
            }
        }, GroupMessageView.class);

        mHub.on("addPMessage", new SubscriptionHandler1<PrivateMessageView>() {
            @Override
            public void run(PrivateMessageView privateMessageView) {
                if (messageActivity != null) {
                    messageActivity.getMessageFragment().GetPMFromSignalr(privateMessageView);
                } else if (mainActivity != null) {
                    mainActivity.GotNewMessage();
                } else {
                    vibrator.vibrate(1000);
                    Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                    intent.putExtra("FromNotifi", true);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                            privateMessageView.PrivateMessageList.get(0).RoomId,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                    String Content = privateMessageView.PrivateMessageList.get(0).Content;
                    if (Content.length() > 12)
                        Content = Content.substring(0, 12) + "...";
                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("私人訊息")
                            .setContentText(privateMessageView.MemberName + " : " + Content)
                            .setContentIntent(pendingIntent)
                            .setSound(soundUri)
                            .setAutoCancel(true)
                            .setWhen(System.currentTimeMillis())
                            .build();
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(privateMessageView.PrivateMessageList.get(0).RoomId, notification);
                }
            }
        }, PrivateMessageView.class);

        //開啟連線
        try {
            mSignalRFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    public void setMessageActivity(MessageActivity activity) {
        messageActivity = activity;
    }

    public void GetNotifiByNotifiId(final String NotifiId) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/NotificationApi/GetNotifiByNotifiId?NotifiId=" + NotifiId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                if (StatusCode == 200) {
                    vibrator.vibrate(1000);
                    Gson gson = new Gson();
                    NotificationView tempView = gson.fromJson(ResMsg, NotificationView.class);
                    final int NotifiId = Integer.parseInt(tempView.NotificationList.get(0).NotifiId);
                    if (mainActivity != null) {
                        ConGroupFragment conGroupFragment = (ConGroupFragment) mainActivity.getSupportFragmentManager().findFragmentByTag("ConGroupFragment");
                        if (conGroupFragment != null) {
                            conGroupFragment.getNotifiListFragment().GetNFromSignalr(tempView);
                        }
                    } else {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("ViewItem", 2);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                                NotifiId,
                                intent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                        final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("通知")
                                .setContentText(tempView.NotificationList.get(0).Content)
                                .setContentIntent(pendingIntent)
                                .setSound(soundUri)
                                .setAutoCancel(true)
                                .setWhen(System.currentTimeMillis())
                                .build();
                        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        manager.notify(NotifiId, notification);
                    }
                }
            }
        });
    }

    private void GetNewNList() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/NotificationApi/GetNewNList")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                if (StatusCode == 200) {
                    if (connection == null) {
                        Log.d("test : ", "GetNewNListUseSignalR");
                        UseSignalR();
                    }
                    Gson gson = new Gson();
                    NotificationView tempView = gson.fromJson(ResMsg, NotificationView.class);
                    if (mainActivity != null) {
                        ConGroupFragment conGroupFragment = (ConGroupFragment) mainActivity.getSupportFragmentManager().findFragmentByTag("ConGroupFragment");
                        if (conGroupFragment != null) {
                            conGroupFragment.getNotifiListFragment().GetNFromSignalr(tempView);
                        }
                    } else {
                        for (int i = 0; i < tempView.NotificationList.size(); i++) {
                            int NotifiId = Integer.parseInt(tempView.NotificationList.get(i).NotifiId);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("ViewItem", 2);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                                    NotifiId,
                                    intent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                            final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("通知")
                                    .setContentText(tempView.NotificationList.get(i).Content)
                                    .setContentIntent(pendingIntent)
                                    .setSound(soundUri)
                                    .setAutoCancel(true)
                                    .setWhen(System.currentTimeMillis())
                                    .build();
                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            manager.notify(NotifiId, notification);
                        }
                    }
                }
            }
        });
    }


}
