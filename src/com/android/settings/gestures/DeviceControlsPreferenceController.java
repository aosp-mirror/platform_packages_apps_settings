/*
 * Copyright (C) 2020 The Android Open Source Project
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
 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;

public class DeviceControlsPreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "device_controls_video";

    @VisibleForTesting
    protected static final String ENABLED_SETTING = Settings.Secure.CONTROLS_ENABLED;

    @VisibleForTesting
    protected static final String TOGGLE_KEY = "gesture_device_controls_switch";

    public DeviceControlsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean available = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CONTROLS);
        return available ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), ENABLED_SETTING,
                isChecked ? 1 : 0);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), TOGGLE_KEY);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean isChecked() {
        int enabled = Settings.Secure.getInt(mContext.getContentResolver(), ENABLED_SETTING, 1);
        return enabled == 1;
    }
}
