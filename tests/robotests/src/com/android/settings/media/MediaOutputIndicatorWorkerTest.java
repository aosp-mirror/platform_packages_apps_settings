/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.VolumeProvider;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;

import com.android.settings.bluetooth.Utils;
import com.android.settings.slices.ShadowSliceBackgroundWorker;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.media.LocalMediaManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class,
        ShadowSliceBackgroundWorker.class})
public class MediaOutputIndicatorWorkerTest {
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String TEST_PACKAGE_NAME = "com.android.test";
    private static final String TEST_PACKAGE_NAME2 = "com.android.test2";

    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothManager mLocalBtManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;
    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private LocalBluetoothLeBroadcast mLeAudioBroadcastProfile;

    private Context mContext;
    private MediaOutputIndicatorWorker mMediaOutputIndicatorWorker;
    private ShadowApplication mShadowApplication;
    private ContentResolver mResolver;
    private List<MediaController> mMediaControllers = new ArrayList<>();
    private PlaybackState mPlaybackState;
    private MediaController.PlaybackInfo mPlaybackInfo;
    private LocalBluetoothManager mLocalBluetoothManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowApplication = ShadowApplication.getInstance();
        mContext = spy(RuntimeEnvironment.application);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile()).thenReturn(null);
        mMediaOutputIndicatorWorker = new MediaOutputIndicatorWorker(mContext, URI);
        doReturn(mMediaSessionManager).when(mContext).getSystemService(MediaSessionManager.class);
        mMediaControllers.add(mMediaController);
        doReturn(mMediaControllers).when(mMediaSessionManager).getActiveSessions(any());

        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
    }

    @Test
    public void onSlicePinned_registerCallback() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        initPlayback();
        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(mLocalMediaManager.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();

        verify(mBluetoothEventManager).registerCallback(mMediaOutputIndicatorWorker);
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mLocalMediaManager).registerCallback(mMediaOutputIndicatorWorker);
        verify(mLocalMediaManager).startScan();
    }

    @Test
    public void onSlicePinned_packageUpdated_checkPackageName() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        initPlayback();
        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(mLocalMediaManager.getPackageName()).thenReturn(TEST_PACKAGE_NAME);

        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();
        assertThat(mMediaOutputIndicatorWorker.mLocalMediaManager.getPackageName()).matches(
                TEST_PACKAGE_NAME);

        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGE_NAME2);
        when(mLocalMediaManager.getPackageName()).thenReturn(TEST_PACKAGE_NAME2);
        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();

        assertThat(mMediaOutputIndicatorWorker.mLocalMediaManager.getPackageName()).matches(
                TEST_PACKAGE_NAME2);
    }

    @Test
    public void onSlicePinned_noActiveController_noPackageName() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        mMediaControllers.clear();

        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();

        assertThat(mMediaOutputIndicatorWorker.mLocalMediaManager.getPackageName()).isNull();
    }

    private void waitForLocalMediaManagerInit() {
        for (int i = 0; i < 20; i++) {
            if (mMediaOutputIndicatorWorker.mLocalMediaManager != null) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void onSliceUnpinned_unRegisterCallback() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        initPlayback();
        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(mLocalMediaManager.getPackageName()).thenReturn(TEST_PACKAGE_NAME);

        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();
        mMediaOutputIndicatorWorker.onSliceUnpinned();

        verify(mBluetoothEventManager).unregisterCallback(mMediaOutputIndicatorWorker);
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mLocalMediaManager).unregisterCallback(mMediaOutputIndicatorWorker);
        verify(mLocalMediaManager).stopScan();
    }

    @Test
    public void onReceive_shouldNotifyChange() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        mMediaOutputIndicatorWorker.onSlicePinned();
        waitForLocalMediaManagerInit();

        final Intent intent = new Intent(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(intent)) {
            receiver.onReceive(mContext, intent);
        }
        // Intent receiver triggers notifyChange() again
        verify(mResolver).notifyChange(URI, null /* observer */);
    }

    @Test
    public void getActiveLocalMediaController_localMediaPlaying_returnController() {
        initPlayback();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isEqualTo(
                mMediaController);
    }

    private void initPlayback() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1)
                .build();
    }

    @Test
    public void getActiveLocalMediaController_remoteMediaPlaying_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isNull();
    }

    @Test
    public void getActiveLocalMediaController_localMediaStopped_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isNull();
    }

    @Test
    public void getActiveLocalMediaController_bothHaveRemoteMediaAndLocalMedia_returnNull() {
        final MediaController.PlaybackInfo playbackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        final PlaybackState playbackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1)
                .build();
        final MediaController remoteMediaController = mock(MediaController.class);

        mMediaControllers.add(remoteMediaController);
        initPlayback();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(remoteMediaController.getPlaybackInfo()).thenReturn(playbackInfo);
        when(remoteMediaController.getPlaybackState()).thenReturn(playbackState);

        assertThat(mMediaOutputIndicatorWorker.getActiveLocalMediaController()).isNull();
    }

    @Test
    public void isBroadcastSupported_leAudioBroadcastProfileIsNull_returnFalse() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        mMediaOutputIndicatorWorker.onSlicePinned();

        assertThat(mMediaOutputIndicatorWorker.isBroadcastSupported()).isFalse();
    }

    @Test
    public void isBroadcastSupported_leAudioBroadcastProfileNotNull_returnTrue() {
        mMediaOutputIndicatorWorker.mLocalMediaManager = mLocalMediaManager;
        mMediaOutputIndicatorWorker.onSlicePinned();
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile())
                .thenReturn(mLeAudioBroadcastProfile);

        assertThat(mMediaOutputIndicatorWorker.isBroadcastSupported()).isTrue();
    }
}
