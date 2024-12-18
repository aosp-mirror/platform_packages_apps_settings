/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.bluetooth.BluetoothCsipSetCoordinator.GROUP_ID_INVALID;
import static android.bluetooth.BluetoothHapClient.PRESET_INDEX_UNAVAILABLE;

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingAidsPresetsController.KEY_HEARING_AIDS_PRESETS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHapPresetInfo;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/** Tests for {@link BluetoothDetailsHearingAidsPresetsController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsHearingAidsPresetsControllerTest extends
        BluetoothDetailsControllerTestBase {

    private static final int TEST_PRESET_INDEX = 1;
    private static final String TEST_PRESET_NAME = "test_preset";
    private static final int TEST_HAP_GROUP_ID = 1;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private HapClientProfile mHapClientProfile;
    @Mock
    private CachedBluetoothDevice mCachedChildDevice;
    @Mock
    private BluetoothDevice mChildDevice;

    private BluetoothDetailsHearingAidsPresetsController mController;

    @Override
    public void setUp() {
        super.setUp();

        when(mLocalManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getHapClientProfile()).thenReturn(mHapClientProfile);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mHapClientProfile));
        when(mCachedDevice.isConnectedHapClientDevice()).thenReturn(true);
        when(mCachedChildDevice.getDevice()).thenReturn(mChildDevice);
        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = new BluetoothDetailsHearingAidsPresetsController(mContext, mFragment,
                mLocalManager, mCachedDevice, mLifecycle);
        mController.init(mScreen);
    }

    @Test
    public void isAvailable_supportHap_returnTrue() {
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mHapClientProfile));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notSupportHap_returnFalse() {
        when(mCachedDevice.getProfiles()).thenReturn(new ArrayList<>());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onResume_registerCallback() {
        mController.onResume();

        verify(mHapClientProfile).registerCallback(any(Executor.class),
                any(BluetoothHapClient.Callback.class));
    }

    @Test
    public void onPause_unregisterCallback() {
        mController.onPause();

        verify(mHapClientProfile).unregisterCallback(any(BluetoothHapClient.Callback.class));
    }

    @Test
    public void onPreferenceChange_keyMatched_verifyStatusUpdated() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);

        boolean handled = mController.onPreferenceChange(presetPreference,
                String.valueOf(TEST_PRESET_INDEX));

        assertThat(handled).isTrue();
        verify(presetPreference).setSummary(TEST_PRESET_NAME);
    }

    @Test
    public void onPreferenceChange_keyNotMatched_doNothing() {
        final ListPreference presetPreference = getTestPresetPreference("wrong_key");

        boolean handled = mController.onPreferenceChange(
                presetPreference, String.valueOf(TEST_PRESET_INDEX));

        assertThat(handled).isFalse();
        verify(presetPreference, never()).setSummary(any());
    }

    @Test
    public void onPreferenceChange_supportGroupOperation_validGroupId_verifySelectPresetForGroup() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);
        when(mHapClientProfile.supportsSynchronizedPresets(mDevice)).thenReturn(true);
        when(mHapClientProfile.getHapGroup(mDevice)).thenReturn(TEST_HAP_GROUP_ID);

        mController.onPreferenceChange(presetPreference, String.valueOf(TEST_PRESET_INDEX));

        verify(mHapClientProfile).selectPresetForGroup(TEST_HAP_GROUP_ID, TEST_PRESET_INDEX);
    }

    @Test
    public void onPreferenceChange_notSupportGroupOperation_verifySelectPreset() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);
        when(mHapClientProfile.supportsSynchronizedPresets(mDevice)).thenReturn(false);
        when(mHapClientProfile.getHapGroup(mDevice)).thenReturn(TEST_HAP_GROUP_ID);

        mController.onPreferenceChange(presetPreference, String.valueOf(TEST_PRESET_INDEX));

        verify(mHapClientProfile).selectPreset(mDevice, TEST_PRESET_INDEX);
    }

    @Test
    public void onPreferenceChange_invalidGroupId_verifySelectPreset() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);
        when(mHapClientProfile.supportsSynchronizedPresets(mDevice)).thenReturn(true);
        when(mHapClientProfile.getHapGroup(mDevice)).thenReturn(GROUP_ID_INVALID);

        mController.onPreferenceChange(presetPreference, String.valueOf(TEST_PRESET_INDEX));

        verify(mHapClientProfile).selectPreset(mDevice, TEST_PRESET_INDEX);
    }

    @Test
    public void onPreferenceChange_notSupportGroupOperation_hasSubDevice_verifyStatusUpdated() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);
        when(mHapClientProfile.supportsSynchronizedPresets(mDevice)).thenReturn(false);
        when(mCachedDevice.getSubDevice()).thenReturn(mCachedChildDevice);

        mController.onPreferenceChange(presetPreference, String.valueOf(TEST_PRESET_INDEX));

        verify(mHapClientProfile).selectPreset(mDevice, TEST_PRESET_INDEX);
        verify(mHapClientProfile).selectPreset(mChildDevice, TEST_PRESET_INDEX);
    }

    @Test
    public void onPreferenceChange_notSupportGroupOperation_hasMemberDevice_verifyStatusUpdated() {
        final ListPreference presetPreference = getTestPresetPreference(KEY_HEARING_AIDS_PRESETS);
        when(mHapClientProfile.supportsSynchronizedPresets(mDevice)).thenReturn(false);
        when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mCachedChildDevice));

        mController.onPreferenceChange(presetPreference, String.valueOf(TEST_PRESET_INDEX));

        verify(mHapClientProfile).selectPreset(mDevice, TEST_PRESET_INDEX);
        verify(mHapClientProfile).selectPreset(mChildDevice, TEST_PRESET_INDEX);
    }

    @Test
    public void refresh_emptyPresetInfo_preferenceDisabled() {
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(new ArrayList<>());

        mController.refresh();

        assertThat(mController.getPreference()).isNotNull();
        assertThat(mController.getPreference().isEnabled()).isFalse();
        assertThat(String.valueOf(mController.getPreference().getSummary())).isEqualTo(
                mContext.getString(R.string.bluetooth_hearing_aids_presets_empty_list_message));
    }

    @Test
    public void refresh_validPresetInfo_preferenceEnabled() {
        BluetoothHapPresetInfo info = getTestPresetInfo(true);
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(List.of(info));

        mController.refresh();

        assertThat(mController.getPreference()).isNotNull();
        assertThat(mController.getPreference().isEnabled()).isTrue();
    }

    @Test
    public void refresh_invalidActivePresetIndex_summaryIsNull() {
        BluetoothHapPresetInfo info = getTestPresetInfo(true);
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(List.of(info));
        when(mHapClientProfile.getActivePresetIndex(mDevice)).thenReturn(PRESET_INDEX_UNAVAILABLE);

        mController.refresh();

        assertThat(mController.getPreference()).isNotNull();
        assertThat(mController.getPreference().getSummary()).isNull();
    }

    @Test
    public void refresh_validActivePresetIndex_summaryIsNotNull() {
        BluetoothHapPresetInfo info = getTestPresetInfo(true);
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(List.of(info));
        when(mHapClientProfile.getActivePresetIndex(mDevice)).thenReturn(TEST_PRESET_INDEX);

        mController.refresh();

        assertThat(mController.getPreference()).isNotNull();
        assertThat(mController.getPreference().getSummary()).isNotNull();
    }

    @Test
    public void onPresetSelectionForGroupFailed_selectPresetIsCalled() {
        when(mHapClientProfile.getHapGroup(mDevice)).thenReturn(TEST_HAP_GROUP_ID);
        mController.getPreference().setValue(String.valueOf(TEST_PRESET_INDEX));

        mController.onPresetSelectionForGroupFailed(TEST_HAP_GROUP_ID, TEST_PRESET_INDEX);

        verify(mHapClientProfile).selectPreset(mDevice, TEST_PRESET_INDEX);
    }

    @Test
    public void loadAllPresetInfo_unavailablePreset_notAddedToEntries() {
        BluetoothHapPresetInfo info = getTestPresetInfo(false);
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(List.of(info));

        mController.refresh();

        assertThat(mController.getPreference().getEntries().length).isEqualTo(0);
    }

    @Test
    public void loadAllPresetInfo_availablePreset_addedToEntries() {
        BluetoothHapPresetInfo info = getTestPresetInfo(true);
        when(mHapClientProfile.getAllPresetInfo(mDevice)).thenReturn(List.of(info));

        mController.refresh();

        assertThat(mController.getPreference().getEntries().length).isEqualTo(1);
    }
    private BluetoothHapPresetInfo getTestPresetInfo(boolean available) {
        BluetoothHapPresetInfo info = mock(BluetoothHapPresetInfo.class);
        when(info.getName()).thenReturn(TEST_PRESET_NAME);
        when(info.getIndex()).thenReturn(TEST_PRESET_INDEX);
        when(info.isAvailable()).thenReturn(available);
        return info;
    }

    private ListPreference getTestPresetPreference(String key) {
        final ListPreference presetPreference = spy(new ListPreference(mContext));
        when(presetPreference.findIndexOfValue(String.valueOf(TEST_PRESET_INDEX))).thenReturn(0);
        when(presetPreference.getEntries()).thenReturn(new CharSequence[]{TEST_PRESET_NAME});
        when(presetPreference.getEntryValues()).thenReturn(
                new CharSequence[]{String.valueOf(TEST_PRESET_INDEX)});
        presetPreference.setKey(key);
        return presetPreference;
    }
}
