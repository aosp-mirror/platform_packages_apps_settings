package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.settings.core.PreferenceController;

public class SafetyLegalPreferenceController extends PreferenceController {

    private static final String KEY_SAFETY_LEGAL = "safetylegal";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";


    public SafetyLegalPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get(PROPERTY_URL_SAFETYLEGAL));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SAFETY_LEGAL;
    }
}
