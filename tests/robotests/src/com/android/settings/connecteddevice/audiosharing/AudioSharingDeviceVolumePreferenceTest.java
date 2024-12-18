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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.widget.SeekBar;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
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

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class AudioSharingDeviceVolumePreferenceTest {
    private static final int TEST_DEVICE_GROUP_ID = 1;
    private static final int TEST_VOLUME_VALUE = 255;
    private static final int TEST_MAX_STREAM_VALUE = 10;
    private static final int TEST_MIN_STREAM_VALUE = 0;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private BluetoothDevice mDevice;
    @Mock private AudioManager mAudioManager;
    @Mock private SeekBar mSeekBar;
    private Context mContext;
    private AudioSharingDeviceVolumePreference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setup() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                .thenReturn(TEST_MAX_STREAM_VALUE);
        when(mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC))
                .thenReturn(TEST_MIN_STREAM_VALUE);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID);
        when(mSeekBar.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mPreference = new AudioSharingDeviceVolumePreference(mContext, mCachedDevice);
    }

    @Test
    public void getCachedDevice_returnsDevice() {
        assertThat(mPreference.getCachedDevice()).isEqualTo(mCachedDevice);
    }

    @Test
    public void initialize_setupMaxMin() {
        mPreference.initialize();
        assertThat(mPreference.getMax()).isEqualTo(AudioSharingDeviceVolumePreference.MAX_VOLUME);
        assertThat(mPreference.getMin()).isEqualTo(AudioSharingDeviceVolumePreference.MIN_VOLUME);
    }

    @Test
    public void onStopTrackingTouch_notFallbackDevice_setDeviceVolume() {
        mPreference.onStopTrackingTouch(mSeekBar);

        verify(mVolumeControl).setDeviceVolume(mDevice, TEST_VOLUME_VALUE, /* isGroupOp= */ true);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                        /* isPrimary= */ false);
    }

    @Test
    public void onProgressChanged_notFallbackDevice_fromUserNotInTouch_setDeviceVolume() {
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ true);

        verify(mVolumeControl).setDeviceVolume(mDevice, TEST_VOLUME_VALUE, /* isGroupOp= */ true);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                        /* isPrimary= */ false);
    }

    @Test
    public void onProgressChanged_notFallbackDevice_fromUserInTouch_doNothing() {
        mPreference.onStartTrackingTouch(mSeekBar);
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ true);

        verifyNoInteractions(mVolumeControl);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME),
                        anyBoolean());
    }

    @Test
    public void onProgressChanged_notFallbackDevice_notFromUserNotInTouch_doNothing() {
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ false);

        verifyNoInteractions(mVolumeControl);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME),
                        anyBoolean());
    }

    @Test
    public void onStopTrackingTouch_fallbackDevice_setDeviceVolume() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID);
        mPreference.onStopTrackingTouch(mSeekBar);

        verifyNoInteractions(mVolumeControl);
        verify(mAudioManager)
                .setStreamVolume(AudioManager.STREAM_MUSIC, TEST_MAX_STREAM_VALUE, /* flags= */ 0);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                        /* isPrimary= */ true);
    }

    @Test
    public void onProgressChanged_fallbackDevice_fromUserNotInTouch_setDeviceVolume() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID);
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ true);

        verifyNoInteractions(mVolumeControl);
        verify(mAudioManager)
                .setStreamVolume(AudioManager.STREAM_MUSIC, TEST_MAX_STREAM_VALUE, /* flags= */ 0);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                        /* isPrimary= */ true);
    }

    @Test
    public void onProgressChanged_fallbackDevice_fromUserInTouch_doNothing() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID);
        mPreference.onStartTrackingTouch(mSeekBar);
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ true);

        verifyNoInteractions(mVolumeControl);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME),
                        anyBoolean());
    }

    @Test
    public void onProgressChanged_fallbackDevice_notFromUserNotInTouch_doNothing() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID);
        mPreference.onProgressChanged(mSeekBar, TEST_VOLUME_VALUE, /* fromUser= */ false);

        verifyNoInteractions(mVolumeControl);
        verifyNoInteractions(mAudioManager);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME),
                        anyBoolean());
    }
}
