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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.accessibility.VideoPlayer.State;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link VideoPlayer}. */
@RunWith(RobolectricTestRunner.class)
public class VideoPlayerTest {

    @Mock
    private MediaPlayer mMediaPlayer;

    @Mock
    private TextureView mTextureView;

    @Mock
    private Surface mSurface;

    private VideoPlayer mVideoPlayer;

    @Before
    public void initVideoPlayer() {
        MockitoAnnotations.initMocks(this);

        final int videoRes = 0;
        final Context context = ApplicationProvider.getApplicationContext();

        mVideoPlayer = spy(VideoPlayer.create(context, videoRes, mTextureView));
        mVideoPlayer.mMediaPlayer = mMediaPlayer;
        mVideoPlayer.mAnimationSurface = mSurface;
    }

    @Test
    public void setSurfaceTextureListener_success() {
        verify(mTextureView).setSurfaceTextureListener(any());
    }

    @Test
    public void onPlayerPaused_startedState_pause() {
        mVideoPlayer.mMediaPlayerState = State.STARTED;

        mVideoPlayer.pause();

        assertThat(mVideoPlayer.mMediaPlayerState).isEqualTo(State.PAUSED);
        verify(mMediaPlayer).pause();
    }

    @Test
    public void onPlayerResumed_pausedState_start() {
        mVideoPlayer.mMediaPlayerState = State.PAUSED;

        mVideoPlayer.resume();

        assertThat(mVideoPlayer.mMediaPlayerState).isEqualTo(State.STARTED);
        verify(mMediaPlayer).start();
    }

    @Test
    public void onPlayerReleased_stoppedState_release() {
        mVideoPlayer.mMediaPlayerState = State.STOPPED;

        mVideoPlayer.release();

        assertThat(mVideoPlayer.mMediaPlayerState).isEqualTo(State.END);
        verify(mMediaPlayer).release();
        verify(mSurface).release();
    }

    @Test
    public void onSurfaceTextureDestroyed_preparedState_release() {
        mVideoPlayer.mMediaPlayerState = State.PREPARED;

        mVideoPlayer.onSurfaceTextureDestroyed(any());

        assertThat(mVideoPlayer.mMediaPlayerState).isEqualTo(State.END);
        verify(mMediaPlayer).release();
        verify(mSurface).release();
    }
}
