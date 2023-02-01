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

package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.connectivity.ConnectivitySubsystemsRecoveryManager;
import com.android.settingslib.utils.HandlerInjector;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to restart connectivity for all requested subsystems.
 */
public class InternetResetHelper implements LifecycleObserver {

    protected static final String TAG = "InternetResetHelper";
    public static final long RESTART_TIMEOUT_MS = 15_000; // 15 seconds

    protected final Context mContext;
    protected Preference mResettingPreference;
    protected NetworkMobileProviderController mMobileNetworkController;
    protected Preference mWifiTogglePreferences;
    protected List<PreferenceCategory> mWifiNetworkPreferences =
            new ArrayList<PreferenceCategory>();

    protected final WifiManager mWifiManager;
    protected final IntentFilter mWifiStateFilter;
    protected final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiStateChange();
        }
    };

    protected RecoveryWorker mRecoveryWorker;
    protected boolean mIsWifiReady = true;
    protected HandlerInjector mHandlerInjector;
    protected final Runnable mTimeoutRunnable = () -> {
        Log.w(TAG, "Resume preferences due to connectivity subsystems recovery timed out.");
        mRecoveryWorker.clearRecovering();
        mIsWifiReady = true;
        resumePreferences();
    };

    public InternetResetHelper(Context context, Lifecycle lifecycle,
            NetworkMobileProviderController mobileNetworkController,
            Preference wifiTogglePreferences,
            PreferenceCategory connectedWifiEntryPreferenceCategory,
            PreferenceCategory firstWifiEntryPreferenceCategory,
            PreferenceCategory wifiEntryPreferenceCategory,
            Preference resettingPreference) {
        mContext = context;
        mMobileNetworkController = mobileNetworkController;
        mWifiTogglePreferences = wifiTogglePreferences;
        mWifiNetworkPreferences.add(connectedWifiEntryPreferenceCategory);
        mWifiNetworkPreferences.add(firstWifiEntryPreferenceCategory);
        mWifiNetworkPreferences.add(wifiEntryPreferenceCategory);
        mResettingPreference = resettingPreference;

        mHandlerInjector = new HandlerInjector(context.getMainThreadHandler());
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mRecoveryWorker = RecoveryWorker.getInstance(mContext, this);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_RESUME) */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE) */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY) */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mHandlerInjector.removeCallbacks(mTimeoutRunnable);
    }

    @VisibleForTesting
    protected void updateWifiStateChange() {
        if (!mIsWifiReady && mWifiManager.isWifiEnabled()) {
            Log.d(TAG, "The Wi-Fi subsystem is done for recovery.");
            mIsWifiReady = true;
            resumePreferences();
        }
    }

    protected void suspendPreferences() {
        Log.d(TAG, "Suspend the subsystem preferences");
        if (mMobileNetworkController != null) {
            mMobileNetworkController.hidePreference(true /* hide */, true /* immediately */);
        }
        if (mWifiTogglePreferences != null) {
            mWifiTogglePreferences.setVisible(false);
        }
        for (PreferenceCategory pref : mWifiNetworkPreferences) {
            pref.removeAll();
            pref.setVisible(false);
        }
        if (mResettingPreference != null) {
            mResettingPreference.setVisible(true);
        }
    }

    protected void resumePreferences() {
        boolean isRecoveryReady = !mRecoveryWorker.isRecovering();
        if (isRecoveryReady && mMobileNetworkController != null) {
            Log.d(TAG, "Resume the Mobile Network controller");
            mMobileNetworkController.hidePreference(false /* hide */, true /* immediately */);
        }
        if (mIsWifiReady && mWifiTogglePreferences != null) {
            Log.d(TAG, "Resume the Wi-Fi preferences");
            mWifiTogglePreferences.setVisible(true);
            for (PreferenceCategory pref : mWifiNetworkPreferences) {
                pref.setVisible(true);
            }
        }
        if (isRecoveryReady && mIsWifiReady) {
            mHandlerInjector.removeCallbacks(mTimeoutRunnable);
            if (mResettingPreference != null) {
                Log.d(TAG, "Resume the Resetting preference");
                mResettingPreference.setVisible(false);
            }
        }
    }

    protected void showResettingAndSendTimeoutChecks() {
        suspendPreferences();
        mHandlerInjector.postDelayed(mTimeoutRunnable, RESTART_TIMEOUT_MS);
    }

    /** Restart connectivity for all requested subsystems. */
    public void restart() {
        if (!mRecoveryWorker.isRecoveryAvailable()) {
            Log.e(TAG, "The connectivity subsystem is not available to restart.");
            return;
        }
        showResettingAndSendTimeoutChecks();
        mIsWifiReady = !mWifiManager.isWifiEnabled();
        mRecoveryWorker.triggerRestart();
    }

    /** Check if the connectivity subsystem is under recovering. */
    public void checkRecovering() {
        if (!mRecoveryWorker.isRecovering()) return;
        mIsWifiReady = false;
        showResettingAndSendTimeoutChecks();
    }

    /**
     * This is a singleton class for ConnectivitySubsystemsRecoveryManager worker.
     */
    @VisibleForTesting
    public static class RecoveryWorker implements
            ConnectivitySubsystemsRecoveryManager.RecoveryStatusCallback {
        private static final String TAG = "RecoveryWorker";
        private static RecoveryWorker sInstance;
        private static WeakReference<InternetResetHelper> sCallback;
        private static ConnectivitySubsystemsRecoveryManager sRecoveryManager;
        private static boolean sIsRecovering;

        /**
         * Create a singleton class for ConnectivitySubsystemsRecoveryManager.
         *
         * @param context  The context to use for the content resolver.
         * @param callback The callback of {@link InternetResetHelper} object.
         * @return an instance of {@link RecoveryWorker} object.
         */
        public static RecoveryWorker getInstance(Context context, InternetResetHelper callback) {
            sCallback = new WeakReference<>(callback);
            if (sInstance != null) return sInstance;

            sInstance = new RecoveryWorker();
            Context appContext = context.getApplicationContext();
            sRecoveryManager = new ConnectivitySubsystemsRecoveryManager(appContext,
                    appContext.getMainThreadHandler());
            return sInstance;
        }

        /** Returns true, If the subsystem service is recovering. */
        public boolean isRecovering() {
            return sIsRecovering;
        }

        /** Clear the recovering flag. */
        public void clearRecovering() {
            sIsRecovering = false;
        }

        /** Returns true, If the subsystem service is recovery available. */
        public boolean isRecoveryAvailable() {
            return sRecoveryManager.isRecoveryAvailable();
        }

        /** Trigger connectivity recovery for all requested technologies. */
        public boolean triggerRestart() {
            if (!isRecoveryAvailable()) {
                Log.e(TAG, "The connectivity subsystem is not available to restart.");
                return false;
            }
            sIsRecovering = true;
            sRecoveryManager.triggerSubsystemRestart(null /* reason */, sInstance);
            Log.d(TAG, "The connectivity subsystem is restarting for recovery.");
            return true;
        }

        @Override
        public void onSubsystemRestartOperationBegin() {
            Log.d(TAG, "The connectivity subsystem is starting for recovery.");
            sIsRecovering = true;
        }

        @Override
        public void onSubsystemRestartOperationEnd() {
            Log.d(TAG, "The connectivity subsystem is done for recovery.");
            sIsRecovering = false;
            InternetResetHelper callback = sCallback.get();
            if (callback == null) return;
            callback.resumePreferences();
        }
    }
}
