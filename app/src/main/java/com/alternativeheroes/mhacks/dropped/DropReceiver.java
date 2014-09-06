package com.alternativeheroes.mhacks.dropped;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class DropReceiver extends BroadcastReceiver {
    private static final String TAG = "DropReceiver";

    public DropReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(Constants.dropEnabled, true)) {
                Intent dropService = new Intent(context,DropService.class);
                context.startService(dropService);
            }
        }
        else if(intent.getAction().equals(Constants.dropEventAction)){
            Log.d(TAG, "Drop phone event");
        }
    }
}
