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

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Wi-Fi Hotspot Speed & compatibility Settings
 */
public class WifiHotspotSpeedSettings extends DashboardFragment implements
        SelectorWithWidgetPreference.OnClickListener {

    private static final String TAG = "WifiHotspotSpeedSettings";

    protected static final String KEY_SPEED_2GHZ = "wifi_hotspot_speed_2g";
    protected static final String KEY_SPEED_5GHZ = "wifi_hotspot_speed_5g";
    protected static final String KEY_SPEED_2GHZ_5GHZ = "wifi_hotspot_speed_2g_5g";
    protected static final String KEY_SPEED_6GHZ = "wifi_hotspot_speed_6g";
    protected static Map<String, Integer> sSpeedKeyMap = new HashMap<>();

    static {
        sSpeedKeyMap.put(KEY_SPEED_2GHZ, SPEED_2GHZ);
        sSpeedKeyMap.put(KEY_SPEED_5GHZ, SPEED_5GHZ);
        sSpeedKeyMap.put(KEY_SPEED_2GHZ_5GHZ, SPEED_2GHZ_5GHZ);
        sSpeedKeyMap.put(KEY_SPEED_6GHZ, SPEED_6GHZ);
    }

    protected WifiHotspotSpeedViewModel mWifiHotspotSpeedViewModel;
    protected Map<Integer, SelectorWithWidgetPreference> mSpeedPreferenceMap = new HashMap<>();

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_hotspot_speed;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_TETHER_SETTINGS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        loadPreferences();
        mWifiHotspotSpeedViewModel = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotSpeedViewModel(this);
        onSpeedInfoMapDataChanged(mWifiHotspotSpeedViewModel.getSpeedInfoMapData().getValue());
        mWifiHotspotSpeedViewModel.getSpeedInfoMapData()
                .observe(this, this::onSpeedInfoMapDataChanged);
        mWifiHotspotSpeedViewModel.getRestarting().observe(this, this::onRestartingChanged);
    }

    protected void loadPreferences() {
        for (Map.Entry<String, Integer> entry : sSpeedKeyMap.entrySet()) {
            SelectorWithWidgetPreference preference = findPreference(entry.getKey());
            if (preference != null) {
                preference.setOnClickListener(this);
                mSpeedPreferenceMap.put(entry.getValue(), preference);
            }
        }
    }

    protected void onSpeedInfoMapDataChanged(
            Map<Integer, WifiHotspotSpeedViewModel.SpeedInfo> speedInfoMap) {
        log("onSpeedViewDataChanged(), speedInfoMap:" + speedInfoMap);
        for (Map.Entry<Integer, SelectorWithWidgetPreference> entry :
                mSpeedPreferenceMap.entrySet()) {
            WifiHotspotSpeedViewModel.SpeedInfo speedInfo = speedInfoMap.get(entry.getKey());
            if (speedInfo == null) {
                continue;
            }
            SelectorWithWidgetPreference radioButton = entry.getValue();
            if (radioButton == null) {
                continue;
            }
            if (!speedInfo.mIsVisible) {
                radioButton.setVisible(false);
                continue;
            }
            radioButton.setEnabled(speedInfo.mIsEnabled);
            radioButton.setChecked(speedInfo.mIsChecked);
            if (speedInfo.mSummary != null) {
                radioButton.setSummary(speedInfo.mSummary);
            }
            // setVisible at the end to avoid UI flickering
            radioButton.setVisible(true);
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
        if (sSpeedKeyMap.containsKey(key)) {
            mWifiHotspotSpeedViewModel.setSpeedType(sSpeedKeyMap.get(key));
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
