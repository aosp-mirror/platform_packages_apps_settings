/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.wifi.helper;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Base class for the WifiTrackerLib related classes.
 */
public class WifiTrackerBase implements DefaultLifecycleObserver {
    private static final String TAG = "WifiTrackerBase";

    // Max age of tracked WifiEntries
    protected static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating Wi-Fi Tracker scans
    protected static final long SCAN_INTERVAL_MILLIS = 10_000;
    // Clock used for evaluating the age of scans
    protected static final Clock ELAPSED_REALTIME_CLOCK = new SimpleClock(ZoneOffset.UTC) {
        @Override
        public long millis() {
            return SystemClock.elapsedRealtime();
        }
    };

    @VisibleForTesting
    protected HandlerThread mWorkerThread;

    public WifiTrackerBase(@NonNull Lifecycle lifecycle) {
        this(lifecycle, null /* handlerThread */);
    }

    @VisibleForTesting
    protected WifiTrackerBase(@NonNull Lifecycle lifecycle, HandlerThread handlerThread) {
        lifecycle.addObserver(this);
        mWorkerThread = (handlerThread != null) ? handlerThread :
                new HandlerThread(getTag()
                        + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                        Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
    }

    protected String getTag() {
        return TAG;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mWorkerThread.quit();
    }

    /** Returns the worker thread handler. */
    public Handler getWorkerThreadHandler() {
        return mWorkerThread.getThreadHandler();
    }
}
