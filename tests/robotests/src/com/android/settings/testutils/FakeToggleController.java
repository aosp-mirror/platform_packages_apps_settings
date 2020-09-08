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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.slices.SliceBackgroundWorker;

import java.io.IOException;

public class FakeToggleController extends TogglePreferenceController {

    public static final String AVAILABILITY_KEY = "fake_toggle_availability_key";

    public static final IntentFilter INTENT_FILTER = new IntentFilter(
            WifiManager.WIFI_AP_STATE_CHANGED_ACTION);

    private static final String SETTING_KEY = "toggle_key";

    private static final int ON = 1;
    private static final int OFF = 0;

    private boolean mIsAsyncUpdate = false;

    public FakeToggleController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_KEY, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY,
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
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return TestWorker.class;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return mIsAsyncUpdate;
    }

    public void setAsyncUpdate(boolean isAsyncUpdate) {
        mIsAsyncUpdate = isAsyncUpdate;
    }

    public static class TestWorker extends SliceBackgroundWorker<Void> {

        public TestWorker(Context context, Uri uri) {
            super(context, uri);
        }

        @Override
        protected void onSlicePinned() {
        }

        @Override
        protected void onSliceUnpinned() {
        }

        @Override
        public void close() {
        }
    }
}
