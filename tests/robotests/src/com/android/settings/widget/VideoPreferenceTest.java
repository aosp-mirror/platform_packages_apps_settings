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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowSettingsMediaPlayer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSettingsMediaPlayer.class)
public class VideoPreferenceTest {
    private static final int VIDEO_WIDTH = 100;
    private static final int VIDEO_HEIGHT = 150;

    private VideoPreference.AnimationController mAnimationController;
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
        mAnimationController = spy(
                new MediaAnimationController(mContext, R.raw.accessibility_screen_magnification));
        mVideoPreference = new VideoPreference(mContext, null /* attrs */);
        mVideoPreference.mAnimationController = mAnimationController;
        when(mAnimationController.getVideoWidth()).thenReturn(VIDEO_WIDTH);
        when(mAnimationController.getVideoHeight()).thenReturn(VIDEO_HEIGHT);

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
        mVideoPreference.onBindViewHolder(mPreferenceViewHolder);
        mAnimationController.attachView(video, fakePreview, fakePlayButton);
        when(mAnimationController.isPlaying()).thenReturn(false);
        final TextureView.SurfaceTextureListener listener = video.getSurfaceTextureListener();

        mVideoPreference.onViewInvisible();
        listener.onSurfaceTextureUpdated(mock(SurfaceTexture.class));

        verify(mAnimationController, never()).start();
    }

    @Test
    public void onViewInvisible_shouldReleaseMediaplayer() {
        mVideoPreference.onViewInvisible();

        verify(mAnimationController).release();
    }

    @Test
    public void updateViewStates_paused_updatesViews() {
        mAnimationController.start();

        mVideoPreference.mAnimationController.attachView(new TextureView(mContext), fakePreview,
                fakePlayButton);

        verify(fakePlayButton).setVisibility(eq(View.VISIBLE));
        verify(fakePreview).setVisibility(eq(View.VISIBLE));
        assertThat(mAnimationController.isPlaying()).isFalse();
    }

    @Test
    public void updateViewStates_playing_updatesViews() {
        mAnimationController.pause();

        mVideoPreference.mAnimationController.attachView(new TextureView(mContext), fakePreview,
                fakePlayButton);

        verify(fakePlayButton).setVisibility(eq(View.GONE));
        verify(fakePreview).setVisibility(eq(View.GONE));
        assertThat(mAnimationController.isPlaying()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void onViewVisible_createAnimationController() {
        final PreferenceFragmentCompat fragment = FragmentController.of(
                new VideoPreferenceTest.TestFragment(),
                new Bundle())
                .create()
                .start()
                .resume()
                .get();

        final VideoPreference vp1 = fragment.findPreference("video1");
        final VideoPreference vp2 = fragment.findPreference("video2");

        assertThat(vp1.mAnimationController instanceof MediaAnimationController).isTrue();
        assertThat(vp2.mAnimationController instanceof VectorAnimationController).isTrue();
    }

    public static class TestFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.video_preference);
        }
    }
}
