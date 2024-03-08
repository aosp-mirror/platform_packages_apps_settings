/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothStackLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    /* Ensure that the indexes match with bt_stack_log_values and bt_stack_log_entries ordering */
    private static final String PREFERENCE_KEY = "bt_stack_log_level";
    @VisibleForTesting static final int BTSTACK_LOG_MODE_VERBOSE_INDEX = 0;
    @VisibleForTesting static final int BTSTACK_LOG_MODE_DEBUG_INDEX = 1;
    @VisibleForTesting static final int BTSTACK_LOG_MODE_INFO_INDEX = 2;
    @VisibleForTesting static final int BTSTACK_LOG_MODE_WARN_INDEX = 3;
    @VisibleForTesting static final int BTSTACK_LOG_MODE_ERROR_INDEX = 4;

    @VisibleForTesting
    static final String BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST = "persist.log.tag.bluetooth";
    static final String BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY = "log.tag.bluetooth";
    static final String BLUETOOTH_STRING_NAME = "bluetooth";
    static final int DEFAULT_MODE = BTSTACK_LOG_MODE_INFO_INDEX;

    private final String[] mListValues;
    private final String[] mListEntries;


    public BluetoothStackLogPreferenceController(@NonNull Context context) {
        super(context);
        mListValues = context.getResources().getStringArray(R.array.bt_stack_log_level_values);
        mListEntries = context.getResources().getStringArray(R.array.bt_stack_log_level_entries);
    }

    /** returns default log level index of INFO */
    public int getDefaultModeIndex() {
        return DEFAULT_MODE;
    }

    @Override
    @Nullable
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        SystemProperties.set(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST, newValue.toString());
        SystemProperties.set(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY, newValue.toString());
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        int index = getBluetoothLogLevelIndex();
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListEntries[index]);
    }

    /**
     *  Returns the current log level from Log.isLoggable().
     */
    @VisibleForTesting
    public int getBluetoothLogLevelIndex() {
        if (Log.isLoggable(BLUETOOTH_STRING_NAME, Log.VERBOSE)) {
            return BTSTACK_LOG_MODE_VERBOSE_INDEX;
        } else if (Log.isLoggable(BLUETOOTH_STRING_NAME, Log.DEBUG)) {
            return BTSTACK_LOG_MODE_DEBUG_INDEX;
        } else if (Log.isLoggable(BLUETOOTH_STRING_NAME, Log.INFO)) {
            return BTSTACK_LOG_MODE_INFO_INDEX;
        } else if (Log.isLoggable(BLUETOOTH_STRING_NAME, Log.WARN)) {
            return BTSTACK_LOG_MODE_WARN_INDEX;
        } else if (Log.isLoggable(BLUETOOTH_STRING_NAME, Log.ERROR)) {
            return BTSTACK_LOG_MODE_ERROR_INDEX;
        }
        return BTSTACK_LOG_MODE_INFO_INDEX;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST, null);
        SystemProperties.set(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY, null);
        ((ListPreference) mPreference).setValue(mListValues[getDefaultModeIndex()]);
        ((ListPreference) mPreference).setSummary(mListEntries[getDefaultModeIndex()]);
    }
}
