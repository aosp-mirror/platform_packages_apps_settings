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
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

public class AmbientDisplayAlwaysOnPreferenceController extends TogglePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final int MY_USER = UserHandle.myUserId();

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
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(mContext);
        }
        return isAvailable(mConfig) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "ambient_display_always_on");
    }

    @Override
    public boolean isChecked() {
        return mConfig.alwaysOnEnabled(MY_USER);
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

    public static boolean isAlwaysOnEnabled(AmbientDisplayConfiguration config) {
        return config.alwaysOnEnabled(MY_USER);
    }

    public static boolean isAvailable(AmbientDisplayConfiguration config) {
        return config.alwaysOnAvailableForUser(MY_USER);
    }

    public static boolean accessibilityInversionEnabled(AmbientDisplayConfiguration config) {
        return config.accessibilityInversionEnabled(MY_USER);
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(mContext,
                AmbientDisplaySettings.class.getName(), getPreferenceKey(),
                mContext.getString(R.string.ambient_display_screen_title));

        return new InlineSwitchPayload(Settings.Secure.DOZE_ALWAYS_ON,
                ResultPayload.SettingsSource.SECURE, ON /* onValue */, intent, isAvailable(),
                ON /* defaultValue */);
    }
}
