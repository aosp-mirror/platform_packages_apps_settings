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

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.p2p.WifiP2pPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureWifiSettings extends DashboardFragment {

    private static final String TAG = "ConfigureWifiSettings";

    private WifiManager mWifiManager;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_WIFI;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(5);
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
        controllers.add(new CellularFallbackPreferenceController(context));
        controllers.add(new AllowRecommendationPreferenceController(context));
        controllers.add(new NotifyOpenNetworksPreferenceController(context, getLifecycle()));
        controllers.add(new WifiWakeupPreferenceController(context, getLifecycle()));
        controllers.add(new WifiSleepPolicyPreferenceController(context));
        controllers.add(new WifiP2pPreferenceController(context, getLifecycle(), mWifiManager));
        controllers.add(new WpsPreferenceController(
                context, getLifecycle(), mWifiManager, getFragmentManager()));
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.wifi_configure_settings;
                    return Arrays.asList(sir);
                }
            };
}
