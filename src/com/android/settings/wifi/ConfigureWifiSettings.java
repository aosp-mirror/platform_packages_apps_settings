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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.network.NetworkScorerPickerPreferenceController;
import com.android.settings.network.WifiCallingPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.p2p.WifiP2pPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureWifiSettings extends DashboardFragment {

    private static final String TAG = "ConfigureWifiSettings";

    public static final String KEY_IP_ADDRESS = "current_ip_address";

    private WifiWakeupPreferenceController mWifiWakeupPreferenceController;
    private UseOpenWifiPreferenceController mUseOpenWifiPreferenceController;

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
        int tileLimit = 1;
        if (mWifiWakeupPreferenceController.isAvailable()) {
            tileLimit++;
        }
        if (mUseOpenWifiPreferenceController.isAvailable()) {
            tileLimit++;
        }
        mProgressiveDisclosureMixin.setTileLimit(tileLimit);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_configure_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final NetworkScoreManagerWrapper networkScoreManagerWrapper =
                new NetworkScoreManagerWrapper(context.getSystemService(NetworkScoreManager.class));
        mWifiWakeupPreferenceController = new WifiWakeupPreferenceController(
                context, getLifecycle());
        mUseOpenWifiPreferenceController = new UseOpenWifiPreferenceController(context, this,
                networkScoreManagerWrapper, getLifecycle());
        final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(mWifiWakeupPreferenceController);
        controllers.add(new NetworkScorerPickerPreferenceController(context,
                networkScoreManagerWrapper));
        controllers.add(new NotifyOpenNetworksPreferenceController(context, getLifecycle()));
        controllers.add(mUseOpenWifiPreferenceController);
        controllers.add(new WifiInfoPreferenceController(context, getLifecycle(), wifiManager));
        controllers.add(new CellularFallbackPreferenceController(context));
        controllers.add(new WifiP2pPreferenceController(context, getLifecycle(), wifiManager));
        controllers.add(new WifiCallingPreferenceController(context));
        controllers.add(new WpsPreferenceController(
                context, getLifecycle(), wifiManager, getFragmentManager()));
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mUseOpenWifiPreferenceController == null ||
                !mUseOpenWifiPreferenceController.onActivityResult(requestCode, resultCode)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys =  super.getNonIndexableKeys(context);

                    // If connected to WiFi, this IP address will be the same as the Status IP.
                    // Or, if there is no connection they will say unavailable.
                    ConnectivityManager cm = (ConnectivityManager)
                            context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = cm.getActiveNetworkInfo();
                    if (info == null
                            || info.getType() == ConnectivityManager.TYPE_WIFI) {
                        keys.add(KEY_IP_ADDRESS);
                    }

                    return keys;
                }
            };
}
