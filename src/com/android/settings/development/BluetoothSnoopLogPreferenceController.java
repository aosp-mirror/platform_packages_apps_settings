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
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothSnoopLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bt_hci_snoop_log";
    @VisibleForTesting static final int BTSNOOP_LOG_MODE_DISABLED_INDEX = 0;
    @VisibleForTesting static final int BTSNOOP_LOG_MODE_FILTERED_INDEX = 1;
    @VisibleForTesting static final int BTSNOOP_LOG_MODE_FULL_INDEX = 2;

    @VisibleForTesting
    static final String BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY = "persist.bluetooth.btsnooplogmode";

    private final String[] mListValues;
    private final String[] mListEntries;
    private DevelopmentSettingsDashboardFragment mFragment;

    public BluetoothSnoopLogPreferenceController(
            Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mListValues = context.getResources()
                .getStringArray(com.android.settingslib.R.array.bt_hci_snoop_log_values);
        mListEntries = context.getResources()
                .getStringArray(com.android.settingslib.R.array.bt_hci_snoop_log_entries);
        mFragment = fragment;
    }

    // Default mode is DISABLED. It can also be changed by modifying the global setting.
    public int getDefaultModeIndex() {
        if (!Build.IS_DEBUGGABLE) {
            return BTSNOOP_LOG_MODE_DISABLED_INDEX;
        }

        final String default_mode =
                Settings.Global.getString(
                        mContext.getContentResolver(),
                        Settings.Global.BLUETOOTH_BTSNOOP_DEFAULT_MODE);

        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(default_mode, mListValues[i])) {
                return i;
            }
        }

        return BTSNOOP_LOG_MODE_DISABLED_INDEX;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, newValue.toString());
        updateState(mPreference);
        if (mFragment != null) {
            mFragment.onSettingChanged();
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final String currentValue = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);

        int index = getDefaultModeIndex();
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListEntries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, null);
        ((ListPreference) mPreference).setValue(mListValues[getDefaultModeIndex()]);
        ((ListPreference) mPreference).setSummary(mListEntries[getDefaultModeIndex()]);
    }
}
