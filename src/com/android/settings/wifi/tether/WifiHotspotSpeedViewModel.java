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

import android.app.Application;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Wi-Fi Hotspot Speed View Model
 */
public class WifiHotspotSpeedViewModel extends AndroidViewModel {
    private static final String TAG = "WifiHotspotSpeedViewModel";
    @VisibleForTesting
    static final int RES_SPEED_5G_SUMMARY = R.string.wifi_hotspot_speed_5g_summary;
    @VisibleForTesting
    static final int RES_SPEED_6G_SUMMARY = R.string.wifi_hotspot_speed_6g_summary;
    @VisibleForTesting
    static final int RES_SUMMARY_UNAVAILABLE = R.string.wifi_hotspot_speed_summary_unavailable;

    protected final WifiHotspotRepository mWifiHotspotRepository;
    protected Map<Integer, SpeedInfo> mSpeedInfoMap = new HashMap<>();
    protected MutableLiveData<Map<Integer, SpeedInfo>> mSpeedInfoMapData;
    protected SpeedInfo mSpeedInfo2g = new SpeedInfo(false, true, false);
    protected SpeedInfo mSpeedInfo5g = new SpeedInfo(false, true, false);
    protected SpeedInfo mSpeedInfo2g5g = new SpeedInfo(false, true, true);
    protected SpeedInfo mSpeedInfo6g = new SpeedInfo(false, true, true);

    protected final Observer<Boolean> m6gAvailableObserver = a -> on6gAvailableChanged(a);
    protected final Observer<Boolean> m5gAvailableObserver = a -> on5gAvailableChanged(a);
    protected final Observer<Integer> mSpeedTypeObserver = st -> onSpeedTypeChanged(st);

    public WifiHotspotSpeedViewModel(@NotNull Application application) {
        super(application);
        mWifiHotspotRepository = FeatureFactory.getFeatureFactory().getWifiFeatureProvider()
                .getWifiHotspotRepository();
        mWifiHotspotRepository.get6gAvailable().observeForever(m6gAvailableObserver);
        mWifiHotspotRepository.get5gAvailable().observeForever(m5gAvailableObserver);
        mWifiHotspotRepository.getSpeedType().observeForever(mSpeedTypeObserver);
        mWifiHotspotRepository.setAutoRefresh(true);

        // The visibility of the 6 GHz speed option will not change on a Pixel device.
        mSpeedInfo6g.mIsVisible = mWifiHotspotRepository.is6GHzBandSupported();
    }

    @Override
    protected void onCleared() {
        mWifiHotspotRepository.get6gAvailable().removeObserver(m6gAvailableObserver);
        mWifiHotspotRepository.get5gAvailable().removeObserver(m5gAvailableObserver);
        mWifiHotspotRepository.getSpeedType().removeObserver(mSpeedTypeObserver);
    }

    protected void on6gAvailableChanged(Boolean available) {
        Log.d(TAG, "on6gAvailableChanged(), available:" + available);
        mSpeedInfo6g.mIsEnabled = available;
        mSpeedInfo6g.mSummary = getApplication()
                .getString(available ? RES_SPEED_6G_SUMMARY : RES_SUMMARY_UNAVAILABLE);
        updateSpeedInfoMapData();
    }

    protected void on5gAvailableChanged(Boolean available) {
        Log.d(TAG, "on5gAvailableChanged(), available:" + available);
        mSpeedInfo5g.mIsEnabled = available;
        mSpeedInfo5g.mSummary = getApplication()
                .getString(available ? RES_SPEED_5G_SUMMARY : RES_SUMMARY_UNAVAILABLE);

        boolean showDualBand = mWifiHotspotRepository.isDualBand() && available;
        log("on5gAvailableChanged(), showDualBand:" + showDualBand);
        mSpeedInfo2g5g.mIsVisible = showDualBand;
        mSpeedInfo2g.mIsVisible = !showDualBand;
        mSpeedInfo5g.mIsVisible = !showDualBand;
        updateSpeedInfoMapData();
    }

    protected void onSpeedTypeChanged(Integer speedType) {
        log("onSpeedTypeChanged(), speedType:" + speedType);
        mSpeedInfo2g.mIsChecked = speedType.equals(SPEED_2GHZ);
        mSpeedInfo5g.mIsChecked = speedType.equals(SPEED_5GHZ);
        mSpeedInfo2g5g.mIsChecked = speedType.equals(SPEED_2GHZ_5GHZ);
        mSpeedInfo6g.mIsChecked = speedType.equals(SPEED_6GHZ);
        updateSpeedInfoMapData();
    }

    /**
     * Sets SpeedType
     */
    public void setSpeedType(Integer speedType) {
        mWifiHotspotRepository.setSpeedType(speedType);
    }

    /**
     * Gets Speed Information LiveData
     */
    public LiveData<Map<Integer, SpeedInfo>> getSpeedInfoMapData() {
        if (mSpeedInfoMapData == null) {
            mSpeedInfoMapData = new MutableLiveData<>();
            mSpeedInfoMapData.setValue(mSpeedInfoMap);
            log("getSpeedViewData(), mSpeedInfoMap:" + mSpeedInfoMapData.getValue());
        }
        return mSpeedInfoMapData;
    }

    protected void updateSpeedInfoMapData() {
        mSpeedInfoMap.put(SPEED_2GHZ, mSpeedInfo2g);
        mSpeedInfoMap.put(SPEED_5GHZ, mSpeedInfo5g);
        mSpeedInfoMap.put(SPEED_2GHZ_5GHZ, mSpeedInfo2g5g);
        mSpeedInfoMap.put(SPEED_6GHZ, mSpeedInfo6g);
        if (mSpeedInfoMapData != null) {
            mSpeedInfoMapData.setValue(mSpeedInfoMap);
        }
    }

    /**
     * Gets Restarting LiveData
     */
    public LiveData<Boolean> getRestarting() {
        return mWifiHotspotRepository.getRestarting();
    }

    /**
     * Wi-Fi Hotspot Speed Information
     */
    public static final class SpeedInfo {
        Boolean mIsChecked;
        boolean mIsEnabled;
        boolean mIsVisible;
        String mSummary;

        public SpeedInfo(boolean isChecked, boolean isEnabled, boolean isVisible) {
            this.mIsChecked = isChecked;
            this.mIsEnabled = isEnabled;
            this.mIsVisible = isVisible;
        }

        @Override
        public String toString() {
            return new StringBuilder("SpeedInfo{")
                    .append("isChecked:").append(mIsChecked)
                    .append(",isEnabled:").append(mIsEnabled)
                    .append(",isVisible:").append(mIsVisible)
                    .append(",mSummary:").append(mSummary)
                    .append('}').toString();
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
