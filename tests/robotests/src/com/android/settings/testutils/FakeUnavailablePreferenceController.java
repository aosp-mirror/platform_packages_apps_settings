package com.android.settings.testutils;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

public class FakeUnavailablePreferenceController extends BasePreferenceController {

    public static final String AVAILABILITY_KEY = "fake_availability_key";

    public FakeUnavailablePreferenceController(Context context) {
        super(context, "key");
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Global.getInt(mContext.getContentResolver(), AVAILABILITY_KEY, 0);
    }

    @Override
    public boolean isSliceable() {
        return true;
    }
}
