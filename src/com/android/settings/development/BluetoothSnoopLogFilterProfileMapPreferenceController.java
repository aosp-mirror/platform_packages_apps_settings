/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.sysprop.BluetoothProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.Locale;

/** Bluetooth Snoop Logger Map Profile Filter Preference Controller */
public class BluetoothSnoopLogFilterProfileMapPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "BluetoothSnoopLogFilterProfileMapPreferenceController";
    private static final String PREFERENCE_KEY = "bt_hci_snoop_log_filter_map";
    @VisibleForTesting static final int BTSNOOP_LOG_PROFILE_FILTER_MODE_DISABLED_INDEX = 0;
    @VisibleForTesting static final int BTSNOOP_LOG_PROFILE_FILTER_MODE_MAGIC_INDEX = 1;
    @VisibleForTesting static final int BTSNOOP_LOG_PROFILE_FILTER_MODE_HEADER_INDEX = 2;
    @VisibleForTesting static final int BTSNOOP_LOG_PROFILE_FILTER_MODE_FULL_FILTER_INDEX = 3;

    private final String[] mListValues;
    private final String[] mListEntries;
    private final String mProfilesFilterDisabledEntry;

    @VisibleForTesting
    static boolean isSnoopLogModeFilteredEnabled() {
        return (BluetoothProperties.snoop_log_mode()
                        .orElse(BluetoothProperties.snoop_log_mode_values.DISABLED)
                == BluetoothProperties.snoop_log_mode_values.FILTERED);
    }

    public BluetoothSnoopLogFilterProfileMapPreferenceController(Context context) {
        super(context);
        mListValues =
                context.getResources()
                        .getStringArray(com.android.settingslib.R
                                .array.bt_hci_snoop_log_profile_filter_values);
        mListEntries =
                context.getResources()
                        .getStringArray(com.android.settingslib.R
                                .array.bt_hci_snoop_log_profile_filter_entries);
        mProfilesFilterDisabledEntry =
                context.getResources()
                        .getString(R.string.bt_hci_snoop_log_filtered_mode_disabled_summary);
    }

    // Default mode is DISABLED.
    public int getDefaultModeIndex() {
        return BTSNOOP_LOG_PROFILE_FILTER_MODE_DISABLED_INDEX;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        BluetoothProperties.snoop_log_filter_profile_map(
                BluetoothProperties.snoop_log_filter_profile_map_values.valueOf(
                        newValue.toString().toUpperCase(Locale.US)));
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        if (isSnoopLogModeFilteredEnabled()) {
            mPreference.setEnabled(true);

            BluetoothProperties.snoop_log_filter_profile_map_values currentValue =
                    BluetoothProperties.snoop_log_filter_profile_map()
                            .orElse(
                                    BluetoothProperties.snoop_log_filter_profile_map_values
                                            .DISABLED);

            int index = getDefaultModeIndex();
            for (int i = 0; i < mListValues.length; i++) {
                if (currentValue
                        == BluetoothProperties.snoop_log_filter_profile_map_values.valueOf(
                                mListValues[i].toUpperCase(Locale.US))) {
                    index = i;
                    break;
                }
            }
            listPreference.setValue(mListValues[index]);
            listPreference.setSummary(mListEntries[index]);
        } else {
            mPreference.setEnabled(false);
            listPreference.setSummary(mProfilesFilterDisabledEntry);
        }
    }

    /** Called when the Bluetooth Snoop Log Mode changes. */
    public void onSettingChanged() {
        updateState(mPreference);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        mPreference.setEnabled(isSnoopLogModeFilteredEnabled());
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        BluetoothProperties.snoop_log_filter_profile_map(
                BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
        ((ListPreference) mPreference).setValue(mListValues[getDefaultModeIndex()]);
        ((ListPreference) mPreference).setSummary(mListEntries[getDefaultModeIndex()]);
        mPreference.setEnabled(false);
    }
}
