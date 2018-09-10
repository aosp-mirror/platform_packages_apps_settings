/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoTimeZonePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_TIME_ZONE = "auto_zone";

    private final boolean mIsFromSUW;
    private final UpdateTimeAndDateCallback mCallback;

    public AutoTimeZonePreferenceController(Context context, UpdateTimeAndDateCallback callback,
            boolean isFromSUW) {
        super(context);
        mCallback = callback;
        mIsFromSUW = isFromSUW;
    }

    @Override
    public boolean isAvailable() {
        return !(Utils.isWifiOnly(mContext) || mIsFromSUW);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_TIME_ZONE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean autoZoneEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE,
                autoZoneEnabled ? 1 : 0);
        mCallback.updateTimeAndDateDisplay(mContext);
        return true;
    }

    public boolean isEnabled() {
        return isAvailable() && Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 0) > 0;
    }
}
