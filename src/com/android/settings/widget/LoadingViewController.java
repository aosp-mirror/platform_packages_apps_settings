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

import androidx.annotation.Nullable;

/**
 * A helper class that manages show/hide loading spinner, content view and empty view (optional).
 */
public class LoadingViewController {

    private static final long DELAY_SHOW_LOADING_CONTAINER_THRESHOLD_MS = 100L;

    private final Handler mFgHandler;
    private final View mLoadingView;
    private final View mContentView;
    private final View mEmptyView;

    public LoadingViewController(View loadingView, View contentView) {
        this(loadingView, contentView, null /* emptyView*/);
    }

    public LoadingViewController(View loadingView, View contentView, @Nullable View emptyView) {
        mLoadingView = loadingView;
        mContentView = contentView;
        mEmptyView = emptyView;
        mFgHandler = new Handler(Looper.getMainLooper());
    }

    private Runnable mShowLoadingContainerRunnable = new Runnable() {
        public void run() {
            showLoadingView();
        }
    };

    /**
     *  Shows content view and hides loading view & empty view.
     */
    public void showContent(boolean animate) {
        // Cancel any pending task to show the loading animation and show the list of
        // apps directly.
        mFgHandler.removeCallbacks(mShowLoadingContainerRunnable);
        handleLoadingContainer(true /* showContent */, false /* showEmpty*/, animate);
    }

    /**
     *  Shows empty view and hides loading view & content view.
     */
    public void showEmpty(boolean animate) {
        if (mEmptyView == null) {
            return;
        }

        // Cancel any pending task to show the loading animation and show the list of
        // apps directly.
        mFgHandler.removeCallbacks(mShowLoadingContainerRunnable);
        handleLoadingContainer(false /* showContent */, true /* showEmpty */, animate);
    }

    /**
     *  Shows loading view and hides content view & empty view.
     */
    public void showLoadingView() {
        handleLoadingContainer(false /* showContent */, false /* showEmpty */, false /* animate */);
    }

    public void showLoadingViewDelayed() {
        mFgHandler.postDelayed(
                mShowLoadingContainerRunnable, DELAY_SHOW_LOADING_CONTAINER_THRESHOLD_MS);
    }

    private void handleLoadingContainer(boolean showContent, boolean showEmpty, boolean animate) {
        handleLoadingContainer(mLoadingView, mContentView, mEmptyView,
                showContent, showEmpty, animate);
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

    /**
     * Show/hide loading view and content view and empty view.
     *
     * @param loading The loading spinner view
     * @param content The content view
     * @param empty The empty view shows no item summary to users.
     * @param showContent    If true, content is set visible and loading is set invisible.
     * @param showEmpty    If true, empty is set visible and loading is set invisible.
     * @param animate Whether or not content/loading views should animate in/out.
     */
    public static void handleLoadingContainer(View loading, View content, View empty,
            boolean showContent, boolean showEmpty, boolean animate) {
        if (empty != null) {
            setViewShown(empty, showEmpty, animate);
        }
        setViewShown(content, showContent, animate);
        setViewShown(loading, !showContent && !showEmpty, animate);
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
