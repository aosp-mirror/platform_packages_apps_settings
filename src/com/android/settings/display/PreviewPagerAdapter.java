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
package com.android.settings.display;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;

/**
 * A PagerAdapter used by PreviewSeekBarPreferenceFragment that for showing multiple preview screen
 * regarding a single setting and allowing the user to swipe across them.
 */
public class PreviewPagerAdapter extends PagerAdapter {

    /** Duration to use when cross-fading between previews. */
    private static final long CROSS_FADE_DURATION_MS = 400;

    /** Interpolator to use when cross-fading between previews. */
    private static final Interpolator FADE_IN_INTERPOLATOR = new DecelerateInterpolator();

    /** Interpolator to use when cross-fading between previews. */
    private static final Interpolator FADE_OUT_INTERPOLATOR = new AccelerateInterpolator();

    private FrameLayout[] mPreviewFrames;

    private boolean mIsLayoutRtl;

    private Runnable mAnimationEndAction;

    private int mAnimationCounter;

    private boolean[][] mViewStubInflated;

    public PreviewPagerAdapter(Context context, boolean isLayoutRtl,
            int[] previewSampleResIds, Configuration[] configurations) {
        mIsLayoutRtl = isLayoutRtl;
        mPreviewFrames = new FrameLayout[previewSampleResIds.length];
        mViewStubInflated = new boolean[previewSampleResIds.length][configurations.length];

        for (int i = 0; i < previewSampleResIds.length; ++i) {
            int p = mIsLayoutRtl ? previewSampleResIds.length - 1 - i : i;
            mPreviewFrames[p] = new FrameLayout(context);
            mPreviewFrames[p].setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            mPreviewFrames[p].setClipToPadding(true);
            mPreviewFrames[p].setClipChildren(true);
            for (int j = 0; j < configurations.length; ++j) {
                // Create a new configuration for the specified value. It won't
                // have any theme set, so manually apply the current theme.
                final Context configContext = context.createConfigurationContext(configurations[j]);
                configContext.getTheme().setTo(context.getTheme());

                final ViewStub sampleViewStub = new ViewStub(configContext);
                sampleViewStub.setLayoutResource(previewSampleResIds[i]);
                final int fi = i, fj = j;
                sampleViewStub.setOnInflateListener((stub, inflated) -> {
                    inflated.setVisibility(stub.getVisibility());
                    mViewStubInflated[fi][fj] = true;
                });

                mPreviewFrames[p].addView(sampleViewStub);
            }
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return mPreviewFrames.length;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mPreviewFrames[position]);
        return mPreviewFrames[position];
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }

    boolean isAnimating() {
        return mAnimationCounter > 0;
    }

    void setAnimationEndAction(Runnable action) {
        mAnimationEndAction = action;
    }

    void setPreviewLayer(int newLayerIndex, int currentLayerIndex, int currentFrameIndex,
            final boolean animate) {
        for (FrameLayout previewFrame : mPreviewFrames) {
            if (currentLayerIndex >= 0) {
                final View lastLayer = previewFrame.getChildAt(currentLayerIndex);
                if (mViewStubInflated[currentFrameIndex][currentLayerIndex]) {
                    // Explicitly set to INVISIBLE only when the stub has
                    // already been inflated.
                    if (previewFrame == mPreviewFrames[currentFrameIndex]) {
                        setVisibility(lastLayer, View.INVISIBLE, animate);
                    } else {
                        setVisibility(lastLayer, View.INVISIBLE, false);
                    }
                }
            }

            // Set next layer visible, as well as inflate necessary views.
            View nextLayer = previewFrame.getChildAt(newLayerIndex);
            if (previewFrame == mPreviewFrames[currentFrameIndex]) {
                // Inflate immediately if the stub has not yet been inflated.
                if (!mViewStubInflated[currentFrameIndex][newLayerIndex]) {
                    nextLayer = ((ViewStub) nextLayer).inflate();
                    nextLayer.setAlpha(0.0f);
                }
                setVisibility(nextLayer, View.VISIBLE, animate);
            } else {
                setVisibility(nextLayer, View.VISIBLE, false);
            }
        }
    }

    private void setVisibility(final View view, final int visibility, boolean animate) {
        final float alpha = (visibility == View.VISIBLE ? 1.0f : 0.0f);
        if (!animate) {
            view.setAlpha(alpha);
            view.setVisibility(visibility);
        } else {
            final Interpolator interpolator = (visibility == View.VISIBLE ? FADE_IN_INTERPOLATOR
                    : FADE_OUT_INTERPOLATOR);
            if (visibility == View.VISIBLE) {
                // Fade in animation.
                view.animate()
                        .alpha(alpha)
                        .setInterpolator(FADE_IN_INTERPOLATOR)
                        .setDuration(CROSS_FADE_DURATION_MS)
                        .setListener(new PreviewFrameAnimatorListener())
                        .withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                view.setVisibility(visibility);
                            }
                        });
            } else {
                // Fade out animation.
                view.animate()
                        .alpha(alpha)
                        .setInterpolator(FADE_OUT_INTERPOLATOR)
                        .setDuration(CROSS_FADE_DURATION_MS)
                        .setListener(new PreviewFrameAnimatorListener())
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.setVisibility(visibility);
                            }
                        });
            }
        }
    }

    private void runAnimationEndAction() {
        if (mAnimationEndAction != null && !isAnimating()) {
            mAnimationEndAction.run();
            mAnimationEndAction = null;
        }
    }

    private class PreviewFrameAnimatorListener implements AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation) {
            mAnimationCounter++;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mAnimationCounter--;
            runAnimationEndAction();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // Empty method.
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            // Empty method.
        }
    }
}
