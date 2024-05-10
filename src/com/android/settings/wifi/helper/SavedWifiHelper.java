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

import android.annotation.TestApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import com.android.wifitrackerlib.SavedNetworkTracker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for saved Wi-Fi networks tracker from WifiTrackerLib.
 */
public class SavedWifiHelper extends WifiTrackerBase {
    private static final String TAG = "SavedWifiHelper";

    private static final Object sInstanceLock = new Object();
    @TestApi
    @GuardedBy("sInstanceLock")
    private static Map<Context, SavedWifiHelper> sTestInstances;

    protected SavedNetworkTracker mSavedNetworkTracker;

    /**
     * Static method to create a SavedWifiHelper class.
     *
     * @param context   The Context this is associated with.
     * @param lifecycle The lifecycle this is associated with.
     * @return an instance of {@link SavedWifiHelper} object.
     */
    public static SavedWifiHelper getInstance(@NonNull Context context,
            @NonNull Lifecycle lifecycle) {
        synchronized (sInstanceLock) {
            if (sTestInstances != null && sTestInstances.containsKey(context)) {
                SavedWifiHelper testInstance = sTestInstances.get(context);
                Log.w(TAG, "The context owner use a test instance:" + testInstance);
                return testInstance;
            }
            return new SavedWifiHelper(context, lifecycle);
        }
    }

    /**
     * A convenience method to set pre-prepared instance or mock(SavedWifiHelper.class) for
     * testing.
     *
     * @param context  The Context this is associated with.
     * @param instance of {@link SavedWifiHelper} object.
     * @hide
     */
    @TestApi
    @VisibleForTesting
    public static void setTestInstance(@NonNull Context context, SavedWifiHelper instance) {
        synchronized (sInstanceLock) {
            if (sTestInstances == null) sTestInstances = new ConcurrentHashMap<>();
            Log.w(TAG, "Set a test instance by context:" + context);
            sTestInstances.put(context, instance);
        }
    }

    public SavedWifiHelper(@NonNull Context context, @NonNull Lifecycle lifecycle) {
        this(context, lifecycle, null);
    }

    @VisibleForTesting
    protected SavedWifiHelper(@NonNull Context context, @NonNull Lifecycle lifecycle,
            SavedNetworkTracker saveNetworkTracker) {
        super(lifecycle);
        mSavedNetworkTracker = (saveNetworkTracker != null) ? saveNetworkTracker
                : createSavedNetworkTracker(context, lifecycle);
    }

    @VisibleForTesting
    protected SavedNetworkTracker createSavedNetworkTracker(@NonNull Context context,
            @NonNull Lifecycle lifecycle) {
        return new SavedNetworkTracker(lifecycle, context.getApplicationContext(),
                context.getApplicationContext().getSystemService(WifiManager.class),
                context.getApplicationContext().getSystemService(ConnectivityManager.class),
                new Handler(Looper.getMainLooper()),
                getWorkerThreadHandler(),
                ELAPSED_REALTIME_CLOCK,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                null /* SavedNetworkTrackerCallback */);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public SavedNetworkTracker getSavedNetworkTracker() {
        return mSavedNetworkTracker;
    }

    /**
     * Returns true when the certificate is being used by a saved network or network suggestion.
     */
    public boolean isCertificateInUse(String certAlias) {
        return mSavedNetworkTracker.isCertificateRequired(certAlias);
    }

    /**
     * Returns a list of network names which is using the certificate alias.
     *
     * @return a list of network names.
     */
    public List<String> getCertificateNetworkNames(String certAlias) {
        return mSavedNetworkTracker.getCertificateRequesterNames(certAlias);
    }
}
