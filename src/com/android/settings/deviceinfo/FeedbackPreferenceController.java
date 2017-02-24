package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.core.PreferenceController;
import com.android.settingslib.DeviceInfoUtils;

public class FeedbackPreferenceController extends PreferenceController {
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private final Fragment mHost;

    public FeedbackPreferenceController(Fragment host, Context context) {
        super(context);
        this.mHost = host;
    }

    public boolean isAvailable() {
        return !TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(this.mContext));
    }

    public String getPreferenceKey() {
        return KEY_DEVICE_FEEDBACK;
    }

    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DEVICE_FEEDBACK)) {
            return false;
        }
        if (!this.isAvailable()) {
            return false;
        }
        String reporterPackage = DeviceInfoUtils.getFeedbackReporterPackage(this.mContext);
        Intent intent = new Intent("android.intent.action.BUG_REPORT");
        intent.setPackage(reporterPackage);
        this.mHost.startActivityForResult(intent, 0);
        return true;
    }
}

