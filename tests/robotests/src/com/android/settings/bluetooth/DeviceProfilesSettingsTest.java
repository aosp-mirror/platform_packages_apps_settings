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

package com.android.settings.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settingslib.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.FragmentTestUtil;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows = {
        ShadowEventLogWriter.class
})
public class DeviceProfilesSettingsTest {
    Context mContext;
    @Mock Activity mActivity;
    @Mock LocalBluetoothManager mManager;
    @Mock LocalBluetoothAdapter mAdapter;
    @Mock LocalBluetoothProfileManager mProfileManager;
    @Mock CachedBluetoothDeviceManager mDeviceManager;
    @Mock CachedBluetoothDevice mCachedDevice;
    @Mock A2dpProfile mProfile;

    ArrayList<LocalBluetoothProfile> mProfiles;
    DeviceProfilesSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        when(mProfile.getNameResource(any())).thenReturn(R.string.bluetooth_profile_a2dp);
        mProfiles = new ArrayList<>();
        mProfiles.add(mProfile);
        when(mCachedDevice.getConnectableProfiles()).thenReturn(mProfiles);

        mFragment = new DeviceProfilesSettings();
        mFragment.setArguments(new Bundle());

        ReflectionHelpers.setStaticField(LocalBluetoothManager.class, "sInstance", mManager);
        when(mManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mManager.getBluetoothAdapter()).thenReturn(mAdapter);
        when(mManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getMapProfile()).thenReturn(null);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
    }

    @Test
    public void deviceHasHighQualityAudio() {
        when(mProfile.supportsHighQualityAudio(any())).thenReturn(true);
        when(mProfile.isHighQualityAudioEnabled(any())).thenReturn(true);
        when(mProfile.isPreferred(any())).thenReturn(true);
        FragmentTestUtil.startFragment(mFragment);

        ViewGroup profilesGroup = mFragment.getDialog().findViewById(R.id.profiles_section);
        CheckBox box = (CheckBox) profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(box).isNotNull();
        assertThat(box.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(box.isEnabled()).isTrue();
        assertThat(box.isChecked()).isTrue();

        box.performClick();
        verify(mProfile).setHighQualityAudioEnabled(any(), eq(false));
        box.performClick();
        verify(mProfile).setHighQualityAudioEnabled(any(), eq(true));
    }

    @Test
    public void busyDeviceDisablesControl() {
        when(mProfile.supportsHighQualityAudio(any())).thenReturn(true);
        when(mProfile.isHighQualityAudioEnabled(any())).thenReturn(true);
        when(mProfile.isPreferred(any())).thenReturn(true);
        when(mCachedDevice.isBusy()).thenReturn(true);
        FragmentTestUtil.startFragment(mFragment);

        // Make sure that the high quality audio option is present but disabled when the device
        // is busy.
        ViewGroup profilesGroup = mFragment.getDialog().findViewById(R.id.profiles_section);
        CheckBox box = (CheckBox) profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(box).isNotNull();
        assertThat(box.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(box.isEnabled()).isFalse();
    }

    @Test
    public void mediaAudioGetsDisabledAndReEnabled() {
        when(mProfile.supportsHighQualityAudio(any())).thenReturn(true);
        when(mProfile.isHighQualityAudioEnabled(any())).thenReturn(true);
        when(mProfile.isPreferred(any())).thenReturn(true);
        FragmentTestUtil.startFragment(mFragment);

        ViewGroup profilesGroup = mFragment.getDialog().findViewById(R.id.profiles_section);
        CheckBox audioBox = profilesGroup.findViewWithTag(mProfile.toString());
        CheckBox highQualityAudioBox = profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(audioBox).isNotNull();
        assertThat(audioBox.isChecked()).isTrue();
        assertThat(highQualityAudioBox).isNotNull();
        assertThat(highQualityAudioBox.isChecked()).isTrue();

        // Disabling media audio should cause the high quality audio box to disappear.
        when(mProfile.isPreferred(any())).thenReturn(false);
        mFragment.onDeviceAttributesChanged();
        audioBox = profilesGroup.findViewWithTag(mProfile.toString());
        highQualityAudioBox = profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(audioBox).isNotNull();
        assertThat(audioBox.isChecked()).isFalse();
        assertThat(highQualityAudioBox).isNotNull();
        assertThat(highQualityAudioBox.getVisibility()).isEqualTo(View.GONE);

        // And re-enabling media audio should make it reappear.
        when(mProfile.isPreferred(any())).thenReturn(true);
        mFragment.onDeviceAttributesChanged();
        audioBox = profilesGroup.findViewWithTag(mProfile.toString());
        highQualityAudioBox = profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(audioBox).isNotNull();
        assertThat(audioBox.isChecked()).isTrue();
        assertThat(highQualityAudioBox).isNotNull();
        assertThat(highQualityAudioBox.isChecked()).isTrue();
    }

    @Test
    public void mediaAudioStartsDisabled() {
        when(mProfile.supportsHighQualityAudio(any())).thenReturn(true);
        when(mProfile.isHighQualityAudioEnabled(any())).thenReturn(true);
        when(mProfile.isPreferred(any())).thenReturn(false);

        FragmentTestUtil.startFragment(mFragment);
        ViewGroup profilesGroup = mFragment.getDialog().findViewById(R.id.profiles_section);
        CheckBox audioBox = profilesGroup.findViewWithTag(mProfile.toString());
        CheckBox highQualityAudioBox = profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);

        assertThat(audioBox).isNotNull();
        assertThat(audioBox.isChecked()).isFalse();
        assertThat(highQualityAudioBox).isNotNull();
        assertThat(highQualityAudioBox.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void deviceDoesntHaveHighQualityAudio() {
        when(mProfile.supportsHighQualityAudio(any())).thenReturn(false);
        when(mProfile.isPreferred(any())).thenReturn(true);
        FragmentTestUtil.startFragment(mFragment);

        // A device that doesn't support high quality audio shouldn't have the checkbox for
        // high quality audio support.
        ViewGroup profilesGroup = mFragment.getDialog().findViewById(R.id.profiles_section);
        CheckBox box = (CheckBox) profilesGroup.findViewWithTag(
                DeviceProfilesSettings.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(box).isNull();
    }

}
