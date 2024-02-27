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

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingAidsPresetsController.KEY_HEARING_AIDS_PRESETS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothHapClient;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;

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

import java.util.List;
import java.util.concurrent.Executor;

/** Tests for {@link BluetoothDetailsHearingAidsPresetsController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsHearingAidsPresetsControllerTest extends
        BluetoothDetailsControllerTestBase {

    private static final int TEST_PRESET_INDEX = 1;
    private static final String TEST_PRESET_NAME = "test_preset";

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private HapClientProfile mHapClientProfile;

    private BluetoothDetailsHearingAidsPresetsController mController;

    @Override
    public void setUp() {
        super.setUp();

        when(mLocalManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getHapClientProfile()).thenReturn(mHapClientProfile);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mHapClientProfile));
        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = new BluetoothDetailsHearingAidsPresetsController(mContext, mFragment,
                mLocalManager, mCachedDevice, mLifecycle);
        mController.init(mScreen);
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
    }

    @Test
    public void onPreferenceChange_keyNotMatched_doNothing() {
        final ListPreference presetPreference = getTestPresetPreference("wrong_key");

        boolean handled = mController.onPreferenceChange(
                presetPreference, String.valueOf(TEST_PRESET_INDEX));

        assertThat(handled).isFalse();
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
