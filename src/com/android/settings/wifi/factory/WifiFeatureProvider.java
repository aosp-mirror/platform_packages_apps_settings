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

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import com.android.settings.wifi.repository.WifiHotspotRepository;

/**
 * Wi-Fi Feature Provider
 */
public class WifiFeatureProvider {

    private final Context mAppContext;
    private WifiManager mWifiManager;
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
     * Get WifiRepository
     */
    public WifiHotspotRepository getWifiHotspotRepository() {
        if (mWifiHotspotRepository == null) {
            mWifiHotspotRepository = new WifiHotspotRepository(mAppContext, getWifiManager());
        }
        return mWifiHotspotRepository;
    }
}

