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

package com.android.settings.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.wifitrackerlib.MergedCarrierEntry;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;

public class WifiPickerTrackerHelper implements LifecycleObserver {

    private static final String TAG = "WifiPickerTrackerHelper";

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;
    // Clock used for evaluating the age of scans
    private static final Clock ELAPSED_REALTIME_CLOCK = new SimpleClock(ZoneOffset.UTC) {
        @Override
        public long millis() {
            return SystemClock.elapsedRealtime();
        }
    };

    private WifiPickerTracker mWifiPickerTracker;
    // Worker thread used for WifiPickerTracker work
    private HandlerThread mWorkerThread;

    public WifiPickerTrackerHelper(@NonNull Lifecycle lifecycle, @NonNull Context context,
            @Nullable WifiPickerTracker.WifiPickerTrackerCallback listener) {
        if (lifecycle == null) {
            throw new IllegalArgumentException("lifecycle must be non-null.");
        }
        lifecycle.addObserver(this);
        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();

        mWifiPickerTracker = new WifiPickerTracker(lifecycle, context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                context.getSystemService(NetworkScoreManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                ELAPSED_REALTIME_CLOCK,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                listener);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mWorkerThread.quit();
    }

    public @NonNull WifiPickerTracker getWifiPickerTracker() {
        return mWifiPickerTracker;
    }

    public boolean setCarrierNetworkEnabled(boolean enable) {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null) {
            return false;
        }
        mergedCarrierEntry.setEnabled(enable);
        return true;
    }

    public boolean connectCarrierNetwork(@Nullable WifiEntry.ConnectCallback callback) {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null || !mergedCarrierEntry.canConnect()) {
            return false;
        }
        mergedCarrierEntry.connect(callback);
        return true;
    }

    @VisibleForTesting
    void setWifiPickerTracker(@NonNull WifiPickerTracker wifiPickerTracker) {
        mWifiPickerTracker = wifiPickerTracker;
    }

    @VisibleForTesting
    void setWorkerThread(@NonNull HandlerThread workerThread) {
        mWorkerThread = workerThread;
    }
}
