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

import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BT_LOG_LEVEL_DEFAULT_INDEX;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BT_LOG_LEVEL_PROP;
import static com.android.settings.development.bluetooth.BluetoothStackLogPreferenceController.BT_LOG_LEVEL_PROP_PERSIST;

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

@RunWith(AndroidJUnit4.class)
public class BluetoothStackLogPreferenceControllerTest {
    private static final String COM_ANDROID_SETTINGS = "com.android.settings";
    private static final String TYPE_ARRAY = "array";

    private static final String XML_DEFINED_PREFERENCE_KEY = "bt_stack_log_level";
    private static final String XML_DEFINED_ENTRIES_RESOURCE = "bt_stack_log_level_entries";
    private static final String XML_DEFINED_VALUES_RESOURCE = "bt_stack_log_level_values";

    private static final String PROPERTY_CLEARED = "";

    private Context mContext;

    private ListPreference mPreference;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    private BluetoothStackLogPreferenceController mController;

    private CharSequence[] mListValues;
    private CharSequence[] mListEntries;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new ListPreference(mContext);
        mController = new BluetoothStackLogPreferenceController(mContext);

        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setEntries(getStringArrayResourceId(XML_DEFINED_ENTRIES_RESOURCE));
        mPreference.setEntryValues(getStringArrayResourceId(XML_DEFINED_VALUES_RESOURCE));

        mPreferenceScreen.addPreference(mPreference);
        mController.displayPreference(mPreferenceScreen);

        mListValues = mPreference.getEntryValues();
        mListEntries = mPreference.getEntries();
    }

    /**
     * Get the resource ID associated with a resource name
     *
     * This looks up the resource id by name using our device's context. This way, we can avoid
     * hardcoding a resource ID or value from the R class which may not match the resource IDs on
     * the device under test.
     *
     * Usage: int valuesResId = getStringArrayResource("bt_stack_log_level_values");
     * Usage: int entriesResId = getStringArrayResource("bt_stack_log_level_entries");
     *
     * @param res - The resource name to look up
     * @return The integer resource ID corresponding to the given resource name
     */
    public int getStringArrayResourceId(String res) {
        return mContext.getResources().getIdentifier(res, TYPE_ARRAY, COM_ANDROID_SETTINGS);
    }

    /**
     * Test that, for each possible value a user can select, our controller properly handles the
     * value to update the underlying system property _and_ set the UI entry to the proper value.
     */
    @Test
    public void onPreferenceChange_withEachValue_uiSetProperlyAndAllValuesWrittenToProperties() {
        for (int index = 0; index < mListValues.length; index++) {
            String value = mListValues[index].toString();
            String entry = mListEntries[index].toString();

            mController.onPreferenceChange(mPreference, value);

            final String persistedLogLevel = SystemProperties.get(BT_LOG_LEVEL_PROP_PERSIST);
            final String logLevel = SystemProperties.get(BT_LOG_LEVEL_PROP);
            final String currentValue = mPreference.getValue().toString();
            final String currentEntry = mPreference.getEntry().toString();
            final String currentSummary = mPreference.getSummary().toString();
            final int currentIndex = mPreference.findIndexOfValue(currentValue);

            assertThat(persistedLogLevel).isEqualTo(value);
            assertThat(logLevel).isEqualTo(value);
            assertThat(currentIndex).isEqualTo(index);
            assertThat(currentValue).isEqualTo(value);
            assertThat(currentEntry).isEqualTo(entry);
            assertThat(currentSummary).isEqualTo(entry);
        }
    }

    /**
     * Test that, for each possible log tag log level value, our controller properly handles the
     * value to set the UI entry to the proper value.
     */
    @Test
    public void updateState_withEachValue_uiSetProperly() {
        for (int index = 0; index < mListValues.length; index++) {
            String value = mListValues[index].toString();
            String entry = mListEntries[index].toString();

            SystemProperties.set(BT_LOG_LEVEL_PROP_PERSIST, value);
            SystemProperties.set(BT_LOG_LEVEL_PROP, value);

            mController.updateState(mPreference);

            final String currentValue = mPreference.getValue().toString();
            final String currentEntry = mPreference.getEntry().toString();
            final String currentSummary = mPreference.getSummary().toString();
            final int currentIndex = mPreference.findIndexOfValue(currentValue);

            assertThat(currentIndex).isEqualTo(index);
            assertThat(currentValue).isEqualTo(value);
            assertThat(currentEntry).isEqualTo(entry);
            assertThat(currentSummary).isEqualTo(entry);
        }
    }

    /**
     * Test that our controller reverts the log level back to a missing/default value when we're
     * notified that Developer Options has been disabled.
     */
    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceSetToDefault() {
        mController.onDeveloperOptionsSwitchDisabled();

        final String defaultEntry = mListEntries[BT_LOG_LEVEL_DEFAULT_INDEX].toString();
        final String defaultValue = mListValues[BT_LOG_LEVEL_DEFAULT_INDEX].toString();

        final String persistedLogLevel = SystemProperties.get(BT_LOG_LEVEL_PROP_PERSIST);
        final String logLevel = SystemProperties.get(BT_LOG_LEVEL_PROP);
        final String currentValue = mPreference.getValue().toString();
        final String currentEntry = mPreference.getEntry().toString();
        final String currentSummary = mPreference.getSummary().toString();
        final int currentIndex = mPreference.findIndexOfValue(currentValue);

        assertThat(persistedLogLevel).isEqualTo(PROPERTY_CLEARED);
        assertThat(logLevel).isEqualTo(PROPERTY_CLEARED);
        assertThat(currentIndex).isEqualTo(BT_LOG_LEVEL_DEFAULT_INDEX);
        assertThat(currentValue).isEqualTo(defaultValue);
        assertThat(currentEntry).isEqualTo(defaultEntry);
        assertThat(currentSummary).isEqualTo(defaultEntry);
    }

    /**
     * Test that our preference key returned by our controller matches the one defined in the XML
     * definition.
     */
    @Test
    public void getPreferenceKey_matchesXmlDefinedPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(XML_DEFINED_PREFERENCE_KEY);
    }
}
