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

package com.android.settings.wifi.details;

import android.app.Application;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.overlay.FeatureFactory;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

import org.jetbrains.annotations.NotNull;

/**
 * Wi-Fi Network Details ViewModel
 */
public class WifiNetworkDetailsViewModel extends AndroidViewModel {
    private static final String TAG = "WifiNetworkDetailsViewModel";

    @VisibleForTesting
    MutableLiveData<HotspotNetworkData> mHotspotNetworkData = new MutableLiveData<>();

    public WifiNetworkDetailsViewModel(@NotNull Application application) {
        super(application);
    }

    /** Sets the {@link WifiEntry} class */
    public void setWifiEntry(WifiEntry wifiEntry) {
        if (!(wifiEntry instanceof HotspotNetworkEntry)) {
            log("post HotspotNetworkData:null");
            mHotspotNetworkData.postValue(null);
            return;
        }
        HotspotNetworkEntry entry = (HotspotNetworkEntry) wifiEntry;
        HotspotNetworkData data = new HotspotNetworkData(
                entry.getNetworkType(),
                entry.getUpstreamConnectionStrength(),
                entry.getBatteryPercentage(),
                entry.isBatteryCharging());
        log("post HotspotNetworkData:" + data);
        mHotspotNetworkData.postValue(data);
    }

    /** Gets the {@link HotspotNetworkData} LiveData */
    public LiveData<HotspotNetworkData> getHotspotNetworkData() {
        return mHotspotNetworkData;
    }

    /** The {@link HotspotNetworkData} class */
    static class HotspotNetworkData {
        private int mNetworkType;
        private int mUpstreamConnectionStrength;
        private int mBatteryPercentage;
        private boolean mIsBatteryCharging;

        HotspotNetworkData(int networkType, int upstreamConnectionStrength,
                int batteryPercentage,
                boolean isBatteryCharging) {
            mNetworkType = networkType;
            mUpstreamConnectionStrength = upstreamConnectionStrength;
            mBatteryPercentage = batteryPercentage;
            mIsBatteryCharging = isBatteryCharging;
        }

        /** Gets the network type */
        public int getNetworkType() {
            return mNetworkType;
        }

        /** Gets the upstream connection strength */
        public int getUpstreamConnectionStrength() {
            return mUpstreamConnectionStrength;
        }

        /** Gets the battery percentage */
        public int getBatteryPercentage() {
            return mBatteryPercentage;
        }

        /** Returns true if the battery is charging */
        public boolean isBatteryCharging() {
            return mIsBatteryCharging;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + ":{networkType:" + mNetworkType
                    + ", upstreamConnectionStrength:" + mUpstreamConnectionStrength
                    + ", batteryPercentage:" + mBatteryPercentage
                    + ", isBatteryCharging:" + mIsBatteryCharging
                    + " }";
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
