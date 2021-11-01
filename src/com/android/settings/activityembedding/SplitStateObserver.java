/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.activityembedding;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitInfo;

import java.util.List;

/** A lifecycle-aware observer listens to active split state. */
public class SplitStateObserver implements LifecycleObserver, Consumer<List<SplitInfo>> {

    private final Activity mActivity;
    private final boolean mListenOnce;
    private final SplitStateListener mListener;
    private final SplitController mSplitController;

    public SplitStateObserver(@NonNull Activity activity, boolean listenOnce,
            @NonNull SplitStateListener listener) {
        mActivity = activity;
        mListenOnce = listenOnce;
        mListener = listener;
        mSplitController = SplitController.getInstance();
    }

    /**
     * Start lifecycle event.
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mSplitController.addSplitListener(mActivity, ContextCompat.getMainExecutor(mActivity),
                this);
    }

    /**
     * Stop lifecycle event.
     */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mSplitController.removeSplitListener(this);
    }

    @Override
    public void accept(List<SplitInfo> splitInfos) {
        if (mListenOnce) {
            mSplitController.removeSplitListener(this);
        }
        mListener.onSplitInfoChanged(splitInfos);
    }

    /** This interface makes as class that it wants to listen to {@link SplitInfo} changes. */
    public interface SplitStateListener {

        /** Receive a set of split info change */
        void onSplitInfoChanged(List<SplitInfo> splitInfos);
    }
}
