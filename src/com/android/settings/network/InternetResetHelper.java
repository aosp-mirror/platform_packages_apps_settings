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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.connectivity.ConnectivitySubsystemsRecoveryManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to restart connectivity for all requested subsystems.
 */
public class InternetResetHelper implements LifecycleObserver,
        ConnectivitySubsystemsRecoveryManager.RecoveryStatusCallback {

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
        @WorkerThread
        public void onReceive(Context context, Intent intent) {
            if (intent != null && TextUtils.equals(intent.getAction(),
                    WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                updateWifiStateChange();
            }
        }
    };

    protected ConnectivitySubsystemsRecoveryManager mConnectivitySubsystemsRecoveryManager;
    protected HandlerThread mWorkerThread;
    protected boolean mIsRecoveryReady;
    protected boolean mIsWifiReady;
    protected HandlerInjector mHandlerInjector;
    protected final Runnable mResumeRunnable = () -> {
        resumePreferences();
    };
    protected final Runnable mTimeoutRunnable = () -> {
        mIsRecoveryReady = true;
        mIsWifiReady = true;
        resumePreferences();
    };

    public InternetResetHelper(Context context, Lifecycle lifecycle) {
        mContext = context;
        mHandlerInjector = new HandlerInjector(context);
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        mConnectivitySubsystemsRecoveryManager = new ConnectivitySubsystemsRecoveryManager(
                mContext, mWorkerThread.getThreadHandler());

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_RESUME) */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE) */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY) */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mHandlerInjector.removeCallbacks(mResumeRunnable);
        mHandlerInjector.removeCallbacks(mTimeoutRunnable);
        mWorkerThread.quit();
    }

    @Override
    @WorkerThread
    public void onSubsystemRestartOperationBegin() {
        Log.d(TAG, "The connectivity subsystem is starting for recovery.");
    }

    @Override
    @WorkerThread
    public void onSubsystemRestartOperationEnd() {
        Log.d(TAG, "The connectivity subsystem is done for recovery.");
        if (!mIsRecoveryReady) {
            mIsRecoveryReady = true;
            mHandlerInjector.postDelayed(mResumeRunnable, 0 /* delayMillis */);
        }
    }

    @VisibleForTesting
    @WorkerThread
    protected void updateWifiStateChange() {
        if (!mIsWifiReady && mWifiManager.isWifiEnabled()) {
            Log.d(TAG, "The Wi-Fi subsystem is done for recovery.");
            mIsWifiReady = true;
            mHandlerInjector.postDelayed(mResumeRunnable, 0 /* delayMillis */);
        }
    }

    /**
     * Sets the resetting preference.
     */
    @UiThread
    public void setResettingPreference(Preference preference) {
        mResettingPreference = preference;
    }

    /**
     * Sets the mobile network controller.
     */
    @UiThread
    public void setMobileNetworkController(NetworkMobileProviderController controller) {
        mMobileNetworkController = controller;
    }

    /**
     * Sets the Wi-Fi toggle preference.
     */
    @UiThread
    public void setWifiTogglePreference(Preference preference) {
        mWifiTogglePreferences = preference;
    }

    /**
     * Adds the Wi-Fi network preference.
     */
    @UiThread
    public void addWifiNetworkPreference(PreferenceCategory preference) {
        if (preference != null) {
            mWifiNetworkPreferences.add(preference);
        }
    }

    @UiThread
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

    @UiThread
    protected void resumePreferences() {
        if (mIsRecoveryReady && mMobileNetworkController != null) {
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
        if (mIsRecoveryReady && mIsWifiReady) {
            mHandlerInjector.removeCallbacks(mTimeoutRunnable);
            if (mResettingPreference != null) {
                Log.d(TAG, "Resume the Resetting preference");
                mResettingPreference.setVisible(false);
            }
        }
    }

    /**
     * Restart connectivity for all requested subsystems.
     */
    @UiThread
    public void restart() {
        if (!mConnectivitySubsystemsRecoveryManager.isRecoveryAvailable()) {
            Log.e(TAG, "The connectivity subsystem is not available to restart.");
            return;
        }

        Log.d(TAG, "The connectivity subsystem is restarting for recovery.");
        suspendPreferences();
        mIsRecoveryReady = false;
        mIsWifiReady = !mWifiManager.isWifiEnabled();
        mHandlerInjector.postDelayed(mTimeoutRunnable, RESTART_TIMEOUT_MS);
        mConnectivitySubsystemsRecoveryManager.triggerSubsystemRestart(null /* reason */, this);
    }

    /**
     * Wrapper for testing compatibility.
     */
    @VisibleForTesting
    static class HandlerInjector {
        protected final Handler mHandler;

        HandlerInjector(Context context) {
            mHandler = context.getMainThreadHandler();
        }

        public void postDelayed(Runnable runnable, long delayMillis) {
            mHandler.postDelayed(runnable, delayMillis);
        }

        public void removeCallbacks(Runnable runnable) {
            mHandler.removeCallbacks(runnable);
        }
    }
}
