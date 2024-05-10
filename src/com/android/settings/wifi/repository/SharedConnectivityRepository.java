/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.repository;

import android.app.PendingIntent;
import android.content.Context;
import android.net.wifi.sharedconnectivity.app.HotspotNetwork;
import android.net.wifi.sharedconnectivity.app.HotspotNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.KnownNetwork;
import android.net.wifi.sharedconnectivity.app.KnownNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityClientCallback;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityManager;
import android.net.wifi.sharedconnectivity.app.SharedConnectivitySettingsState;
import android.os.HandlerThread;
import android.provider.DeviceConfig;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.overlay.FeatureFactory;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Shared Connectivity Repository for {@link SharedConnectivityManager}
 */
public class SharedConnectivityRepository {
    private static final String TAG = "SharedConnectivityRepository";
    private static final String DEVICE_CONFIG_NAMESPACE = "wifi";
    private static final String DEVICE_CONFIG_KEY = "shared_connectivity_enabled";

    private Context mAppContext;
    private SharedConnectivityManager mManager;
    private ClientCallback mClientCallback = new ClientCallback();
    private HandlerThread mWorkerThread = new HandlerThread(TAG);
    private Executor mWorkerExecutor = cmd -> mWorkerThread.getThreadHandler().post(cmd);
    private Runnable mLaunchSettingsRunnable = () -> handleLaunchSettings();
    @VisibleForTesting
    MutableLiveData<SharedConnectivitySettingsState> mSettingsState = new MutableLiveData<>();

    public SharedConnectivityRepository(@NonNull Context appContext) {
        this(appContext, isDeviceConfigEnabled());
    }

    @VisibleForTesting
    SharedConnectivityRepository(@NonNull Context appContext, boolean isConfigEnabled) {
        mAppContext = appContext;
        if (!isConfigEnabled) {
            return;
        }
        mManager = mAppContext.getSystemService(SharedConnectivityManager.class);
        if (mManager == null) {
            Log.w(TAG, "Failed to get SharedConnectivityManager");
            return;
        }
        mWorkerThread.start();
        mManager.registerCallback(mWorkerExecutor, mClientCallback);
    }

    /**
     * Return whether Wi-Fi Shared Connectivity service is available or not.
     *
     * @return {@code true} if Wi-Fi Shared Connectivity service is available
     */
    public boolean isServiceAvailable() {
        return mManager != null;
    }

    /**
     * Gets SharedConnectivitySettingsState LiveData
     */
    public LiveData<SharedConnectivitySettingsState> getSettingsState() {
        return mSettingsState;
    }

    /**
     * Launch Instant Hotspot Settings
     */
    public void launchSettings() {
        mWorkerExecutor.execute(mLaunchSettingsRunnable);
    }

    @WorkerThread
    @VisibleForTesting
    void handleLaunchSettings() {
        if (mManager == null) {
            return;
        }
        SharedConnectivitySettingsState state = mManager.getSettingsState();
        log("handleLaunchSettings(), state:" + state);
        if (state == null) {
            Log.e(TAG, "No SettingsState to launch Instant Hotspot settings");
            return;
        }
        PendingIntent intent = state.getInstantTetherSettingsPendingIntent();
        if (intent == null) {
            Log.e(TAG, "No PendingIntent to launch Instant Hotspot settings");
            return;
        }
        sendSettingsIntent(intent);
    }

    @WorkerThread
    @VisibleForTesting
    void sendSettingsIntent(@NonNull PendingIntent intent) {
        try {
            log("sendSettingsIntent(), sent intent:" + intent);
            intent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to launch Instant Hotspot settings", e);
        }
    }

    @WorkerThread
    class ClientCallback implements SharedConnectivityClientCallback {

        @Override
        public void onHotspotNetworkConnectionStatusChanged(HotspotNetworkConnectionStatus status) {
            log("onHotspotNetworkConnectionStatusChanged(), status:" + status);
        }

        @Override
        public void onHotspotNetworksUpdated(List<HotspotNetwork> networks) {
            log("onHotspotNetworksUpdated(), networks:" + networks);
        }

        @Override
        public void onKnownNetworkConnectionStatusChanged(KnownNetworkConnectionStatus status) {
            log("onKnownNetworkConnectionStatusChanged(), status:" + status);
        }

        @Override
        public void onKnownNetworksUpdated(List<KnownNetwork> networks) {
            log("onKnownNetworksUpdated(), networks:" + networks);
        }

        @Override
        public void onRegisterCallbackFailed(Exception e) {
            Log.e(TAG, "onRegisterCallbackFailed(), e:" + e);
        }

        @Override
        public void onServiceConnected() {
            SharedConnectivitySettingsState state = mManager.getSettingsState();
            Log.d(TAG, "onServiceConnected(), Manager#getSettingsState:" + state);
            mSettingsState.postValue(state);
        }

        @Override
        public void onServiceDisconnected() {
            log("onServiceDisconnected()");
        }

        @Override
        public void onSharedConnectivitySettingsChanged(SharedConnectivitySettingsState state) {
            Log.d(TAG, "onSharedConnectivitySettingsChanged(), state:" + state);
            mSettingsState.postValue(state);
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }

    /**
     * Returns true if Shared Connectivity feature is enabled.
     */
    public static boolean isDeviceConfigEnabled() {
        return DeviceConfig.getBoolean(DEVICE_CONFIG_NAMESPACE, DEVICE_CONFIG_KEY, false);
    }
}
