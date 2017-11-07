package com.tsai.congroup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by user on 2017/4/19.
 */

public class BootBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("test", "BootBroadCastReceiver");
        Intent nIntent = new Intent(context, SignalrService.class);
        context.startService(nIntent);
    }
}
