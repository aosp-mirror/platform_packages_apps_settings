/*
 * Copyright (C) 2020 The Android Open Source Project
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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.VolumeProvider;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

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
public class MediaOutputUtilsTest {

    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;

    private Context mContext;
    private List<MediaController> mMediaControllers = new ArrayList<>();
    private PlaybackState mPlaybackState;
    private MediaController.PlaybackInfo mPlaybackInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mMediaSessionManager).when(mContext).getSystemService(MediaSessionManager.class);
        mMediaControllers.add(mMediaController);
        doReturn(mMediaControllers).when(mMediaSessionManager).getActiveSessions(any());
    }

    @Test
    public void getActiveLocalMediaController_localMediaPlaying_returnController() {
        initPlayback(PlaybackState.STATE_PLAYING);

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isEqualTo(
                mMediaController);
    }

    @Test
    public void getActiveLocalMediaController_localMediaPause_returnController() {
        initPlayback(PlaybackState.STATE_PAUSED);

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isEqualTo(
                mMediaController);
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

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
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

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
    }

    @Test
    public void getActiveLocalMediaController_localMediaNone_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_NONE, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
    }

    @Test
    public void getActiveLocalMediaController_localMediaError_returnNull() {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_ERROR, 0, 1)
                .build();

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
    }

    @Test
    public void getActiveLocalMediaController_bothHaveRemoteMediaAndLocalMedia_returnNull() {
        mMediaControllers.clear();
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
        mMediaControllers.add(mMediaController);
        initPlayback(PlaybackState.STATE_PLAYING);

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(remoteMediaController.getPlaybackInfo()).thenReturn(playbackInfo);
        when(remoteMediaController.getPlaybackState()).thenReturn(playbackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
    }

    @Test
    public void getActiveLocalMediaController_bothHaveLocalMediaAndRemoteMedia_returnNull() {
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
        initPlayback(PlaybackState.STATE_PLAYING);

        when(mMediaController.getPlaybackInfo()).thenReturn(mPlaybackInfo);
        when(mMediaController.getPlaybackState()).thenReturn(mPlaybackState);
        when(remoteMediaController.getPlaybackInfo()).thenReturn(playbackInfo);
        when(remoteMediaController.getPlaybackState()).thenReturn(playbackState);

        assertThat(MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager)).isNull();
    }

    private void initPlayback(int playbackState) {
        mPlaybackInfo = new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                VolumeProvider.VOLUME_CONTROL_ABSOLUTE,
                100,
                10,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                null);
        mPlaybackState = new PlaybackState.Builder()
                .setState(playbackState, 0, 1)
                .build();
    }
}
