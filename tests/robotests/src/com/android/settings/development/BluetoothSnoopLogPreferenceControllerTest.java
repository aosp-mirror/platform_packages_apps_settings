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

import static com.android.settings.development.BluetoothSnoopLogPreferenceController.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothSnoopLogPreferenceControllerTest {

    @Spy private Context mSpyContext = RuntimeEnvironment.application;
    @Spy private Resources mSpyResources = RuntimeEnvironment.application.getResources();
    private ListPreference mPreference;
    @Mock private PreferenceScreen mPreferenceScreen;
    private BluetoothSnoopLogPreferenceController mController;

    private CharSequence[] mListValues;
    private CharSequence[] mListEntries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(mSpyResources).when(mSpyContext).getResources();
        // Get XML values without mock
        // Setup test list preference using XML values
        mPreference = new ListPreference(mSpyContext);
        mPreference.setEntries(com.android.settingslib.R.array.bt_hci_snoop_log_entries);
        mPreference.setEntryValues(com.android.settingslib.R.array.bt_hci_snoop_log_values);
        // Init the actual controller
        mController = new BluetoothSnoopLogPreferenceController(mSpyContext, null);
        // Construct preference in the controller via a mocked preference screen object
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mListValues = mPreference.getEntryValues();
        mListEntries = mPreference.getEntries();
    }

    @Test
    public void verifyResourceSizeAndRange() {
        // Verify normal list entries and default preference entries have the same size
        Assert.assertEquals(mListEntries.length, mListValues.length);
        // Update the preference
        mController.updateState(mPreference);
        // Verify default preference value, entry and summary
        final int defaultIndex = mController.getDefaultModeIndex();
        Assert.assertEquals(mPreference.getValue(), mListValues[defaultIndex]);
        Assert.assertEquals(mPreference.getEntry(), mListEntries[defaultIndex]);
        Assert.assertEquals(mPreference.getSummary(), mListEntries[defaultIndex]);
    }

    @Test
    public void onPreferenceChanged_turnOnFullBluetoothSnoopLog() {
        mController.onPreferenceChange(
                null,
                mListValues[BluetoothSnoopLogPreferenceController.BTSNOOP_LOG_MODE_FULL_INDEX]);
        final String mode = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        // "full" is hard-coded between Settings and system/bt
        Assert.assertEquals(mode, "full");
    }

    @Test
    public void onPreferenceChanged_turnOnFilteredBluetoothSnoopLog() {
        mController.onPreferenceChange(
                null,
                mListValues[BluetoothSnoopLogPreferenceController.BTSNOOP_LOG_MODE_FILTERED_INDEX]);
        final String mode = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        // "filtered" is hard-coded between Settings and system/bt
        Assert.assertEquals(mode, "filtered");
    }

    @Test
    public void onPreferenceChanged_turnOffBluetoothSnoopLog() {
        mController.onPreferenceChange(
                null,
                mListValues[BluetoothSnoopLogPreferenceController.BTSNOOP_LOG_MODE_DISABLED_INDEX]);
        final String mode = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        // "disabled" is hard-coded between Settings and system/bt
        Assert.assertEquals(mode, "disabled");
    }

    @Test
    public void updateState_preferenceShouldBeSetToRightValue() {
        for (int i = 0; i < mListValues.length; ++i) {
            SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, mListValues[i].toString());
            mController.updateState(mPreference);
            Assert.assertEquals(mPreference.getValue(), mListValues[i].toString());
            Assert.assertEquals(mPreference.getSummary(), mListEntries[i].toString());
        }
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        Assert.assertFalse(mPreference.isEnabled());
        Assert.assertEquals(
                mPreference.getValue(), mListValues[mController.getDefaultModeIndex()].toString());
        Assert.assertEquals(
                mPreference.getSummary(),
                mListEntries[mController.getDefaultModeIndex()].toString());
    }
}
