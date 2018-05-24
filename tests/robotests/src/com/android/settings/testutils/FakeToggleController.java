/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.settings.testutils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class FakeToggleController extends TogglePreferenceController {

    private String settingKey = "toggle_key";

    public static final String AVAILABILITY_KEY = "fake_toggle_availability_key";

    public static final IntentFilter INTENT_FILTER = new IntentFilter(
            WifiManager.WIFI_AP_STATE_CHANGED_ACTION);

    private final int ON = 1;
    private final int OFF = 0;

    private boolean mIsAsyncUpdate = false;

    public FakeToggleController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                settingKey, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), settingKey,
                isChecked ? ON : OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                AVAILABILITY_KEY, AVAILABLE);
    }

    @Override
    public IntentFilter getIntentFilter() {
        return INTENT_FILTER;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return mIsAsyncUpdate;
    }

    public void setAsyncUpdate(boolean isAsyncUpdate) {
        mIsAsyncUpdate = isAsyncUpdate;
    }
}
