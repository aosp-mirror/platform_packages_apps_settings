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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkCapabilities.Transport;
import android.net.wifi.WifiManager;

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

    private OnInternetTypeChangedListener mOnInternetTypeChangedListener;

    /** Interface that handles the internet type changed callback */
    public interface OnInternetTypeChangedListener {
        /**
         * Called when internet type is changed.
         *
         * @param internetType the internet type
         */
        void onInternetTypeChanged(@InternetType int internetType);
    }

    /**
     * Indicates this internet is unavailable type in airplane mode on.
     */
    public static final int INTERNET_APM = 0;

    /**
     * Indicates this internet uses an airplane mode network type.
     */
    public static final int INTERNET_APM_NETWORKS = 1;

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
            INTERNET_APM,
            INTERNET_APM_NETWORKS,
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
    @Transport int mTransport;
    private static Map<Integer, Integer> sTransportMap = new HashMap<>();
    static {
        sTransportMap.put(TRANSPORT_WIFI, INTERNET_WIFI);
        sTransportMap.put(TRANSPORT_CELLULAR, INTERNET_CELLULAR);
        sTransportMap.put(TRANSPORT_ETHERNET, INTERNET_ETHERNET);
    }

    private NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            if (network == null) {
                return;
            }
            final NetworkCapabilities networkCapabilities =
                    mConnectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities == null) {
                return;
            }
            for (@Transport int transport : networkCapabilities.getTransportTypes()) {
                if (sTransportMap.containsKey(transport)) {
                    mTransport = transport;
                    break;
                }
            }
            update();
        }
    };

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    public InternetUpdater(Context context, Lifecycle lifecycle,
            OnInternetTypeChangedListener listener) {
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }
        mContext = context;
        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mOnInternetTypeChangedListener = listener;
        lifecycle.addObserver(this);
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mAirplaneModeEnabler.start();
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter);
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mAirplaneModeEnabler.stop();
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        update();
    }

    @VisibleForTesting
    void update() {
        if (mAirplaneModeEnabler.isAirplaneModeOn()) {
            mInternetType = mWifiManager.isWifiEnabled() ? INTERNET_APM_NETWORKS : INTERNET_APM;
        } else {
            mInternetType = sTransportMap.get(mTransport);
        }
        if (mOnInternetTypeChangedListener != null) {
            mOnInternetTypeChangedListener.onInternetTypeChanged(mInternetType);
        }
    }

    /**
     * Get the internet type.
     */
    public @InternetType int getInternetType() {
        return mInternetType;
    }
}
