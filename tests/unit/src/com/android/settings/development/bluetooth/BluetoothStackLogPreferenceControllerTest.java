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

import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BTSTACK_LOG_MODE_VERBOSE_INDEX;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BTSTACK_LOG_MODE_DEBUG_INDEX;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BTSTACK_LOG_MODE_INFO_INDEX;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BTSTACK_LOG_MODE_WARN_INDEX;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BTSTACK_LOG_MODE_ERROR_INDEX;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BluetoothStackLogPreferenceControllerTest {
    private static final String TAG = "BluetoothStackLogPreferenceControllerTest";

    @Mock private Context mContext;

    private ListPreference mPreference;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    private BluetoothStackLogPreferenceController mController;

    private CharSequence[] mListValues;
    private CharSequence[] mListEntries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new ListPreference(mContext);

        mController = new BluetoothStackLogPreferenceController(mContext);

        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setEntries(com.android.settings.R.array.bt_stack_log_level_entries);
        mPreference.setEntryValues(com.android.settings.R.array.bt_stack_log_level_values);

        mPreferenceScreen.addPreference(mPreference);
        mController.displayPreference(mPreferenceScreen);

        mListValues = mPreference.getEntryValues();
        mListEntries = mPreference.getEntries();
    }

    /**
     * Test that default log level is set to INFO
     */
    @Test
    public void verifyDefaultState_enablesDefaultLogLevelEntriesAndValuesSameSize() {
        mController.onPreferenceChange(mPreference, mController.getDefaultModeIndex());
        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues
                        [BTSTACK_LOG_MODE_INFO_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_INFO_INDEX].toString());
    }

    /**
     * Test that log level is changed to VERBOSE when VERBOSE is selected
     */
    @Test
    public void onPreferenceChanged_enableBluetoothStackVerboseLogLevel() {
        mController.onPreferenceChange(mPreference, mListValues[BTSTACK_LOG_MODE_VERBOSE_INDEX]
                        .toString());

        final String persistedLogLevel = SystemProperties.get(
                        BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST);
        final String logLevel = SystemProperties.get(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY);
        assertThat(persistedLogLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_VERBOSE_INDEX]
                        .toString());
        assertThat(logLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_VERBOSE_INDEX].toString());

        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues
                        [BTSTACK_LOG_MODE_VERBOSE_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_VERBOSE_INDEX].toString());
    }

    /**
     * Test that log level is changed to DEBUG when DEBUG is selected
     */
    @Test
    public void onPreferenceChanged_enableBluetoothStackDebugLogLevel() {
        mController.onPreferenceChange(mPreference, mListValues[BTSTACK_LOG_MODE_DEBUG_INDEX]
                        .toString());

        final String persistedLogLevel = SystemProperties.get(
                BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST);
        final String logLevel = SystemProperties.get(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY);
        assertThat(persistedLogLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_DEBUG_INDEX]
                        .toString());
        assertThat(logLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_DEBUG_INDEX].toString());

        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues
                        [BTSTACK_LOG_MODE_DEBUG_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_DEBUG_INDEX].toString());
    }

    /**
     * Test that log level is changed to INFO when INFO is selected
     */
    @Test
    public void onPreferenceChanged_enableBluetoothStackInfoLogLevel() {
        mController.onPreferenceChange(mPreference, mListValues[BTSTACK_LOG_MODE_INFO_INDEX]
                        .toString());

        final String persistedLogLevel = SystemProperties.get(
                BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST);
        final String logLevel = SystemProperties.get(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY);
        assertThat(persistedLogLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_INFO_INDEX]
                        .toString());
        assertThat(logLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_INFO_INDEX].toString());

        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues
                        [BTSTACK_LOG_MODE_INFO_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_INFO_INDEX].toString());
    }

    /**
     * Test that log level is changed to WARN when WARN is selected
     */
    @Test
    public void onPreferenceChanged_enableBluetoothStackWarnLogLevel() {
        mController.onPreferenceChange(mPreference, mListValues[BTSTACK_LOG_MODE_WARN_INDEX]
                        .toString());

        final String persistedLogLevel = SystemProperties.get(
                BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST);
        final String logLevel = SystemProperties.get(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY);
        assertThat(persistedLogLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_WARN_INDEX]
                        .toString());
        assertThat(logLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_WARN_INDEX].toString());

        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues

                        [BTSTACK_LOG_MODE_WARN_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_WARN_INDEX].toString());
    }

    /**
     * Test that log level is changed to ERROR when ERROR is selected
     */
    @Test
    public void onPreferenceChanged_enableBluetoothStackErrorLogLevel() {
        mController.onPreferenceChange(mPreference, mListValues[BTSTACK_LOG_MODE_ERROR_INDEX]
                        .toString());

        final String persistedLogLevel = SystemProperties.get(
                BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY_PERSIST);
        final String logLevel = SystemProperties.get(BLUETOOTH_BTSTACK_LOG_MODE_PROPERTY);
        assertThat(persistedLogLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_ERROR_INDEX]
                        .toString());
        assertThat(logLevel).isEqualTo(mListValues[BTSTACK_LOG_MODE_ERROR_INDEX].toString());

        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues
                        [BTSTACK_LOG_MODE_ERROR_INDEX].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries
                        [BTSTACK_LOG_MODE_ERROR_INDEX].toString());
    }

    /**
     * Test that preference is disabled when developer options is disabled
     * Log level is also reset to default
     */
    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getValue().toString()).isEqualTo(mListValues[mController
                .getDefaultModeIndex()].toString());
        assertThat(mPreference.getSummary().toString()).isEqualTo(mListEntries[mController
                .getDefaultModeIndex()].toString());
    }
}
