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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.After;
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
@Config(
        shadows = {
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamsActiveDeviceSummaryUpdaterTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String DEVICE_NAME = "device_name";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final AudioStreamsActiveDeviceSummaryUpdater.OnSummaryChangeListener mFakeListener =
            summary -> mUpdatedSummary = summary;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    private @Nullable String mUpdatedSummary;
    private AudioStreamsActiveDeviceSummaryUpdater mUpdater;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        mUpdater = new AudioStreamsActiveDeviceSummaryUpdater(mContext, mFakeListener);
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
    }

    @Test
    public void register_summaryUpdated() {
        mUpdater.register(true);

        assertThat(mUpdatedSummary).isNotNull();
    }

    @Test
    public void unregister_doNothing() {
        mUpdater.register(false);

        assertThat(mUpdatedSummary).isNull();
    }

    @Test
    public void onProfileConnectionStateChanged_notLeAssistProfile_doNothing() {
        mUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice, 0, 0);

        assertThat(mUpdatedSummary).isNull();
    }

    @Test
    public void onProfileConnectionStateChanged_leAssistantProfile_summaryUpdated() {
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mCachedBluetoothDevice.getName()).thenReturn(DEVICE_NAME);
        mUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);

        assertThat(mUpdatedSummary).isEqualTo(DEVICE_NAME);
    }

    @Test
    public void onActiveDeviceChanged_leAssistantProfile_noDevice_summaryUpdated() {
        mUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);

        assertThat(mUpdatedSummary)
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_title));
    }

    @Test
    public void onBluetoothStateOff_summaryUpdated() {
        mUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);

        assertThat(mUpdatedSummary)
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_title));
    }
}
