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
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_UNKNOWN;

import android.app.Application;
import android.net.wifi.SoftApConfiguration;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Wi-Fi Hotspot ViewModel
 */
public class WifiTetherViewModel extends AndroidViewModel {
    private static final String TAG = "WifiTetherViewModel";

    protected static Map<Integer, Integer> sSpeedSummaryResMap = new HashMap<>();

    static {
        sSpeedSummaryResMap.put(SPEED_UNKNOWN, R.string.summary_placeholder);
        sSpeedSummaryResMap.put(SPEED_2GHZ, R.string.wifi_hotspot_speed_2g_summary);
        sSpeedSummaryResMap.put(SPEED_5GHZ, R.string.wifi_hotspot_speed_5g_summary);
        sSpeedSummaryResMap.put(SPEED_6GHZ, R.string.wifi_hotspot_speed_6g_summary);
        sSpeedSummaryResMap.put(SPEED_2GHZ_5GHZ, R.string.wifi_hotspot_speed_2g_and_5g_summary);
    }

    protected final WifiHotspotRepository mWifiHotspotRepository;
    protected MutableLiveData<Integer> mSpeedSummary;

    public WifiTetherViewModel(@NotNull Application application) {
        super(application);
        mWifiHotspotRepository = FeatureFactory.getFactory(application).getWifiFeatureProvider()
                .getWifiHotspotRepository();
        mWifiHotspotRepository.setAutoRefresh(true);
    }

    @Override
    protected void onCleared() {
        mWifiHotspotRepository.setAutoRefresh(false);
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
     * Gets SpeedSummary LiveData
     */
    public LiveData<Integer> getSpeedSummary() {
        if (mSpeedSummary == null) {
            mSpeedSummary = new MutableLiveData<>();
            mWifiHotspotRepository.getSpeedType().observeForever(this::onSpeedTypeChanged);
        }
        return Transformations.distinctUntilChanged(mSpeedSummary);
    }

    protected void onSpeedTypeChanged(Integer speedType) {
        mSpeedSummary.setValue(sSpeedSummaryResMap.get(speedType));
    }
}
