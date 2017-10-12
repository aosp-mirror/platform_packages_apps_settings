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

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SecondaryDisplayPreferenceController extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String OVERLAY_DISPLAY_DEVICES_KEY = "overlay_display_devices";

    private final String[] mListValues;
    private final String[] mListSummaries;
    private ListPreference mPreference;

    public SecondaryDisplayPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.overlay_display_devices_values);
        mListSummaries = context.getResources().getStringArray(
                R.array.overlay_display_devices_entries);
    }

    @Override
    public String getPreferenceKey() {
        return OVERLAY_DISPLAY_DEVICES_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSecondaryDisplayDevicesOption(newValue.toString());
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSecondaryDisplayDevicesOptions();
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        writeSecondaryDisplayDevicesOption(null);
        mPreference.setEnabled(false);
    }

    private void updateSecondaryDisplayDevicesOptions() {
        final String value = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES);
        int index = 0; // default
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(value, mListValues[i])) {
                index = i;
                break;
            }
        }
        mPreference.setValue(mListValues[index]);
        mPreference.setSummary(mListSummaries[index]);
    }

    private void writeSecondaryDisplayDevicesOption(String newValue) {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES, newValue);
        updateSecondaryDisplayDevicesOptions();
    }
}
