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

import static android.media.Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL;
import static android.media.Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE;

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
import androidx.preference.TwoStatePreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

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

    private AudioDeviceAttributes mAvailableDevice;

    private BluetoothDetailsSpatialAudioController mController;
    private TwoStatePreference mSpatialAudioPref;
    private TwoStatePreference mHeadTrackingPref;

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

        mAvailableDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                MAC_ADDRESS);
    }

    @Test
    public void isAvailable_forSpatializerWithLevelNone_returnsFalse() {
        when(mSpatializer.getImmersiveAudioLevel()).thenReturn(SPATIALIZER_IMMERSIVE_LEVEL_NONE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_forSpatializerWithLevelNotNone_returnsTrue() {
        when(mSpatializer.getImmersiveAudioLevel()).thenReturn(
                SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void refresh_spatialAudioIsTurnedOn_checksSpatialAudioPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        mController.setAvailableDevice(mAvailableDevice);
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.isAvailableForDevice(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mSpatialAudioPref.isChecked()).isTrue();
    }

    @Test
    public void refresh_spatialAudioIsTurnedOff_unchecksSpatialAudioPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mSpatialAudioPref.isChecked()).isFalse();
    }

    @Test
    public void refresh_spatialAudioOnAndHeadTrackingIsAvailable_showsHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mHeadTrackingPref.isVisible()).isTrue();
    }

    @Test
    public void
            refresh_spatialAudioOnAndHeadTrackingIsNotAvailable_hidesHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        mController.setAvailableDevice(mAvailableDevice);
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        verify(mProfilesContainer).removePreference(mHeadTrackingPref);
    }

    @Test
    public void refresh_spatialAudioOff_hidesHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        verify(mProfilesContainer).removePreference(mHeadTrackingPref);
    }

    @Test
    public void refresh_headTrackingIsTurnedOn_checksHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        mController.setAvailableDevice(mAvailableDevice);
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.isAvailableForDevice(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mHeadTrackingPref.isChecked()).isTrue();
    }

    @Test
    public void refresh_headTrackingIsTurnedOff_unchecksHeadTrackingPreference() {
        List<AudioDeviceAttributes> compatibleAudioDevices = new ArrayList<>();
        mController.setAvailableDevice(mAvailableDevice);
        compatibleAudioDevices.add(mController.mAudioDevice);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(compatibleAudioDevices);
        when(mSpatializer.isAvailableForDevice(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.hasHeadTracker(mController.mAudioDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mHeadTrackingPref.isChecked()).isFalse();
    }

    @Test
    public void turnedOnSpatialAudio_invokesAddCompatibleAudioDevice() {
        mController.setAvailableDevice(mAvailableDevice);
        mSpatialAudioPref.setChecked(true);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).addCompatibleAudioDevice(mController.mAudioDevice);
    }

    @Test
    public void turnedOffSpatialAudio_invokesRemoveCompatibleAudioDevice() {
        mController.setAvailableDevice(mAvailableDevice);
        mSpatialAudioPref.setChecked(false);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).removeCompatibleAudioDevice(mController.mAudioDevice);
    }

    @Test
    public void turnedOnHeadTracking_invokesSetHeadTrackerEnabled_setsTrue() {
        mController.setAvailableDevice(mAvailableDevice);
        mHeadTrackingPref.setChecked(true);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(true, mController.mAudioDevice);
    }

    @Test
    public void turnedOffHeadTracking_invokesSetHeadTrackerEnabled_setsFalse() {
        mController.setAvailableDevice(mAvailableDevice);
        mHeadTrackingPref.setChecked(false);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(false, mController.mAudioDevice);
    }
}
