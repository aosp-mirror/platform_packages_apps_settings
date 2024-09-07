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

import static org.mockito.Mockito.*;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowLocalMediaManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.BluetoothMediaDevice;
import com.android.settingslib.media.LocalMediaManager;

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

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAudioStreamsHelper.class,
            ShadowLocalMediaManager.class,
        })
public class MediaControlHelperTest {
    private static final String FAKE_PACKAGE = "fake_package";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private Context mContext;
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private MediaSessionManager mMediaSessionManager;
    @Mock private MediaController mMediaController;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private PlaybackState mPlaybackState;
    @Mock private LocalMediaManager mLocalMediaManager;
    @Mock private BluetoothMediaDevice mBluetoothMediaDevice;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);
        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(List.of(mMediaController));
        when(mMediaController.getPackageName()).thenReturn(FAKE_PACKAGE);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        ShadowLocalMediaManager.setUseMock(mLocalMediaManager);
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
        ShadowLocalMediaManager.reset();
    }

    @Test
    public void testStart_noBluetoothManager_doNothing() {
        MediaControlHelper helper = new MediaControlHelper(mContext, null);
        helper.start();

        verify(mLocalMediaManager, never()).startScan();
    }

    @Test
    public void testStart_noConnectedDevice_doNothing() {
        MediaControlHelper helper = new MediaControlHelper(mContext, mLocalBluetoothManager);
        helper.start();

        verify(mLocalMediaManager, never()).startScan();
    }

    @Test
    public void testStart_isStopped_onDeviceListUpdate_shouldNotStopMedia() {
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_STOPPED);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(null);

        MediaControlHelper helper = new MediaControlHelper(mContext, mLocalBluetoothManager);
        helper.start();
        ShadowLocalMediaManager.onDeviceListUpdate();

        verify(mMediaController, never()).getTransportControls();
    }

    @Test
    public void testStart_isPlaying_onDeviceListUpdate_noDevice_shouldNotStopMedia() {
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_PLAYING);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(null);

        MediaControlHelper helper = new MediaControlHelper(mContext, mLocalBluetoothManager);
        helper.start();
        ShadowLocalMediaManager.onDeviceListUpdate();

        verify(mMediaController, never()).getTransportControls();
    }

    @Test
    public void testStart_isPlaying_onDeviceListUpdate_deviceMatch_shouldStopMedia() {
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_PLAYING);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(mBluetoothMediaDevice);
        when(mBluetoothMediaDevice.getCachedDevice()).thenReturn(mCachedBluetoothDevice);

        MediaControlHelper helper = new MediaControlHelper(mContext, mLocalBluetoothManager);
        helper.start();
        ShadowLocalMediaManager.onDeviceListUpdate();

        verify(mMediaController).getTransportControls();
    }
}
