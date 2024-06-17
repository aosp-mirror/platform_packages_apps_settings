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

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_ETHERNET;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.AirplaneModeEnabler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to update the internet type for connected network preference
 */
public class InternetUpdater implements AirplaneModeEnabler.OnAirplaneModeChangedListener,
        LifecycleObserver {

    private static final String TAG = "InternetUpdater";

    private InternetChangeListener mListener;

    /** Interface that handles the internet updater callback */
    public interface InternetChangeListener {
        /**
         * Called when internet type is changed.
         *
         * @param internetType the internet type
         */
        default void onInternetTypeChanged(@InternetType int internetType) {};

        /**
         * Called when airplane mode state is changed.
         */
        default void onAirplaneModeChanged(boolean isAirplaneModeOn) {};

        /**
         * Called when Wi-Fi enabled is changed.
         */
        default void onWifiEnabledChanged(boolean enabled) {};
    }

    /**
     * Indicates the internet is off when airplane mode is on.
     */
    public static final int INTERNET_OFF = 0;

    /**
     * Indicates this internet is not connected (includes no networks connected) or network(s)
     * available.
     *
     * Examples include:
     * <p>When airplane mode is turned off, and some networks (Wi-Fi, Mobile-data) are turned on,
     * but no network can access the Internet.
     *
     * <p>When the airplane mode is turned on, and the WiFi is also turned on, but the WiFi is not
     * connected or cannot access the Internet.
     */
    public static final int INTERNET_NETWORKS_AVAILABLE = 1;

    /**
     * Indicates this internet uses a Wi-Fi network type.
     */
    public static final int INTERNET_WIFI = 2;

    /**
     * Indicates this internet uses a Cellular network type.
     */
    public static final int INTERNET_CELLULAR = 3;

    /**
     * Indicates this internet uses a Ethernet network type.
     */
    public static final int INTERNET_ETHERNET = 4;

    @Retention(RetentionPolicy.SOURCE)
    @android.annotation.IntDef(prefix = { "INTERNET_" }, value = {
            INTERNET_OFF,
            INTERNET_NETWORKS_AVAILABLE,
            INTERNET_WIFI,
            INTERNET_CELLULAR,
            INTERNET_ETHERNET,
    })
    public @interface InternetType { }
    private @InternetType int mInternetType;

    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;
    private final IntentFilter mWifiStateFilter;
    @VisibleForTesting
    AirplaneModeEnabler mAirplaneModeEnabler;

    @VisibleForTesting
    boolean mInternetAvailable;
    @VisibleForTesting
    int mTransport;
    private static Map<Integer, Integer> sTransportMap = new HashMap<>();
    static {
        sTransportMap.put(TRANSPORT_WIFI, INTERNET_WIFI);
        sTransportMap.put(TRANSPORT_CELLULAR, INTERNET_CELLULAR);
        sTransportMap.put(TRANSPORT_ETHERNET, INTERNET_ETHERNET);
    }

    private NetworkCallback mNetworkCallback = new NetworkCallback() {
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {
            updateInternetAvailable(networkCapabilities);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mInternetAvailable = false;
            updateInternetType();
        }
    };

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fetchActiveNetwork();
            if (mListener != null) {
                mListener.onWifiEnabledChanged(mWifiManager.isWifiEnabled());
            }
        }
    };

    public InternetUpdater(Context context, Lifecycle lifecycle, InternetChangeListener listener) {
        mContext = context;
        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mListener = listener;
        fetchActiveNetwork();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mAirplaneModeEnabler.start();
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mAirplaneModeEnabler.stop();
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    /** @OnLifecycleEvent(ON_DESTROY) */
    @OnLifecycleEvent(ON_DESTROY)
    public void onDestroy() {
        mAirplaneModeEnabler.close();
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        fetchActiveNetwork();
        if (mListener != null) {
            mListener.onAirplaneModeChanged(isAirplaneModeOn);
        }
    }

    private void fetchActiveNetwork() {
        Network activeNetwork = mConnectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            mInternetAvailable = false;
            updateInternetType();
            return;
        }

        NetworkCapabilities activeNetworkCapabilities =
                mConnectivityManager.getNetworkCapabilities(activeNetwork);
        if (activeNetworkCapabilities == null) {
            mInternetAvailable = false;
            updateInternetType();
            return;
        }

        updateInternetAvailable(activeNetworkCapabilities);
    }

    @VisibleForTesting
    void updateInternetAvailable(@NonNull NetworkCapabilities capabilities) {
        boolean internetAvailable = false;
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            for (int transport : capabilities.getTransportTypes()) {
                if (sTransportMap.containsKey(transport)) {
                    mTransport = transport;
                    internetAvailable = true;
                    Log.i(TAG, "Detect an internet available network with transport type: "
                            + mTransport);
                    break;
                }
            }
        }
        mInternetAvailable = internetAvailable;
        updateInternetType();
    }

    @VisibleForTesting
    void updateInternetType() {
        @InternetType int internetType = INTERNET_NETWORKS_AVAILABLE;
        if (mInternetAvailable) {
            internetType = sTransportMap.get(mTransport);
            if (internetType == INTERNET_WIFI && isCarrierWifiActive()) {
                internetType = INTERNET_CELLULAR;
            }
        } else if (mAirplaneModeEnabler.isAirplaneModeOn()
                && mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            internetType = INTERNET_OFF;
        }
        mInternetType = internetType;

        if (mListener != null) {
            mListener.onInternetTypeChanged(mInternetType);
        }
    }

    protected boolean isCarrierWifiActive() {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null || !wifiInfo.isCarrierMerged()) {
            return false;
        }
        Log.i(TAG, "Detect a merged carrier Wi-Fi connected.");
        return true;
    }

    /**
     * Get the internet type.
     */
    public @InternetType int getInternetType() {
        return mInternetType;
    }

    /**
     * Return ture when the airplane mode is on.
     */
    public boolean isAirplaneModeOn() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    /**
     * Return ture when the Wi-Fi is enabled.
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }
}
