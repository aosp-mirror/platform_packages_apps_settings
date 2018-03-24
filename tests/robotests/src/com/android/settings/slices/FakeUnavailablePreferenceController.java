package com.android.settings.slices;

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
        return Settings.System.getInt(mContext.getContentResolver(),
                AVAILABILITY_KEY, 0);
    }
}
