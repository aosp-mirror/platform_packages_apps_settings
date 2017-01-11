/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.android.settings.core.PreferenceController;

import java.util.List;

/**
 * {@link PreferenceController} that opens saved network subsetting.
 */
public class SavedNetworkPreferenceController extends PreferenceController {

    private static final String KEY_SAVED_NETWORKS = "saved_networks";

    private final WifiManager mWifiManager;

    public SavedNetworkPreferenceController(Context context, WifiManager wifiManager) {
        super(context);
        mWifiManager = wifiManager;
    }

    @Override
    public boolean isAvailable() {
        final List<WifiConfiguration> config = mWifiManager.getConfiguredNetworks();
        return config != null && !config.isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SAVED_NETWORKS;
    }
}
