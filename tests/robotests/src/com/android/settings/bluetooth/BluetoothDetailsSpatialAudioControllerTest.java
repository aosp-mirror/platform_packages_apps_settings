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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsSpatialAudioControllerTest extends BluetoothDetailsControllerTestBase {

    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String KEY_SPATIAL_AUDIO = "spatial_audio";
    private static final String KEY_HEAD_TRACKING = "head_tracking";

    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Spatializer mSpatializer;
    @Mock
    private Lifecycle mSpatialAudioLifecycle;
    @Mock
    private PreferenceCategory mProfilesContainer;
    @Mock
    private BluetoothDevice mBluetoothDevice;

    private BluetoothDetailsSpatialAudioController mController;
    private SwitchPreference mSpatialAudioPref;
    private SwitchPreference mHeadTrackingPref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mAudioManager.getSpatializer()).thenReturn(mSpatializer);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(MAC_ADDRESS);

        mController = new BluetoothDetailsSpatialAudioController(mContext, mFragment,
                mCachedDevice, mSpatialAudioLifecycle);
        mController.mProfilesContainer = mProfilesContainer;

        mSpatialAudioPref = mController.createSpatialAudioPreference(mContext);
        mHeadTrackingPref = mController.createHeadTrackingPreference(mContext);

        when(mProfilesContainer.findPreference(KEY_SPATIAL_AUDIO)).thenReturn(mSpatialAudioPref);
        when(mProfilesContainer.findPreference(KEY_HEAD_TRACKING)).thenReturn(mHeadTrackingPref);
    }

    @Test
    public void isAvailable_spatialAudioSupportA2dpDevice_returnsTrue() {
        AudioDeviceAttributes a2dpDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                MAC_ADDRESS);
        when(mSpatializer.isAvailableForDevice(a2dpDevice)).thenReturn(true);

        mController.setAvailableDevice(a2dpDevice);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
    }

    @Test
    public void isAvailable_spatialAudioSupportBleHeadsetDevice_returnsTrue() {
        AudioDeviceAttributes bleHeadsetDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                MAC_ADDRESS);
        when(mSpatializer.isAvailableForDevice(bleHeadsetDevice)).thenReturn(true);

        mController.setAvailableDevice(bleHeadsetDevice);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_BLE_HEADSET);
    }

    @Test
    public void isAvailable_spatialAudioSupportBleSpeakerDevice_returnsTrue() {
        AudioDeviceAttributes bleSpeakerDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                MAC_ADDRESS);
        when(mSpatializer.isAvailableForDevice(bleSpeakerDevice)).thenReturn(true);

        mController.setAvailableDevice(bleSpeakerDevice);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_BLE_SPEAKER);
    }

    @Test
    public void isAvailable_spatialAudioSupportBleBroadcastDevice_returnsTrue() {
        AudioDeviceAttributes bleBroadcastDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                MAC_ADDRESS);
        when(mSpatializer.isAvailableForDevice(bleBroadcastDevice)).thenReturn(true);

        mController.setAvailableDevice(bleBroadcastDevice);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_BLE_BROADCAST);
    }

    @Test
    public void isAvailable_spatialAudioSupportHearingAidDevice_returnsTrue() {
        AudioDeviceAttributes hearingAidDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                MAC_ADDRESS);
        when(mSpatializer.isAvailableForDevice(hearingAidDevice)).thenReturn(true);

        mController.setAvailableDevice(hearingAidDevice);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_HEARING_AID);
    }

    @Test
    public void isAvailable_spatialAudioNotSupported_returnsFalse() {
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.mAudioDevice.getType())
                .isEqualTo(AudioDeviceInfo.TYPE_HEARING_AID);
    }

    @Test
    public void refresh_spatialAudioIsTurnedOn_checksSpatialAudioPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();

        assertThat(mSpatialAudioPref.isChecked()).isTrue();
    }

    @Test
    public void refresh_spatialAudioIsTurnedOff_unchecksSpatialAudioPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();

        assertThat(mSpatialAudioPref.isChecked()).isFalse();
    }

    @Test
    public void refresh_spatialAudioOnAndHeadTrackingIsAvailable_showsHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);

        mController.refresh();

        assertThat(mHeadTrackingPref.isVisible()).isTrue();
    }

    @Test
    public void
            refresh_spatialAudioOnAndHeadTrackingIsNotAvailable_hidesHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();

        assertThat(mHeadTrackingPref.isVisible()).isFalse();
    }

    @Test
    public void refresh_spatialAudioOff_hidesHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();

        assertThat(mHeadTrackingPref.isVisible()).isFalse();
    }

    @Test
    public void refresh_headTrackingIsTurnedOn_checksHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(true);

        mController.refresh();

        assertThat(mHeadTrackingPref.isChecked()).isTrue();
    }

    @Test
    public void refresh_headTrackingIsTurnedOff_unchecksHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();

        assertThat(mHeadTrackingPref.isChecked()).isFalse();
    }

    @Test
    public void turnedOnSpatialAudio_invokesAddCompatibleAudioDevice() {
        mSpatialAudioPref.setChecked(true);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).addCompatibleAudioDevice(mController.mAudioDevice);
    }

    @Test
    public void turnedOffSpatialAudio_invokesRemoveCompatibleAudioDevice() {
        mSpatialAudioPref.setChecked(false);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).removeCompatibleAudioDevice(mController.mAudioDevice);
    }

    @Test
    public void turnedOnHeadTracking_invokesSetHeadTrackerEnabled_setsTrue() {
        mHeadTrackingPref.setChecked(true);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(true, mController.mAudioDevice);
    }

    @Test
    public void turnedOffHeadTracking_invokesSetHeadTrackerEnabled_setsFalse() {
        mHeadTrackingPref.setChecked(false);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(false, mController.mAudioDevice);
    }
}
