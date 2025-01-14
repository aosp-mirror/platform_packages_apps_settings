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

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.AmbientVolumeUiController;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link BluetoothDetailsAmbientVolumePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowThreadUtils.class
})
public class BluetoothDetailsAmbientVolumePreferenceControllerTest extends
        BluetoothDetailsControllerTestBase {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private BluetoothEventManager mEventManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private VolumeControlProfile mVolumeControlProfile;
    @Mock
    private AmbientVolumeUiController mUiController;

    private BluetoothDetailsAmbientVolumePreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();

        mContext = spy(mContext);

        when(mBluetoothManager.getProfileManager()).thenReturn(mProfileManager);
        when(mBluetoothManager.getEventManager()).thenReturn(mEventManager);
        mController = spy(
                new BluetoothDetailsAmbientVolumePreferenceController(mContext, mBluetoothManager,
                        mFragment, mCachedDevice, mLifecycle, mUiController));

        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
    }

    @Test
    public void init_preferenceAdded() {
        mController.init(mScreen);

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
    }

    @Test
    public void isAvailable_notHearingDevice_returnFalse() {
        when(mCachedDevice.isHearingDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isHearingDeviceAndNotSupportVcp_returnFalse() {
        when(mCachedDevice.isHearingDevice()).thenReturn(true);
        when(mCachedDevice.getProfiles()).thenReturn(List.of());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isHearingDeviceAndSupportVcp_returnTrue() {
        when(mCachedDevice.isHearingDevice()).thenReturn(true);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mVolumeControlProfile));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void refresh_isHearingDeviceAndNotSupportVcp_verifyUiControllerNoRefresh() {
        when(mCachedDevice.isHearingDevice()).thenReturn(true);
        when(mCachedDevice.getProfiles()).thenReturn(List.of());

        mController.refresh();

        verify(mUiController, never()).refresh();
    }

    @Test
    public void refresh_isHearingDeviceAndSupportVcp_verifyUiControllerRefresh() {
        when(mCachedDevice.isHearingDevice()).thenReturn(true);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mVolumeControlProfile));

        mController.refresh();

        verify(mUiController).refresh();
    }

    @Test
    public void onStart_verifyUiControllerStart() {
        mController.onStart();

        verify(mUiController).start();
    }

    @Test
    public void onStop_verifyUiControllerStop() {
        mController.onStop();

        verify(mUiController).stop();
    }
}
