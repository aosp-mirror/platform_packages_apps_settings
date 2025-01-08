/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowAudioStreamsHelper.class})
public class BluetoothDetailsAudioSharingControllerTest extends BluetoothDetailsControllerTestBase {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;

    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothDetailsAudioSharingController mController;
    private PreferenceCategory mContainer;

    @Override
    public void setUp() {
        super.setUp();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        mController =
                new BluetoothDetailsAudioSharingController(
                        mContext, mFragment, mLocalManager, mCachedDevice, mLifecycle);
        mContainer = new PreferenceCategory(mContext);
        mContainer.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mContainer);
        setupDevice(mDeviceConfig);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void notConnected_noAudioSharingPreferences() {
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);

        showScreen(mController);

        assertThat(mContainer.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void connected_showOnePreference() {
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(true);
        when(mCachedDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        when(mLocalManager
                        .getProfileManager()
                        .getLeAudioBroadcastAssistantProfile()
                        .getAllSources(mDevice))
                .thenReturn(List.of());
        when(mLocalManager
                .getProfileManager()
                .getLeAudioBroadcastProfile()
                .isEnabled(mDevice))
                .thenReturn(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

        showScreen(mController);

        assertThat(mContainer.isVisible()).isTrue();
        assertThat(mContainer.getPreferenceCount()).isEqualTo(1);
        assertThat(mContainer.getPreference(0).getTitle())
                .isEqualTo(mContext.getString(R.string.audio_sharing_title));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void connected_active_showTwoPreference() {
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(true);
        when(mCachedDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(true);
        when(mLocalManager
                .getProfileManager()
                .getLeAudioBroadcastAssistantProfile()
                .getAllSources(mDevice))
                .thenReturn(List.of());
        when(mLocalManager
                .getProfileManager()
                .getLeAudioBroadcastProfile()
                .isEnabled(mDevice))
                .thenReturn(false);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

        showScreen(mController);

        assertThat(mContainer.isVisible()).isTrue();
        assertThat(mContainer.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void connected_hasConnectedBroadcastSource_showTwoPreference() {
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(true);
        when(mCachedDevice.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        when(mLocalManager
                .getProfileManager()
                .getLeAudioBroadcastAssistantProfile()
                .getAllSources(mDevice))
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mLocalManager
                .getProfileManager()
                .getLeAudioBroadcastProfile()
                .isEnabled(mDevice))
                .thenReturn(false);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

        showScreen(mController);

        assertThat(mContainer.isVisible()).isTrue();
        assertThat(mContainer.getPreferenceCount()).isEqualTo(2);
    }
}
