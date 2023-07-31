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

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.R;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control Bluetooth MAP version
 */
public class BluetoothMapVersionPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String BLUETOOTH_SELECT_MAP_VERSION_KEY =
            "bluetooth_select_map_version";

    @VisibleForTesting
    static final String BLUETOOTH_MAP_VERSION_PROPERTY = "persist.bluetooth.mapversion";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public BluetoothMapVersionPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.bluetooth_map_version_values);
        mListSummaries = context.getResources().getStringArray(R.array.bluetooth_map_versions);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_MAP_VERSION_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(BLUETOOTH_MAP_VERSION_PROPERTY, newValue.toString());
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final String currentValue = SystemProperties.get(BLUETOOTH_MAP_VERSION_PROPERTY);
        int index = 0; // Defaults to MAP 1.2
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }
}
