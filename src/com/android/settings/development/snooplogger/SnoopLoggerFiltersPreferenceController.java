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

package com.android.settings.development.snooplogger;

import android.content.Context;
import android.sysprop.BluetoothProperties;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/** A {@link BasePreferenceController} used in {@link SnoopLoggerFiltersDashboard} */
public class SnoopLoggerFiltersPreferenceController extends BasePreferenceController {
    private static final String TAG = "SnoopLoggerFiltersPreferenceController";

    private final String[] mListValues;
    private final String[] mListEntries;

    public SnoopLoggerFiltersPreferenceController(Context context, String key) {
        super(context, key);
        mListValues =
                context.getResources().getStringArray(
                        com.android.settingslib.R.array.bt_hci_snoop_log_filters_values);
        mListEntries =
                context.getResources().getStringArray(
                        com.android.settingslib.R.array.bt_hci_snoop_log_filters_entries);
    }

    @Override
    public int getAvailabilityStatus() {
        BluetoothProperties.snoop_log_mode_values snoopLogMode =
                BluetoothProperties.snoop_log_mode()
                        .orElse(BluetoothProperties.snoop_log_mode_values.DISABLED);
        if (snoopLogMode == BluetoothProperties.snoop_log_mode_values.FILTERED) {
            return AVAILABLE;
        }
        return DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceGroup mGroup = screen.findPreference(getPreferenceKey());
        mGroup.removeAll();
        final Context prefContext = mGroup.getContext();
        for (int i = 0; i < mListValues.length; i++) {
            mGroup.addPreference(
                    new SnoopLoggerFiltersPreference(prefContext, mListValues[i], mListEntries[i]));
        }
    }
}
