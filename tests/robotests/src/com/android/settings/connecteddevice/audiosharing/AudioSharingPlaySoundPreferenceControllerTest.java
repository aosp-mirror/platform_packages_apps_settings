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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class AudioSharingPlaySoundPreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_play_sound";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private Ringtone mRingtone;

    private AudioSharingPlaySoundPreferenceController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mRingtone.getStreamType()).thenReturn(AudioManager.STREAM_MUSIC);
        when(mRingtone.getAudioAttributes()).thenReturn(new AudioAttributes.Builder().build());
        mController = new AudioSharingPlaySoundPreferenceController(mContext);
        mController.setRingtone(mRingtone);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_flagOn_available() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff_unsupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_nullRingtone_unsupported() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setRingtone(null);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_visible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_nullRingtone_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(true);
        mController.setRingtone(null);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_flagOff_invisible() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mPreference.setVisible(true);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_KEY);
    }

    @Test
    public void clickPreference_ringtoneIsNull_doNothing() {
        mController.setRingtone(null);
        when(mRingtone.isPlaying()).thenReturn(false);
        doNothing().when(mRingtone).play();
        mController.displayPreference(mScreen);
        mPreference.performClick();
        verify(mRingtone, times(0)).play();
    }

    @Test
    public void clickPreference_isPlaying_doNothing() {
        when(mRingtone.isPlaying()).thenReturn(true);
        doNothing().when(mRingtone).play();
        mController.displayPreference(mScreen);
        mPreference.performClick();
        verify(mRingtone, times(0)).play();
    }

    @Test
    public void clickPreference_notPlaying_play() {
        when(mRingtone.isPlaying()).thenReturn(false);
        doNothing().when(mRingtone).play();
        mController.displayPreference(mScreen);
        mPreference.performClick();
        verify(mRingtone).play();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_SHARING_PLAY_TEST_SOUND);
    }

    @Test
    public void nonStop_isPlaying_stop() {
        when(mRingtone.isPlaying()).thenReturn(true);
        doNothing().when(mRingtone).stop();
        mController.onStop(mLifecycleOwner);
        verify(mRingtone).stop();
    }

    @Test
    public void nonStop_notPlaying_doNothing() {
        when(mRingtone.isPlaying()).thenReturn(false);
        doNothing().when(mRingtone).stop();
        mController.onStop(mLifecycleOwner);
        verify(mRingtone, times(0)).stop();
    }
}
