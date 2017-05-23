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

import android.app.ActionBar;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class ActionBarShadowController implements LifecycleObserver, OnStart, OnStop {

    private ScrollChangeWatcher mScrollChangeWatcher;
    private RecyclerView mRecyclerView;
    private boolean isScrollWatcherAttached;

    public static ActionBarShadowController attachToRecyclerView(Activity activity,
            Lifecycle lifecycle, RecyclerView recyclerView) {
        return new ActionBarShadowController(activity, lifecycle, recyclerView);
    }

    private ActionBarShadowController(Activity activity, Lifecycle lifecycle,
            RecyclerView recyclerView) {
        mScrollChangeWatcher = new ScrollChangeWatcher(activity);
        mRecyclerView = recyclerView;
        attachScrollWatcher();
        lifecycle.addObserver(this);
    }

    @Override
    public void onStop() {
        detachScrollWatcher();
    }

    private void detachScrollWatcher() {
        mRecyclerView.removeOnScrollListener(mScrollChangeWatcher);
        isScrollWatcherAttached = false;
    }

    @Override
    public void onStart() {
        attachScrollWatcher();
    }

    private void attachScrollWatcher() {
        if (!isScrollWatcherAttached) {
            isScrollWatcherAttached = true;
            mRecyclerView.addOnScrollListener(mScrollChangeWatcher);
            mScrollChangeWatcher.updateDropShadow(mRecyclerView);
        }
    }

    /**
     * Update the drop shadow as the scrollable entity is scrolled.
     */
    private final class ScrollChangeWatcher extends RecyclerView.OnScrollListener {

        private Activity mActivity;

        public ScrollChangeWatcher(Activity activity) {
            mActivity = activity;
        }

        // RecyclerView scrolled.
        @Override
        public void onScrolled(RecyclerView view, int dx, int dy) {
            updateDropShadow(view);
        }

        public void updateDropShadow(View view) {
            final boolean shouldShowShadow = view.canScrollVertically(-1);
            final ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                actionBar.setElevation(shouldShowShadow ? 8 : 0);
            }
        }
    }

}
