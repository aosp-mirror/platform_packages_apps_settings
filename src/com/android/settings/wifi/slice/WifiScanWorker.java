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

package com.android.settings.wifi.slice;

import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.utils.ThreadUtils;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.WifiEntryCallback;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link SliceBackgroundWorker} for Wi-Fi, used by {@link WifiSlice}.
 */
public class WifiScanWorker extends SliceBackgroundWorker<WifiSliceItem> implements
        WifiPickerTracker.WifiPickerTrackerCallback, LifecycleOwner, WifiEntryCallback {

    private static final String TAG = "WifiScanWorker";

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting
    final LifecycleRegistry mLifecycleRegistry;
    @VisibleForTesting
    WifiPickerTracker mWifiPickerTracker;
    // Worker thread used for WifiPickerTracker work
    private final HandlerThread mWorkerThread;

    public WifiScanWorker(Context context, Uri uri) {
        super(context, uri);

        mLifecycleRegistry = new LifecycleRegistry(this);

        mWorkerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        mWifiPickerTracker = new WifiPickerTracker(getLifecycle(), context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                context.getSystemService(NetworkScoreManager.class),
                ThreadUtils.getUiThreadHandler(),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                this);

        mLifecycleRegistry.markState(Lifecycle.State.INITIALIZED);
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);
    }

    @Override
    protected void onSlicePinned() {
        mLifecycleRegistry.markState(Lifecycle.State.STARTED);
        mLifecycleRegistry.markState(Lifecycle.State.RESUMED);
        updateResults();
    }

    @Override
    protected void onSliceUnpinned() {
        mLifecycleRegistry.markState(Lifecycle.State.STARTED);
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);
    }

    @Override
    public void close() {
        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED);
        mWorkerThread.quit();
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged() {
        notifySliceChange();
    }

    /**
     * Update the results when data changes
     */
    @Override
    public void onWifiEntriesChanged() {
        updateResults();
    }

    /**
     * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
     * the WifiEntry getter methods.
     */
    @Override
    public void onUpdated() {
        updateResults();
    }

    protected int getApRowCount() {
        return DEFAULT_EXPANDED_ROW_COUNT;
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        // Do nothing.
    }

    @Override
    public void onNumSavedNetworksChanged() {
        // Do nothing.
    }

    /**
     * To get the WifiEntry of key.
     */
    public WifiEntry getWifiEntry(String key) {
        // Get specified WifiEntry.
        WifiEntry keyWifiEntry = null;
        final WifiEntry connectedWifiEntry = mWifiPickerTracker.getConnectedWifiEntry();
        if (connectedWifiEntry != null && TextUtils.equals(key, connectedWifiEntry.getKey())) {
            keyWifiEntry = connectedWifiEntry;
        } else {
            for (WifiEntry wifiEntry : mWifiPickerTracker.getWifiEntries()) {
                if (TextUtils.equals(key, wifiEntry.getKey())) {
                    keyWifiEntry = wifiEntry;
                    break;
                }
            }
        }
        return keyWifiEntry;
    }

    @VisibleForTesting
    void updateResults() {
        if (mWifiPickerTracker.getWifiState() != WifiManager.WIFI_STATE_ENABLED
                || mLifecycleRegistry.getCurrentState() != Lifecycle.State.RESUMED) {
            super.updateResults(null);
            return;
        }

        final List<WifiSliceItem> resultList = new ArrayList<>();
        final WifiEntry connectedWifiEntry = mWifiPickerTracker.getConnectedWifiEntry();
        if (connectedWifiEntry != null) {
            connectedWifiEntry.setListener(this);
            resultList.add(new WifiSliceItem(getContext(), connectedWifiEntry));
        }
        for (WifiEntry wifiEntry : mWifiPickerTracker.getWifiEntries()) {
            if (resultList.size() >= getApRowCount()) {
                break;
            }
            if (wifiEntry.getLevel() != WifiEntry.WIFI_LEVEL_UNREACHABLE) {
                wifiEntry.setListener(this);
                resultList.add(new WifiSliceItem(getContext(), wifiEntry));
            }
        }
        super.updateResults(resultList);
    }
}
