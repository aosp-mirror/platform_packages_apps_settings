/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.core.TogglePreferenceController;

public class AmbientDisplayAlwaysOnPreferenceController extends TogglePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final int MY_USER = UserHandle.myUserId();
    private static final String PROP_AWARE_AVAILABLE = "ro.vendor.aware_available";

    private AmbientDisplayConfiguration mConfig;
    private OnPreferenceChangedCallback mCallback;

    public interface OnPreferenceChangedCallback {
        void onPreferenceChanged();
    }

    public AmbientDisplayAlwaysOnPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(getConfig())
                && !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false) ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "ambient_display_always_on");
    }

    @Override
    public boolean isChecked() {
        return getConfig().alwaysOnEnabled(MY_USER);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int enabled = isChecked ? ON : OFF;
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.DOZE_ALWAYS_ON, enabled);
        if (mCallback != null) {
            mCallback.onPreferenceChanged();
        }
        return true;
    }

    public AmbientDisplayAlwaysOnPreferenceController setConfig(
            AmbientDisplayConfiguration config) {
        mConfig = config;
        return this;
    }

    public AmbientDisplayAlwaysOnPreferenceController setCallback(
            OnPreferenceChangedCallback callback) {
        mCallback = callback;
        return this;
    }

    public static boolean isAvailable(AmbientDisplayConfiguration config) {
        return config.alwaysOnAvailableForUser(MY_USER);
    }

    private AmbientDisplayConfiguration getConfig() {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mConfig;
    }
}
