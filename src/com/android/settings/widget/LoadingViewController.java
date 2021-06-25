/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * A helper class that manages show/hide loading spinner.
 */
public class LoadingViewController {

    private static final long DELAY_SHOW_LOADING_CONTAINER_THRESHOLD_MS = 100L;

    public final Handler mFgHandler;
    public final View mLoadingView;
    public final View mContentView;

    public LoadingViewController(View loadingView, View contentView) {
        mLoadingView = loadingView;
        mContentView = contentView;
        mFgHandler = new Handler(Looper.getMainLooper());
    }

    private Runnable mShowLoadingContainerRunnable = new Runnable() {
        public void run() {
            handleLoadingContainer(false /* done */, false /* animate */);
        }
    };

    public void showContent(boolean animate) {
        // Cancel any pending task to show the loading animation and show the list of
        // apps directly.
        mFgHandler.removeCallbacks(mShowLoadingContainerRunnable);
        handleLoadingContainer(true /* show */, animate);
    }

    public void showLoadingViewDelayed() {
        mFgHandler.postDelayed(
                mShowLoadingContainerRunnable, DELAY_SHOW_LOADING_CONTAINER_THRESHOLD_MS);
    }

    public void handleLoadingContainer(boolean done, boolean animate) {
        handleLoadingContainer(mLoadingView, mContentView, done, animate);
    }

    /**
     * Show/hide loading view and content view.
     *
     * @param loading The loading spinner view
     * @param content The content view
     * @param done    If true, content is set visible and loading is set invisible.
     * @param animate Whether or not content/loading views should animate in/out.
     */
    public static void handleLoadingContainer(View loading, View content, boolean done,
            boolean animate) {
        setViewShown(loading, !done, animate);
        setViewShown(content, done, animate);
    }

    private static void setViewShown(final View view, boolean shown, boolean animate) {
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(),
                    shown ? android.R.anim.fade_in : android.R.anim.fade_out);
            if (shown) {
                view.setVisibility(View.VISIBLE);
            } else {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.INVISIBLE);
                    }
                });
            }
            view.startAnimation(animation);
        } else {
            view.clearAnimation();
            view.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
