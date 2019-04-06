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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.p2p.WifiP2pPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class ConfigureWifiSettings extends DashboardFragment {

    private static final String TAG = "ConfigureWifiSettings";

    public static final String KEY_IP_ADDRESS = "current_ip_address";
    public static final int WIFI_WAKEUP_REQUEST_CODE = 600;

    private WifiWakeupPreferenceController mWifiWakeupPreferenceController;
    private UseOpenWifiPreferenceController mUseOpenWifiPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONFIGURE_WIFI;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getInitialExpandedChildCount() {
        int tileLimit = 2;
        if (mUseOpenWifiPreferenceController.isAvailable()) {
            tileLimit++;
        }
        return tileLimit;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_configure_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mWifiWakeupPreferenceController = new WifiWakeupPreferenceController(context, this,
                getSettingsLifecycle());
        mUseOpenWifiPreferenceController = new UseOpenWifiPreferenceController(context, this,
                getSettingsLifecycle());
        final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(mWifiWakeupPreferenceController);
        controllers.add(new NotifyOpenNetworksPreferenceController(context,
                getSettingsLifecycle()));
        controllers.add(mUseOpenWifiPreferenceController);
        controllers.add(new WifiInfoPreferenceController(context, getSettingsLifecycle(),
                wifiManager));
        controllers.add(new WifiP2pPreferenceController(context, getSettingsLifecycle(),
                wifiManager));
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WIFI_WAKEUP_REQUEST_CODE && mWifiWakeupPreferenceController != null) {
            mWifiWakeupPreferenceController.onActivityResult(requestCode, resultCode);
            return;
        }
        if (requestCode == UseOpenWifiPreferenceController.REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY
                && mUseOpenWifiPreferenceController != null) {
            mUseOpenWifiPreferenceController.onActivityResult(requestCode, resultCode);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                    List<String> keys = super.getNonIndexableKeys(context);

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

                protected boolean isPageSearchEnabled(Context context) {
                    return context.getResources()
                            .getBoolean(R.bool.config_show_wifi_settings);
                }
            };
}
