/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceController;

public class TapToWakePreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";

    public TapToWakePreferenceController(Context context) {
        super(context);
    }

    @Override
    protected String getPreferenceKey() {
        return KEY_TAP_TO_WAKE;
    }

    @Override
    protected boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void updateState(PreferenceScreen screen) {
        final SwitchPreference preference =
                (SwitchPreference) screen.findPreference(KEY_TAP_TO_WAKE);
        if (preference != null) {
            int value = Settings.Secure.getInt(
                    mContext.getContentResolver(), Settings.Secure.DOUBLE_TAP_TO_WAKE, 0);
            preference.setChecked(value != 0);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        return true;
    }
}
