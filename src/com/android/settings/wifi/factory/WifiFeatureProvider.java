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

package com.android.settings.wifi.factory;

import android.annotation.Nullable;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.android.settings.wifi.repository.WifiHotspotRepository;
import com.android.settings.wifi.tether.WifiHotspotSecurityViewModel;
import com.android.settings.wifi.tether.WifiHotspotSpeedViewModel;
import com.android.settings.wifi.tether.WifiTetherViewModel;

import org.jetbrains.annotations.NotNull;

/**
 * Wi-Fi Feature Provider
 */
public class WifiFeatureProvider {
    private static final String TAG = "WifiFeatureProvider";

    private final Context mAppContext;
    private WifiManager mWifiManager;
    private WifiVerboseLogging mWifiVerboseLogging;
    private WifiHotspotRepository mWifiHotspotRepository;

    public WifiFeatureProvider(@NonNull Context appContext) {
        mAppContext = appContext;
    }

    /**
     * Get WifiManager
     */
    public WifiManager getWifiManager() {
        if (mWifiManager == null) {
            mWifiManager = mAppContext.getSystemService(WifiManager.class);
        }
        return mWifiManager;
    }

    /**
     * Get WifiVerboseLogging
     */
    public WifiVerboseLogging getWifiVerboseLogging() {
        if (mWifiVerboseLogging == null) {
            mWifiVerboseLogging = new WifiVerboseLogging(mAppContext, getWifiManager());
        }
        return mWifiVerboseLogging;
    }

    /**
     * Get WifiHotspotRepository
     */
    public WifiHotspotRepository getWifiHotspotRepository() {
        if (mWifiHotspotRepository == null) {
            mWifiHotspotRepository = new WifiHotspotRepository(mAppContext, getWifiManager());
            verboseLog(TAG, "getWifiHotspotRepository():" + mWifiHotspotRepository);
        }
        return mWifiHotspotRepository;
    }

    /**
     * Get WifiTetherViewModel
     */
    public WifiTetherViewModel getWifiTetherViewModel(@NotNull ViewModelStoreOwner owner) {
        return new ViewModelProvider(owner).get(WifiTetherViewModel.class);
    }

    /**
     * Get WifiHotspotSecurityViewModel
     */
    public WifiHotspotSecurityViewModel getWifiHotspotSecurityViewModel(
            @NotNull ViewModelStoreOwner owner) {
        WifiHotspotSecurityViewModel viewModel =
                new ViewModelProvider(owner).get(WifiHotspotSecurityViewModel.class);
        verboseLog(TAG, "getWifiHotspotSecurityViewModel():" + viewModel);
        return viewModel;
    }

    /**
     * Get WifiHotspotSpeedViewModel
     */
    public WifiHotspotSpeedViewModel getWifiHotspotSpeedViewModel(
            @NotNull ViewModelStoreOwner owner) {
        WifiHotspotSpeedViewModel viewModel =
                new ViewModelProvider(owner).get(WifiHotspotSpeedViewModel.class);
        verboseLog(TAG, "getWifiHotspotSpeedViewModel():" + viewModel);
        return viewModel;
    }

    /**
     * Send a {@link Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public void verboseLog(@Nullable String tag, @NonNull String msg) {
        getWifiVerboseLogging().log(tag, msg);
    }
}

