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

/**
 * This preference represents the default log level for the Bluetooth stack
 *
 * The default log level is captured and held in an Android Log Framework log tag, using "bluetooth"
 * as the tag name. The Log framework does not provide methods to directly write a log tag value,
 * but instead leverages special system properties to hold the value of a log tag.
 *
 * This preferences aims to keep the selection in sync with the currently set log tag value. It
 * writes directly to the system properties that hold the level associated with the bluetooth log
 * tag. It leverages the Log.isLoggable("bluetooth", level) function to discern the current value.
 * The default level is INFO.
 *
 * This value is read once at start of the Bluetooth stack. To use a new value once setting it, be
 * sure to turn Bluetooth off and back on again.
 */
public class BluetoothStackLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String TAG = BluetoothStackLogPreferenceController.class.getSimpleName();

    private static final String PREFERENCE_KEY = "bt_stack_log_level";

    /* Ensure that the indexes match with bt_stack_log_values and bt_stack_log_entries ordering */
    private static final int BT_LOG_LEVEL_VERBOSE_INDEX = 0;
    private static final int BT_LOG_LEVEL_DEBUG_INDEX = 1;
    private static final int BT_LOG_LEVEL_INFO_INDEX = 2;
    private static final int BT_LOG_LEVEL_WARN_INDEX = 3;
    private static final int BT_LOG_LEVEL_ERROR_INDEX = 4;
    @VisibleForTesting static final int BT_LOG_LEVEL_DEFAULT_INDEX = BT_LOG_LEVEL_INFO_INDEX;

    private static final String BT_LOG_TAG = "bluetooth";
    @VisibleForTesting static final String BT_LOG_LEVEL_PROP_PERSIST = "persist.log.tag.bluetooth";
    @VisibleForTesting static final String BT_LOG_LEVEL_PROP = "log.tag.bluetooth";

    // Values represents the untranslatable log level strings that should be used for writing to
    // system properties. Entries represents the translatable log level strings that should be used
    // in the UI to communicate to the user their options for this preference.
    private String[] mListValues;
    private String[] mListEntries;

    /**
     * Create a BluetoothStackLogPreferenceController instance
     */
    public BluetoothStackLogPreferenceController(@NonNull Context context) {
        super(context);
        mListValues = context.getResources().getStringArray(R.array.bt_stack_log_level_values);
        mListEntries = context.getResources().getStringArray(R.array.bt_stack_log_level_entries);
    }

    /**
     * Returns the preference key associated with this preference
     *
     * Note that this key is _usually_ a system property in and of itself, which is expected to hold
     * the value of the preference. In this case though, this key *does not* hold the preference. It
     * is only really used to tie this controller to the list preference defined in the XML file.
     *
     * @return the preference key associated with this preference
     */
    @Override
    @Nullable
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    /**
     * Update the state of the preference based on what the user has selected
     *
     * This function is invoked when the user has selected a new value for this preference. The new
     * value is the entry value at the index of the list the user has selected. This value will be
     * one of the values from the array returned in getEntryValues(). Specifically, this array is
     * set using R.array.bt_stack_log_level_values
     *
     * @param preference - the preference object to set the value of
     * @param newValue - the value the user has selected, as an Object
     * @return True when updated successfully
     */
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        Log.v(TAG, "onPreferenceChange(pref=" + preference + "value=" + newValue.toString() + ")");
        setBluetoothLogTag(newValue.toString());
        setBluetoothLogLevelIndex(getBluetoothLogLevelIndex());
        return true;
    }

    /**
     * Refresh the state of this preference based on the state stored on the system
     *
     * Read the Bluetooth stack log level from the underlying system property/log tag, and map that
     * level to the proper index in the values and entries array. Use those strings to set the value
     * and summary of the preference.
     *
     * @param preference - the preference object to refresh the state of
     */
    @Override
    public void updateState(@NonNull Preference preference) {
        Log.v(TAG, "updateState(pref=" + preference + "): refresh preference state");
        setBluetoothLogLevelIndex(getBluetoothLogLevelIndex());
    }

    /**
     * Notify this developer options preference of a change to developer options visibility
     *
     * We developer options are closed, we should clear out the value of this developer option
     * preference and revert it back to the default state of INFO.
     */
    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Log.v(TAG, "onDeveloperOptionsSwitchDisabled(): Revert stack log to default");
        setBluetoothLogTag(null);
        setBluetoothLogLevelIndex(BT_LOG_LEVEL_DEFAULT_INDEX);
    }

    /**
     * Set the system property values used by the Log framework to read the "bluetooth" log tag
     *
     * @param logLevel - the log level to set the Bluetooth stack minimum log level to
     */
    private void setBluetoothLogTag(@Nullable String logLevel) {
        Log.i(TAG, "setBluetoothLogTag(logLevel=" + logLevel + "): Set properties for log tag");
        SystemProperties.set(BT_LOG_LEVEL_PROP_PERSIST, logLevel);
        SystemProperties.set(BT_LOG_LEVEL_PROP, logLevel);
    }

    /**
     * Get the entry and value index corresponding to the current Bluetooth stack log level
     *
     * Since this preference uses an actual log tag and not a specific/private system property, we
     * can read the value using the Log.isLoggable() function with our "bluetooth" log tag that
     * represents the log level of the Bluetooth stack. This is safer than trying to replacate the
     * logic used in the Log framework around the various persist, ro, and blank variants of the tag
     *
     * If no value is present, INFO is used.
     *
     * @return the entry/value index corresponding to the current log level of the tag "bluetooth"
     */
    @VisibleForTesting
    public int getBluetoothLogLevelIndex() {
        int level = BT_LOG_LEVEL_DEFAULT_INDEX;
        if (Log.isLoggable(BT_LOG_TAG, Log.VERBOSE)) {
            level = BT_LOG_LEVEL_VERBOSE_INDEX;
        } else if (Log.isLoggable(BT_LOG_TAG, Log.DEBUG)) {
            level = BT_LOG_LEVEL_DEBUG_INDEX;
        } else if (Log.isLoggable(BT_LOG_TAG, Log.INFO)) {
            level = BT_LOG_LEVEL_INFO_INDEX;
        } else if (Log.isLoggable(BT_LOG_TAG, Log.WARN)) {
            level = BT_LOG_LEVEL_WARN_INDEX;
        } else if (Log.isLoggable(BT_LOG_TAG, Log.ERROR)) {
            level = BT_LOG_LEVEL_ERROR_INDEX;
        }
        Log.v(TAG, "getBluetoothLogLevelIndex() -> " + level);
        return level;
    }

    /**
     * Set the current Bluetooth stack log level displayed in the list for this preference
     *
     * @param index - the index representing the log level choice of this preference
     */
    private void setBluetoothLogLevelIndex(int index) {
        if (index < BT_LOG_LEVEL_VERBOSE_INDEX || index > BT_LOG_LEVEL_ERROR_INDEX) {
            Log.e(TAG, "setBluetoothLogLevelIndex(index=" + index + "): Log level invalid");
            return;
        }

        String value = mListValues[index];
        String entryValue = mListEntries[index];

        ListPreference preference = ((ListPreference) mPreference);
        if (preference == null) {
            Log.e(TAG, "setBluetoothLogLevelIndex(index=" + index + "): mPreference is null");
            return;
        }

        preference.setValue(value);
        preference.setSummary(entryValue);

        Log.i(TAG, "setBluetoothLogLevelIndex(index=" + index
                + "): Updated Bluetooth stack log level to value='" + value + "', entryValue='"
                + entryValue + "'");
    }
}
