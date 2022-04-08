package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.android.settings.Settings.TestingSettingsActivity;


public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {

    public TestingSettingsBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_SECRET_CODE)) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClass(context, TestingSettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
