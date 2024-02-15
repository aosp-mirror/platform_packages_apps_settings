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

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import android.app.Application;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.sharedconnectivity.app.SharedConnectivitySettingsState;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.factory.WifiFeatureProvider;
import com.android.settings.wifi.repository.SharedConnectivityRepository;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Wi-Fi Hotspot ViewModel
 */
public class WifiTetherViewModel extends AndroidViewModel {
    private static final String TAG = "WifiTetherViewModel";
    static final int RES_INSTANT_HOTSPOT_SUMMARY_ON = R.string.wifi_hotspot_instant_summary_on;
    static final int RES_INSTANT_HOTSPOT_SUMMARY_OFF = R.string.wifi_hotspot_instant_summary_off;

    static Map<Integer, Integer> sSecuritySummaryResMap = new HashMap<>();

    static {
        sSecuritySummaryResMap.put(
                SECURITY_TYPE_WPA3_SAE, com.android.settingslib.R.string.wifi_security_sae);
        sSecuritySummaryResMap.put(SECURITY_TYPE_WPA3_SAE_TRANSITION,
                com.android.settingslib.R.string.wifi_security_psk_sae);
        sSecuritySummaryResMap.put(
                SECURITY_TYPE_WPA2_PSK, com.android.settingslib.R.string.wifi_security_wpa2);
        sSecuritySummaryResMap.put(
                SECURITY_TYPE_OPEN, com.android.settingslib.R.string.wifi_security_none);
    }

    static Map<Integer, Integer> sSpeedSummaryResMap = new HashMap<>();

    static {
        sSpeedSummaryResMap.put(SPEED_2GHZ, R.string.wifi_hotspot_speed_summary_2g);
        sSpeedSummaryResMap.put(SPEED_5GHZ, R.string.wifi_hotspot_speed_summary_5g);
        sSpeedSummaryResMap.put(SPEED_6GHZ, R.string.wifi_hotspot_speed_summary_6g);
        sSpeedSummaryResMap.put(SPEED_2GHZ_5GHZ, R.string.wifi_hotspot_speed_summary_2g_and_5g);
    }

    protected final WifiHotspotRepository mWifiHotspotRepository;
    protected MutableLiveData<Integer> mSecuritySummary;
    protected MutableLiveData<Integer> mSpeedSummary;

    protected final Observer<Integer> mSecurityTypeObserver = st -> onSecurityTypeChanged(st);
    protected final Observer<Integer> mSpeedTypeObserver = st -> onSpeedTypeChanged(st);

    private SharedConnectivityRepository mSharedConnectivityRepository;
    @VisibleForTesting
    MutableLiveData<String> mInstantHotspotSummary = new MutableLiveData<>();
    @VisibleForTesting
    Observer<SharedConnectivitySettingsState> mInstantHotspotStateObserver =
            state -> onInstantHotspotStateChanged(state);

    public WifiTetherViewModel(@NotNull Application application) {
        super(application);
        WifiFeatureProvider featureProvider = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider();
        mWifiHotspotRepository = featureProvider.getWifiHotspotRepository();
        mSharedConnectivityRepository = featureProvider.getSharedConnectivityRepository();
        if (mSharedConnectivityRepository.isServiceAvailable()) {
            mSharedConnectivityRepository.getSettingsState()
                    .observeForever(mInstantHotspotStateObserver);
        }
    }

    @Override
    protected void onCleared() {
        if (mSecuritySummary != null) {
            mWifiHotspotRepository.getSecurityType().removeObserver(mSecurityTypeObserver);
        }
        if (mSpeedSummary != null) {
            mWifiHotspotRepository.getSpeedType().removeObserver(mSpeedTypeObserver);
        }
        if (mSharedConnectivityRepository.isServiceAvailable()) {
            mSharedConnectivityRepository.getSettingsState()
                    .removeObserver(mInstantHotspotStateObserver);
        }
    }

    /**
     * Return whether Wi-Fi Hotspot Speed Feature is available or not.
     *
     * @return {@code true} if Wi-Fi Hotspot Speed Feature is available
     */
    public boolean isSpeedFeatureAvailable() {
        return mWifiHotspotRepository.isSpeedFeatureAvailable();
    }

    /**
     * Gets the Wi-Fi tethered AP Configuration.
     *
     * @return AP details in {@link SoftApConfiguration}
     */
    public SoftApConfiguration getSoftApConfiguration() {
        return mWifiHotspotRepository.getSoftApConfiguration();
    }

    /**
     * Sets the tethered Wi-Fi AP Configuration.
     *
     * @param config A valid SoftApConfiguration specifying the configuration of the SAP.
     */
    public void setSoftApConfiguration(SoftApConfiguration config) {
        mWifiHotspotRepository.setSoftApConfiguration(config);
    }

    /**
     * Refresh data from the SoftApConfiguration.
     */
    public void refresh() {
        mWifiHotspotRepository.refresh();
    }

    /**
     * Gets SecuritySummary LiveData
     */
    public LiveData<Integer> getSecuritySummary() {
        if (mSecuritySummary == null) {
            mSecuritySummary = new MutableLiveData<>();
            mWifiHotspotRepository.getSecurityType().observeForever(mSecurityTypeObserver);
        }
        return mSecuritySummary;
    }

    protected void onSecurityTypeChanged(int securityType) {
        int resId = R.string.summary_placeholder;
        if (sSecuritySummaryResMap.containsKey(securityType)) {
            resId = sSecuritySummaryResMap.get(securityType);
        }
        mSecuritySummary.setValue(resId);
    }

    /**
     * Gets SpeedSummary LiveData
     */
    public LiveData<Integer> getSpeedSummary() {
        if (mSpeedSummary == null) {
            mSpeedSummary = new MutableLiveData<>();
            mWifiHotspotRepository.getSpeedType().observeForever(mSpeedTypeObserver);
        }
        return mSpeedSummary;
    }

    protected void onSpeedTypeChanged(Integer speedType) {
        int resId = R.string.summary_placeholder;
        if (sSpeedSummaryResMap.containsKey(speedType)) {
            resId = sSpeedSummaryResMap.get(speedType);
        }
        mSpeedSummary.setValue(resId);
    }

    /**
     * Gets Restarting LiveData
     */
    public LiveData<Boolean> getRestarting() {
        return mWifiHotspotRepository.getRestarting();
    }

    /**
     * Return whether Wi-Fi Instant Hotspot feature is available or not.
     *
     * @return {@code true} if Wi-Fi Instant Hotspot feature is available
     */
    public boolean isInstantHotspotFeatureAvailable() {
        return mSharedConnectivityRepository.isServiceAvailable();
    }

    /**
     * Gets InstantHotspotSummary
     */
    public LiveData<String> getInstantHotspotSummary() {
        return mInstantHotspotSummary;
    }

    @VisibleForTesting
    void onInstantHotspotStateChanged(SharedConnectivitySettingsState state) {
        log("onInstantHotspotStateChanged(), state:" + state);
        if (state == null) {
            mInstantHotspotSummary.setValue(null);
            return;
        }
        mInstantHotspotSummary.setValue(getInstantHotspotSummary(state.isInstantTetherEnabled()));
    }

    private String getInstantHotspotSummary(boolean enabled) {
        return getApplication().getString(
                enabled ? RES_INSTANT_HOTSPOT_SUMMARY_ON : RES_INSTANT_HOTSPOT_SUMMARY_OFF);
    }

    /**
     * Launch Instant Hotspot Settings
     */
    public void launchInstantHotspotSettings() {
        mSharedConnectivityRepository.launchSettings();
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
