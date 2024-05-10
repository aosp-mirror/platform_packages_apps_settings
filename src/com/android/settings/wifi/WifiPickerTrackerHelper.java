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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.overlay.FeatureFactory;
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

    protected WifiPickerTracker mWifiPickerTracker;
    // Worker thread used for WifiPickerTracker work
    protected HandlerThread mWorkerThread;

    protected final WifiManager mWifiManager;
    protected final CarrierConfigCache mCarrierConfigCache;

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

        mWifiPickerTracker = FeatureFactory.getFeatureFactory()
                .getWifiTrackerLibProvider()
                .createWifiPickerTracker(lifecycle, context,
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                ELAPSED_REALTIME_CLOCK,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                listener);

        mWifiManager = context.getSystemService(WifiManager.class);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
    }

    /** @OnLifecycleEvent(ON_DESTROY) */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mWorkerThread.quit();
    }

    /** Return the WifiPickerTracker class */
    public @NonNull WifiPickerTracker getWifiPickerTracker() {
        return mWifiPickerTracker;
    }

    /** Return the enabled/disabled state of the carrier network provision */
    public boolean isCarrierNetworkProvisionEnabled(int subId) {
        final PersistableBundle config = mCarrierConfigCache.getConfigForSubId(subId);
        if (config == null) {
            Log.e(TAG, "Could not get carrier config, subId:" + subId);
            return false;
        }
        final boolean enabled = config.getBoolean(
                CarrierConfigManager.KEY_CARRIER_PROVISIONS_WIFI_MERGED_NETWORKS_BOOL);
        Log.i(TAG, "isCarrierNetworkProvisionEnabled:" + enabled);
        return enabled;
    }

    /** Return the enabled/disabled state of the carrier network */
    public boolean isCarrierNetworkEnabled() {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null) {
            Log.e(TAG, "Failed to get MergedCarrierEntry to query enabled status");
            return false;
        }
        final boolean isCarrierNetworkEnabled = mergedCarrierEntry.isEnabled();
        Log.i(TAG, "isCarrierNetworkEnabled:" + isCarrierNetworkEnabled);
        return isCarrierNetworkEnabled;
    }

    /** Enables/disables the carrier network */
    public void setCarrierNetworkEnabled(boolean enabled) {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null) {
            Log.e(TAG, "Unable to get MergedCarrierEntry to set enabled status");
            return;
        }
        Log.i(TAG, "setCarrierNetworkEnabled:" + enabled);
        mergedCarrierEntry.setEnabled(enabled);
    }

    /** Connect to the carrier network */
    public boolean connectCarrierNetwork(@Nullable WifiEntry.ConnectCallback callback) {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null || !mergedCarrierEntry.canConnect()) {
            return false;
        }
        mergedCarrierEntry.connect(callback);
        return true;
    }

    /** Confirms connection of the carrier network connected with the internet access */
    public boolean isCarrierNetworkActive() {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        return (mergedCarrierEntry != null && mergedCarrierEntry.isDefaultNetwork());
    }

    /** Return the carrier network ssid */
    public String getCarrierNetworkSsid() {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null) {
            return null;
        }
        return mergedCarrierEntry.getSsid();
    }

    /** Return the carrier network level */
    public int getCarrierNetworkLevel() {
        final MergedCarrierEntry mergedCarrierEntry = mWifiPickerTracker.getMergedCarrierEntry();
        if (mergedCarrierEntry == null) return WifiEntry.WIFI_LEVEL_MIN;

        int level = mergedCarrierEntry.getLevel();
        // To avoid icons not found with WIFI_LEVEL_UNREACHABLE(-1), use WIFI_LEVEL_MIN(0) instead.
        if (level < WifiEntry.WIFI_LEVEL_MIN) level = WifiEntry.WIFI_LEVEL_MIN;
        return level;
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
