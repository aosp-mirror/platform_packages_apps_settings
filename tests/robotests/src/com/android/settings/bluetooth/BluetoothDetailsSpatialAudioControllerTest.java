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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
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
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String KEY_SPATIAL_AUDIO = "spatial_audio";
    private static final String KEY_HEAD_TRACKING = "head_tracking";

    @Mock private AudioManager mAudioManager;
    @Mock private Spatializer mSpatializer;
    @Mock private Lifecycle mSpatialAudioLifecycle;
    @Mock private PreferenceCategory mProfilesContainer;
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private A2dpProfile mA2dpProfile;
    @Mock private LeAudioProfile mLeAudioProfile;
    @Mock private HearingAidProfile mHearingAidProfile;

    private AudioDeviceAttributes mAvailableDevice;

    private BluetoothDetailsSpatialAudioController mController;
    private TwoStatePreference mSpatialAudioPref;
    private TwoStatePreference mHeadTrackingPref;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mAudioManager.getSpatializer()).thenReturn(mSpatializer);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getProfiles())
                .thenReturn(List.of(mA2dpProfile, mLeAudioProfile, mHearingAidProfile));
        when(mA2dpProfile.isEnabled(mBluetoothDevice)).thenReturn(true);
        when(mA2dpProfile.getProfileId()).thenReturn(BluetoothProfile.A2DP);
        when(mLeAudioProfile.getProfileId()).thenReturn(BluetoothProfile.LE_AUDIO);
        when(mHearingAidProfile.getProfileId()).thenReturn(BluetoothProfile.HEARING_AID);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(MAC_ADDRESS);
        when(mFeatureFactory.getBluetoothFeatureProvider().getSpatializer(mContext))
                .thenReturn(mSpatializer);

        mController =
                new BluetoothDetailsSpatialAudioController(
                        mContext, mFragment, mCachedDevice, mSpatialAudioLifecycle);
        mController.mProfilesContainer = mProfilesContainer;

        mSpatialAudioPref = mController.createSpatialAudioPreference(mContext);
        mHeadTrackingPref = mController.createHeadTrackingPreference(mContext);

        when(mProfilesContainer.findPreference(KEY_SPATIAL_AUDIO)).thenReturn(mSpatialAudioPref);
        when(mProfilesContainer.findPreference(KEY_HEAD_TRACKING)).thenReturn(mHeadTrackingPref);

        mAvailableDevice =
                new AudioDeviceAttributes(
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
        when(mSpatializer.getImmersiveAudioLevel())
                .thenReturn(SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL);
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
    public void refresh_spatialAudioOnHeadTrackingOff_recordMetrics() {
        mController.setAvailableDevice(mAvailableDevice);
        when(mSpatializer.isAvailableForDevice(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.getCompatibleAudioDevices())
                .thenReturn(ImmutableList.of(mAvailableDevice));
        when(mSpatializer.hasHeadTracker(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_SPATIAL_AUDIO_TRIGGERED,
                        true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TRIGGERED,
                        false);
    }

    @Test
    public void refresh_spatialAudioOff_recordMetrics() {
        mController.setAvailableDevice(mAvailableDevice);
        when(mSpatializer.isAvailableForDevice(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(ImmutableList.of());
        when(mSpatializer.hasHeadTracker(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.isHeadTrackerEnabled(mController.mAudioDevice)).thenReturn(false);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_SPATIAL_AUDIO_TRIGGERED,
                        false);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TRIGGERED,
                        false);
    }

    @Test
    public void refresh_spatialAudioOnAndHeadTrackingIsNotAvailable_hidesHeadTrackingPreference() {
        mController.setAvailableDevice(mAvailableDevice);
        when(mSpatializer.isAvailableForDevice(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.getCompatibleAudioDevices())
                .thenReturn(ImmutableList.of(mAvailableDevice));
        when(mSpatializer.hasHeadTracker(mAvailableDevice)).thenReturn(false);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mHeadTrackingPref.isVisible()).isFalse();
    }

    @Test
    public void refresh_spatialAudioOff_hidesHeadTrackingPreference() {
        mController.setAvailableDevice(mAvailableDevice);
        when(mSpatializer.isAvailableForDevice(mAvailableDevice)).thenReturn(true);
        when(mSpatializer.getCompatibleAudioDevices()).thenReturn(ImmutableList.of());
        when(mSpatializer.hasHeadTracker(mAvailableDevice)).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mHeadTrackingPref.isVisible()).isFalse();
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
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TRIGGERED,
                        true);
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
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TRIGGERED,
                        false);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DETERMINING_SPATIAL_AUDIO_ATTRIBUTES_BY_PROFILE)
    public void refresh_leAudioProfileEnabledForHeadset_useLeAudioHeadsetAttributes() {
        when(mLeAudioProfile.isEnabled(mBluetoothDevice)).thenReturn(true);
        when(mA2dpProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mHearingAidProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mAudioManager.getBluetoothAudioDeviceCategory(MAC_ADDRESS))
                .thenReturn(AudioManager.AUDIO_DEVICE_CATEGORY_HEADPHONES);
        when(mSpatializer.isAvailableForDevice(any())).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mController.mAudioDevice.getType()).isEqualTo(AudioDeviceInfo.TYPE_BLE_HEADSET);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DETERMINING_SPATIAL_AUDIO_ATTRIBUTES_BY_PROFILE)
    public void refresh_leAudioProfileEnabledForSpeaker_useLeAudioSpeakerAttributes() {
        when(mLeAudioProfile.isEnabled(mBluetoothDevice)).thenReturn(true);
        when(mA2dpProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mHearingAidProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mAudioManager.getBluetoothAudioDeviceCategory(MAC_ADDRESS))
                .thenReturn(AudioManager.AUDIO_DEVICE_CATEGORY_SPEAKER);
        when(mSpatializer.isAvailableForDevice(any())).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mController.mAudioDevice.getType()).isEqualTo(AudioDeviceInfo.TYPE_BLE_SPEAKER);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DETERMINING_SPATIAL_AUDIO_ATTRIBUTES_BY_PROFILE)
    public void refresh_hearingAidProfileEnabled_useHearingAidAttributes() {
        when(mLeAudioProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mA2dpProfile.isEnabled(mBluetoothDevice)).thenReturn(false);
        when(mHearingAidProfile.isEnabled(mBluetoothDevice)).thenReturn(true);
        when(mSpatializer.isAvailableForDevice(any())).thenReturn(true);

        mController.refresh();
        ShadowLooper.idleMainLooper();

        assertThat(mController.mAudioDevice.getType()).isEqualTo(AudioDeviceInfo.TYPE_HEARING_AID);
    }

    @Test
    public void turnedOnSpatialAudio_invokesAddCompatibleAudioDevice() {
        mController.setAvailableDevice(mAvailableDevice);
        mSpatialAudioPref.setChecked(true);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).addCompatibleAudioDevice(mController.mAudioDevice);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_SPATIAL_AUDIO_TOGGLE_CLICKED,
                        true);
    }

    @Test
    public void turnedOffSpatialAudio_invokesRemoveCompatibleAudioDevice() {
        mController.setAvailableDevice(mAvailableDevice);
        mSpatialAudioPref.setChecked(false);
        mController.onPreferenceClick(mSpatialAudioPref);
        verify(mSpatializer).removeCompatibleAudioDevice(mController.mAudioDevice);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_SPATIAL_AUDIO_TOGGLE_CLICKED,
                        false);
    }

    @Test
    public void turnedOnHeadTracking_invokesSetHeadTrackerEnabled_setsTrue() {
        mController.setAvailableDevice(mAvailableDevice);
        mHeadTrackingPref.setChecked(true);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(true, mController.mAudioDevice);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TOGGLE_CLICKED,
                        true);
    }

    @Test
    public void turnedOffHeadTracking_invokesSetHeadTrackerEnabled_setsFalse() {
        mController.setAvailableDevice(mAvailableDevice);
        mHeadTrackingPref.setChecked(false);
        mController.onPreferenceClick(mHeadTrackingPref);
        verify(mSpatializer).setHeadTrackerEnabled(false, mController.mAudioDevice);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_HEAD_TRACKING_TOGGLE_CLICKED,
                        false);
    }
}
