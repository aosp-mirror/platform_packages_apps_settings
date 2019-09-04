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

package com.android.settings.dashboard;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Control ui blocker data and check whether it is finished
 *
 * @see BasePreferenceController.UiBlocker
 * @see BasePreferenceController.UiBlockListener
 */
public class UiBlockerController {
    private static final String TAG = "UiBlockerController";
    private static final int TIMEOUT_MILLIS = 500;

    private CountDownLatch mCountDownLatch;
    private boolean mBlockerFinished;
    private Set<String> mKeys;
    private long mTimeoutMillis;

    public UiBlockerController(@NonNull List<String> keys) {
        this(keys, TIMEOUT_MILLIS);
    }

    public UiBlockerController(@NonNull List<String> keys, long timeout) {
        mCountDownLatch = new CountDownLatch(keys.size());
        mBlockerFinished = keys.isEmpty();
        mKeys = new HashSet<>(keys);
        mTimeoutMillis = timeout;
    }

    /**
     * Start background thread, it will invoke {@code finishRunnable} if any condition is met
     *
     * 1. Waiting time exceeds {@link #mTimeoutMillis}
     * 2. All background work that associated with {@link #mCountDownLatch} is finished
     */
    public boolean start(Runnable finishRunnable) {
        if (mKeys.isEmpty()) {
            // Don't need to run finishRunnable because it doesn't start
            return false;
        }
        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                mCountDownLatch.await(mTimeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.w(TAG, "interrupted");
            }
            mBlockerFinished = true;
            ThreadUtils.postOnMainThread(finishRunnable);
        });

        return true;
    }

    /**
     * Return {@code true} if all work finished
     */
    public boolean isBlockerFinished() {
        return mBlockerFinished;
    }

    /**
     * Count down latch by {@code key}. It only count down 1 time if same key count down multiple
     * times.
     */
    public boolean countDown(String key) {
        if (mKeys.remove(key)) {
            mCountDownLatch.countDown();
            return true;
        }

        return false;
    }
}
