/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.TextureView;

import android.view.View;
import android.widget.ImageView;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VideoPreferenceTest {
    private static final int VIDEO_WIDTH = 100;
    private static final int VIDEO_HEIGHT = 150;

    @Mock
    private MediaPlayer mMediaPlayer;
    @Mock
    private ImageView fakePreview;
    @Mock
    private ImageView fakePlayButton;
    private Context mContext;
    private VideoPreference mVideoPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mVideoPreference = new VideoPreference(mContext, null /* attrs */);
        mVideoPreference.mMediaPlayer = mMediaPlayer;
        when(mMediaPlayer.getVideoWidth()).thenReturn(VIDEO_WIDTH);
        when(mMediaPlayer.getVideoHeight()).thenReturn(VIDEO_HEIGHT);

        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.video_preference, null));
    }

    @Test
    public void onBindViewHolder_hasCorrectRatio() {
        mVideoPreference.mAnimationAvailable = true;

        mVideoPreference.updateAspectRatio();
        mVideoPreference.onBindViewHolder(mPreferenceViewHolder);

        final AspectRatioFrameLayout layout =
                (AspectRatioFrameLayout) mPreferenceViewHolder.findViewById(R.id.video_container);
        assertThat(layout.mAspectRatio).isWithin(0.01f).of(VIDEO_WIDTH / (float) VIDEO_HEIGHT);
    }

    @Test
    public void onSurfaceTextureUpdated_viewInvisible_shouldNotStartPlayingVideo() {
        final TextureView video =
            (TextureView) mPreferenceViewHolder.findViewById(R.id.video_texture_view);
        mVideoPreference.mAnimationAvailable = true;
        mVideoPreference.mVideoReady = true;
        mVideoPreference.onViewInvisible();
        mVideoPreference.onBindViewHolder(mPreferenceViewHolder);
        when(mMediaPlayer.isPlaying()).thenReturn(false);
        final TextureView.SurfaceTextureListener listener = video.getSurfaceTextureListener();

        listener.onSurfaceTextureUpdated(mock(SurfaceTexture.class));

        verify(mMediaPlayer, never()).start();
    }

    @Test
    public void onViewInvisible_shouldReleaseMediaplayer() {
        mVideoPreference.onViewVisible(false);

        mVideoPreference.onViewInvisible();

        verify(mMediaPlayer).release();
    }

    @Test
    public void updateViewStates_paused_updatesViews() {
        when(mMediaPlayer.isPlaying()).thenReturn(true);
        mVideoPreference.updateViewStates(fakePreview, fakePlayButton);
        verify(fakePlayButton).setVisibility(eq(View.VISIBLE));
        verify(fakePreview).setVisibility(eq(View.VISIBLE));
        verify(mMediaPlayer).pause();
    }

    @Test
    public void updateViewStates_playing_updatesViews() {
        when(mMediaPlayer.isPlaying()).thenReturn(false);
        mVideoPreference.updateViewStates(fakePreview, fakePlayButton);
        verify(fakePlayButton).setVisibility(eq(View.GONE));
        verify(fakePreview).setVisibility(eq(View.GONE));
        verify(mMediaPlayer).start();
    }

    @Test
    public void updateViewStates_noMediaPlayer_skips() {
        mVideoPreference.mMediaPlayer = null;
        mVideoPreference.updateViewStates(fakePreview, fakePlayButton);
        verify(fakePlayButton, never()).setVisibility(anyInt());
        verify(fakePreview, never()).setVisibility(anyInt());
    }
}
