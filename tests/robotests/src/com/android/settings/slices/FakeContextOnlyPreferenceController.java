package com.android.settings.slices;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class FakeContextOnlyPreferenceController extends BasePreferenceController {

    public static final String KEY = "fakeController2";

    public FakeContextOnlyPreferenceController(Context context) {
        super(context, KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}