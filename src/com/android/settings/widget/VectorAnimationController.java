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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.TextureView;
import android.view.View;

import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

/**
 * A {@link VideoPreference.AnimationController} containing a {@link
 * AnimatedVectorDrawableCompat}. The controller is used by {@link VideoPreference}
 * to display AnimatedVectorDrawable content.
 */
class VectorAnimationController implements VideoPreference.AnimationController {
    private AnimatedVectorDrawableCompat mAnimatedVectorDrawableCompat;
    private Drawable mPreviewDrawable;
    private Animatable2Compat.AnimationCallback mAnimationCallback;

    /**
     * Called by a preference panel fragment to finish itself.
     *
     * @param context Application Context
     * @param animationId An {@link android.graphics.drawable.AnimationDrawable} resource id
     */
    VectorAnimationController(Context context, int animationId) {
        mAnimatedVectorDrawableCompat = AnimatedVectorDrawableCompat.create(context, animationId);
        mAnimationCallback = new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                mAnimatedVectorDrawableCompat.start();
            }
        };
    }

    @Override
    public int getVideoWidth() {
        return mAnimatedVectorDrawableCompat.getIntrinsicWidth();
    }

    @Override
    public int getVideoHeight() {
        return mAnimatedVectorDrawableCompat.getIntrinsicHeight();
    }

    @Override
    public void pause() {
        mAnimatedVectorDrawableCompat.stop();
    }

    @Override
    public void start() {
        mAnimatedVectorDrawableCompat.start();
    }

    @Override
    public boolean isPlaying() {
        return mAnimatedVectorDrawableCompat.isRunning();
    }

    @Override
    public int getDuration() {
        // We can't get duration from AnimatedVectorDrawable, just return a non zero value.
        return 5000;
    }

    @Override
    public void attachView(TextureView video, View preview, View playButton) {
        mPreviewDrawable = preview.getForeground();
        video.setVisibility(View.GONE);
        updateViewStates(preview, playButton);
        preview.setOnClickListener(v -> updateViewStates(preview, playButton));
    }

    @Override
    public void release() {
        mAnimatedVectorDrawableCompat.stop();
        mAnimatedVectorDrawableCompat.clearAnimationCallbacks();
    }

    private void updateViewStates(View imageView, View playButton) {
        if (mAnimatedVectorDrawableCompat.isRunning()) {
            mAnimatedVectorDrawableCompat.stop();
            mAnimatedVectorDrawableCompat.clearAnimationCallbacks();
            playButton.setVisibility(View.VISIBLE);
            imageView.setForeground(mPreviewDrawable);
        } else {
            playButton.setVisibility(View.GONE);
            imageView.setForeground(mAnimatedVectorDrawableCompat);
            mAnimatedVectorDrawableCompat.start();
            mAnimatedVectorDrawableCompat.registerAnimationCallback(mAnimationCallback);
        }
    }
}
