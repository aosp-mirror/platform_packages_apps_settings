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

package com.android.settings.wifi.tether;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wi-Fi Hotspot Security Settings
 */
public class WifiHotspotSecuritySettings extends DashboardFragment implements
        SelectorWithWidgetPreference.OnClickListener {
    private static final String TAG = "WifiHotspotSecuritySettings";

    protected WifiHotspotSecurityViewModel mWifiHotspotSecurityViewModel;
    protected Map<Integer, SelectorWithWidgetPreference> mPreferenceMap = new HashMap<>();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_TETHER_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_hotspot_security;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        loadViewModel();
    }

    protected void loadViewModel() {
        mWifiHotspotSecurityViewModel = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotSecurityViewModel(this);
        LiveData<List<WifiHotspotSecurityViewModel.ViewItem>> viewItemListData =
                mWifiHotspotSecurityViewModel.getViewItemListData();
        viewItemListData.observe(this, this::onViewItemListDataChanged);
        // set the onRadioButtonClicked callback to related preference
        for (WifiHotspotSecurityViewModel.ViewItem viewItem : viewItemListData.getValue()) {
            SelectorWithWidgetPreference preference = findPreference(viewItem.mKey);
            preference.setOnClickListener(this);
        }
        mWifiHotspotSecurityViewModel.getRestarting().observe(this, this::onRestartingChanged);
    }

    protected void onViewItemListDataChanged(
            List<WifiHotspotSecurityViewModel.ViewItem> viewItems) {
        log("onViewItemListDataChanged(), viewItems:" + viewItems);
        for (WifiHotspotSecurityViewModel.ViewItem viewItem : viewItems) {
            SelectorWithWidgetPreference preference = findPreference(viewItem.mKey);
            if (preference == null) {
                continue;
            }
            if (preference.isChecked() != viewItem.mIsChecked) {
                preference.setChecked(viewItem.mIsChecked);
            }
            if (preference.isEnabled() != viewItem.mIsEnabled) {
                preference.setEnabled(viewItem.mIsEnabled);
                if (viewItem.mIsEnabled) {
                    preference.setSummary(null);
                } else {
                    preference.setSummary(R.string.wifi_hotspot_security_summary_unavailable);
                }
            }
        }
    }

    @VisibleForTesting
    void onRestartingChanged(Boolean restarting) {
        log("onRestartingChanged(), restarting:" + restarting);
        setLoading(restarting, false);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        String key = emiter.getKey();
        log("onRadioButtonClicked(), key:" + key);
        if (key.isEmpty()) {
            return;
        }
        mWifiHotspotSecurityViewModel.handleRadioButtonClicked(key);
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
