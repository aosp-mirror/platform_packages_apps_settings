/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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

    public PreviewPagerAdapter(Context context, boolean isLayoutRtl,
            int[] previewSampleResIds, Configuration[] configurations) {
        mIsLayoutRtl = isLayoutRtl;
        mPreviewFrames = new FrameLayout[previewSampleResIds.length];

        for (int i = 0; i < previewSampleResIds.length; ++i) {
            int p = mIsLayoutRtl ? previewSampleResIds.length - 1 - i : i;
            mPreviewFrames[p] = new FrameLayout(context);
            mPreviewFrames[p].setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            for (Configuration configuration : configurations) {
                // Create a new configuration for the specified value. It won't
                // have any theme set, so manually apply the current theme.
                final Context configContext = context.createConfigurationContext(configuration);
                configContext.setTheme(context.getThemeResId());

                final LayoutInflater configInflater = LayoutInflater.from(configContext);
                final View sampleView = configInflater.inflate(previewSampleResIds[i],
                        mPreviewFrames[p], false);
                sampleView.setAlpha(0);
                sampleView.setVisibility(View.INVISIBLE);
                mPreviewFrames[p].addView(sampleView);
            }
        }
    }

    @Override
    public void destroyItem (ViewGroup container, int position, Object object) {
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

    void setPreviewLayer(int newIndex, int currentIndex, int currentItem, boolean animate) {
        for (FrameLayout previewFrame : mPreviewFrames) {
            if (currentIndex >= 0) {
                final View lastLayer = previewFrame.getChildAt(currentIndex);
                if (animate && previewFrame == mPreviewFrames[currentItem]) {
                    lastLayer.animate()
                            .alpha(0)
                            .setInterpolator(FADE_OUT_INTERPOLATOR)
                            .setDuration(CROSS_FADE_DURATION_MS)
                            .setListener(new PreviewFrameAnimatorListener())
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    lastLayer.setVisibility(View.INVISIBLE);
                                }
                            });
                } else {
                    lastLayer.setAlpha(0);
                    lastLayer.setVisibility(View.INVISIBLE);
                }
            }

            final View nextLayer = previewFrame.getChildAt(newIndex);
            if (animate && previewFrame == mPreviewFrames[currentItem]) {
                nextLayer.animate()
                        .alpha(1)
                        .setInterpolator(FADE_IN_INTERPOLATOR)
                        .setDuration(CROSS_FADE_DURATION_MS)
                        .setListener(new PreviewFrameAnimatorListener())
                        .withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                nextLayer.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                nextLayer.setVisibility(View.VISIBLE);
                nextLayer.setAlpha(1);
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
