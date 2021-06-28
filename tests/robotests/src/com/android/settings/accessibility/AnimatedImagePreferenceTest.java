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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.airbnb.lottie.LottieAnimationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.InputStream;

/** Tests for {@link AnimatedImagePreference}. */
@RunWith(RobolectricTestRunner.class)
public class AnimatedImagePreferenceTest {
    private final Context mContext = RuntimeEnvironment.application;
    private Uri mImageUri;
    private PreferenceViewHolder mViewHolder;
    private AnimatedImagePreference mAnimatedImagePreference;

    @Mock
    private ViewGroup mRootView;

    @Spy
    private ImageView mImageView;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(mRootView));
        doReturn(new LottieAnimationView(mContext)).when(mRootView).findViewById(R.id.lottie_view);
        mImageView = spy(new ImageView(mContext));

        mAnimatedImagePreference = new AnimatedImagePreference(mContext);
        mImageUri = new Uri.Builder().build();
    }

    @Test
    public void playAnimation_animatedImageDrawable_success() {
        final AnimatedImageDrawable drawable = mock(AnimatedImageDrawable.class);
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);
        doReturn(drawable).when(mImageView).getDrawable();

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(drawable).start();
    }

    @Test
    public void playAnimation_animatedVectorDrawable_success() {
        final AnimatedVectorDrawable drawable = mock(AnimatedVectorDrawable.class);
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);
        doReturn(drawable).when(mImageView).getDrawable();

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(drawable).start();
    }

    @Test
    public void playAnimation_animationDrawable_success() {
        final AnimationDrawable drawable = mock(AnimationDrawable.class);
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);
        doReturn(drawable).when(mImageView).getDrawable();

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(drawable).start();
    }

    @Test
    public void setImageUri_viewNotExist_setFail() {
        doReturn(null).when(mRootView).findViewById(R.id.animated_img);

        mAnimatedImagePreference.setImageUri(mImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(mImageView, never()).setImageURI(mImageUri);
    }

    @Test
    public void setMaxHeight_success() {
        final int maxHeight = 100;
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);

        mAnimatedImagePreference.setMaxHeight(maxHeight);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        assertThat(mImageView.getMaxHeight()).isEqualTo(maxHeight);
    }

    @Test
    public void setImageUriAndRebindViewHolder_lottieImageFromRawFolder_setAnimation() {
        final int fakeLottieResId = 111111;
        final Uri lottieImageUri =
                new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mContext.getPackageName())
                .appendPath(String.valueOf(fakeLottieResId))
                .build();
        final LottieAnimationView lottieView = spy(new LottieAnimationView(mContext));
        doReturn(mImageView).when(mRootView).findViewById(R.id.animated_img);
        doReturn(lottieView).when(mRootView).findViewById(R.id.lottie_view);

        mAnimatedImagePreference.setImageUri(lottieImageUri);
        mAnimatedImagePreference.onBindViewHolder(mViewHolder);

        verify(lottieView).setAnimation(any(InputStream.class), eq(null));
    }
}
