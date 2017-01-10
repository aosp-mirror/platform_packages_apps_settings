/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;

public class ConfigureWifiSettings extends DashboardFragment {

    private static final String TAG = "ConfigureWifiSettings";

    private WifiManager mWifiManager;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_WIFI;
    }

    @Override
    protected String getCategoryKey() {
        // We don't want to inject any external settings into this screen.
        return null;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_configure_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new WifiInfoPreferenceController(context, getLifecycle(), mWifiManager));
        controllers.add(new SavedNetworkPreferenceController(context, mWifiManager));
        controllers.add(new NotifyOpenNetworksPreferenceController(context, mWifiManager));
        controllers.add(new CellularFallbackPreferenceController(context));
        controllers.add(new AllowRecommendationPreferenceController(context));
        controllers.add(new WifiSleepPolicyPreferenceController(context));
        return controllers;
    }
}
