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
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothAvrcpVersionPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String BLUETOOTH_SELECT_AVRCP_VERSION_KEY =
            "bluetooth_select_avrcp_version";

    @VisibleForTesting
    static final String BLUETOOTH_AVRCP_VERSION_PROPERTY = "persist.bluetooth.avrcpversion";

    private final String[] mListValues;
    private final String[] mListSummaries;
    private ListPreference mPreference;

    public BluetoothAvrcpVersionPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.bluetooth_avrcp_version_values);
        mListSummaries = context.getResources().getStringArray(R.array.bluetooth_avrcp_versions);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_AVRCP_VERSION_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(BLUETOOTH_AVRCP_VERSION_PROPERTY, newValue.toString());
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final String currentValue = SystemProperties.get(BLUETOOTH_AVRCP_VERSION_PROPERTY);
        int index = 0; // Defaults to AVRCP 1.4
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        mPreference.setValue(mListValues[index]);
        mPreference.setSummary(mListSummaries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
    }
}
