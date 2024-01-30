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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.sysprop.BluetoothProperties;

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

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class BluetoothSnoopLogFilterProfilePbapPreferenceControllerTest {

    @Spy private Context mSpyContext = RuntimeEnvironment.application;
    @Spy private Resources mSpyResources = RuntimeEnvironment.application.getResources();
    private ListPreference mPreference;
    @Mock private PreferenceScreen mPreferenceScreen;
    private BluetoothSnoopLogFilterProfilePbapPreferenceController mController;

    private CharSequence[] mListValues;
    private CharSequence[] mListEntries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(mSpyResources).when(mSpyContext).getResources();
        // Get XML values without mock
        // Setup test list preference using XML values
        mPreference = new ListPreference(mSpyContext);
        mPreference.setEntries(
                com.android.settingslib.R.array.bt_hci_snoop_log_profile_filter_entries);
        mPreference.setEntryValues(
                com.android.settingslib.R.array.bt_hci_snoop_log_profile_filter_values);
        // Init the actual controller
        mController = new BluetoothSnoopLogFilterProfilePbapPreferenceController(mSpyContext);
        // Construct preference in the controller via a mocked preference screen object
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mListValues = mPreference.getEntryValues();
        mListEntries = mPreference.getEntries();
        BluetoothProperties.snoop_log_mode(BluetoothProperties.snoop_log_mode_values.FILTERED);
    }

    @Test
    public void verifyResourceSizeAndRange() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
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
    public void onPreferenceChanged_turnOnBluetoothSnoopLogFullFilterPbap() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfilePbapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_FULL_FILTER_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_pbap()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_pbap_values.DISABLED);
        // "fullfilter" is hard-coded between Settings and system/bt
        Assert.assertEquals(
                mode, BluetoothProperties.snoop_log_filter_profile_pbap_values.FULLFILTER);
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogHeaderFilterPbap() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfilePbapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_HEADER_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_pbap()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_pbap_values.DISABLED);
        // "header" is hard-coded between Settings and system/bt
        Assert.assertEquals(mode, BluetoothProperties.snoop_log_filter_profile_pbap_values.HEADER);
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogMagicFilterPbap() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfilePbapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_MAGIC_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_pbap()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_pbap_values.DISABLED);
        // "magic" is hard-coded between Settings and system/bt
        Assert.assertEquals(mode, BluetoothProperties.snoop_log_filter_profile_pbap_values.MAGIC);
    }

    @Test
    public void onPreferenceChanged_turnOffBluetoothSnoopLogFilterPbap() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfilePbapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_DISABLED_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_pbap()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_pbap_values.DISABLED);
        // "disabled" is hard-coded between Settings and system/bt
        Assert.assertEquals(
                mode, BluetoothProperties.snoop_log_filter_profile_pbap_values.DISABLED);
    }

    @Test
    public void updateState_preferenceShouldBeSetToRightValue() {
        Assert.assertTrue(
                BluetoothSnoopLogFilterProfilePbapPreferenceController
                        .isSnoopLogModeFilteredEnabled());
        for (int i = 0; i < mListValues.length; ++i) {
            BluetoothProperties.snoop_log_filter_profile_pbap(
                    BluetoothProperties.snoop_log_filter_profile_pbap_values.valueOf(
                            mListValues[i].toString().toUpperCase(Locale.US)));
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
