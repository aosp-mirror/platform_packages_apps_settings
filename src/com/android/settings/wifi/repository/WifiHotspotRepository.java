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

package com.android.settings.wifi.repository;

import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_5GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_6GHZ;
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiAvailableChannel;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wi-Fi Hotspot Repository
 */
public class WifiHotspotRepository {
    private static final String TAG = "WifiHotspotRepository";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /** Wi-Fi hotspot band unknown. */
    public static final int BAND_UNKNOWN = 0;
    /** Wi-Fi hotspot band 2.4GHz and 5GHz. */
    public static final int BAND_2GHZ_5GHZ = BAND_2GHZ | BAND_5GHZ;
    /** Wi-Fi hotspot band 2.4GHz and 5GHz and 6GHz. */
    public static final int BAND_2GHZ_5GHZ_6GHZ = BAND_2GHZ | BAND_5GHZ | BAND_6GHZ;

    /** Wi-Fi hotspot speed unknown. */
    public static final int SPEED_UNKNOWN = 0;
    /** Wi-Fi hotspot speed 2.4GHz. */
    public static final int SPEED_2GHZ = 1;
    /** Wi-Fi hotspot speed 5GHz. */
    public static final int SPEED_5GHZ = 2;
    /** Wi-Fi hotspot speed 2.4GHz and 5GHz. */
    public static final int SPEED_2GHZ_5GHZ = 3;
    /** Wi-Fi hotspot speed 6GHz. */
    public static final int SPEED_6GHZ = 4;

    protected static Map<Integer, Integer> sSpeedMap = new HashMap<>();

    static {
        sSpeedMap.put(BAND_UNKNOWN, SPEED_UNKNOWN);
        sSpeedMap.put(BAND_2GHZ, SPEED_2GHZ);
        sSpeedMap.put(BAND_5GHZ, SPEED_5GHZ);
        sSpeedMap.put(BAND_6GHZ, SPEED_6GHZ);
        sSpeedMap.put(BAND_2GHZ_5GHZ, SPEED_2GHZ_5GHZ);
    }

    protected final Context mAppContext;
    protected final WifiManager mWifiManager;

    protected String mLastPassword;
    protected LastPasswordListener mLastPasswordListener = new LastPasswordListener();

    protected MutableLiveData<Integer> mSpeedType;

    protected Boolean mIsDualBand;
    protected Boolean mIs5gAvailable;
    protected Boolean mIs6gAvailable;
    protected String mCurrentCountryCode;
    protected ActiveCountryCodeChangedCallback mActiveCountryCodeChangedCallback;

    public WifiHotspotRepository(@NonNull Context appContext, @NonNull WifiManager wifiManager) {
        mAppContext = appContext;
        mWifiManager = wifiManager;
    }

    /**
     * Query the last configured Tethered Ap Passphrase since boot.
     */
    public void queryLastPasswordIfNeeded() {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_OPEN) {
            return;
        }
        mWifiManager.queryLastConfiguredTetheredApPassphraseSinceBoot(mAppContext.getMainExecutor(),
                mLastPasswordListener);
    }

    /**
     * Generate password.
     */
    public String generatePassword() {
        return !TextUtils.isEmpty(mLastPassword) ? mLastPassword : generateRandomPassword();
    }

    private class LastPasswordListener implements Consumer<String> {
        @Override
        public void accept(String password) {
            mLastPassword = password;
        }
    }

    private static String generateRandomPassword() {
        String randomUUID = UUID.randomUUID().toString();
        //first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }

    /**
     * Sets the tethered Wi-Fi AP Configuration.
     *
     * @param config A valid SoftApConfiguration specifying the configuration of the SAP.
     */
    public void setSoftApConfiguration(@NonNull SoftApConfiguration config) {
        mWifiManager.setSoftApConfiguration(config);
        refresh();
    }

    /**
     * Refresh data from the SoftApConfiguration.
     */
    public void refresh() {
        updateSpeedType();
    }

    /**
     * Set to auto refresh data.
     *
     * @param enabled whether the auto refresh should be enabled or not.
     */
    public void setAutoRefresh(boolean enabled) {
        if (enabled) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    /**
     * Gets SpeedType LiveData
     */
    public LiveData<Integer> getSpeedType() {
        if (mSpeedType == null) {
            mSpeedType = new MutableLiveData<>();
            updateSpeedType();
        }
        return Transformations.distinctUntilChanged(mSpeedType);
    }

    protected void updateSpeedType() {
        if (mSpeedType == null) {
            return;
        }
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSpeedType.setValue(SPEED_UNKNOWN);
            return;
        }
        int keyBand = config.getBand();
        logd("updateSpeedType(), getBand():" + keyBand);
        if (!is5gAvailable()) {
            keyBand &= ~BAND_5GHZ;
        }
        if (!is6gAvailable()) {
            keyBand &= ~BAND_6GHZ;
        }
        if ((keyBand & BAND_6GHZ) != 0) {
            keyBand = BAND_6GHZ;
        } else if (isDualBand() && is5gAvailable()) {
            keyBand = BAND_2GHZ_5GHZ;
        } else if ((keyBand & BAND_5GHZ) != 0) {
            keyBand = BAND_5GHZ;
        } else if ((keyBand & BAND_2GHZ) != 0) {
            keyBand = BAND_2GHZ;
        } else {
            keyBand = 0;
        }
        logd("updateSpeedType(), keyBand:" + keyBand);
        mSpeedType.setValue(sSpeedMap.get(keyBand));
    }

    protected boolean isDualBand() {
        if (mIsDualBand == null) {
            mIsDualBand = mWifiManager.isBridgedApConcurrencySupported();
            logd("isDualBand():" + mIsDualBand);
        }
        return mIsDualBand;
    }

    protected boolean is5gAvailable() {
        if (mIs5gAvailable == null) {
            // TODO(b/272450463): isBandAvailable(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS) will
            //  cause crash in the old model device, use a simple check to workaround it first.
            mIs5gAvailable = (mWifiManager.is5GHzBandSupported() && mCurrentCountryCode != null);
            logd("is5gAvailable():" + mIs5gAvailable);
        }
        return mIs5gAvailable;
    }

    protected boolean is6gAvailable() {
        if (mIs6gAvailable == null) {
            mIs6gAvailable = mWifiManager.is6GHzBandSupported()
                    && isBandAvailable(WifiScanner.WIFI_BAND_6_GHZ);
            logd("is6gAvailable():" + mIs6gAvailable);
        }
        return mIs6gAvailable;
    }

    /**
     * Return whether the Hotspot band is available or not.
     *
     * @param band one of the following band constants defined in {@code WifiScanner#WIFI_BAND_*}
     *             constants.
     *             1. {@code WifiScanner#WIFI_BAND_5_GHZ_WITH_DFS}
     *             2. {@code WifiScanner#WIFI_BAND_6_GHZ}
     */
    protected boolean isBandAvailable(int band) {
        List<WifiAvailableChannel> channels = mWifiManager.getUsableChannels(band, OP_MODE_SAP);
        return (channels != null && channels.size() > 0);
    }

    protected void purgeRefreshData() {
        mIsDualBand = null;
        mIs5gAvailable = null;
        mIs6gAvailable = null;
    }

    protected void startAutoRefresh() {
        if (mActiveCountryCodeChangedCallback != null) {
            return;
        }
        logd("startMonitorSoftApConfiguration()");
        mActiveCountryCodeChangedCallback = new ActiveCountryCodeChangedCallback();
        mWifiManager.registerActiveCountryCodeChangedCallback(mAppContext.getMainExecutor(),
                mActiveCountryCodeChangedCallback);
    }

    protected void stopAutoRefresh() {
        if (mActiveCountryCodeChangedCallback == null) {
            return;
        }
        logd("stopMonitorSoftApConfiguration()");
        mWifiManager.unregisterActiveCountryCodeChangedCallback(mActiveCountryCodeChangedCallback);
        mActiveCountryCodeChangedCallback = null;
    }

    protected class ActiveCountryCodeChangedCallback implements
            WifiManager.ActiveCountryCodeChangedCallback {
        @Override
        public void onActiveCountryCodeChanged(String country) {
            logd("onActiveCountryCodeChanged(), country:" + country);
            mCurrentCountryCode = country;
            purgeRefreshData();
            refresh();
        }

        @Override
        public void onCountryCodeInactive() {
            logd("onCountryCodeInactive()");
            mCurrentCountryCode = null;
            purgeRefreshData();
            refresh();
        }
    }

    private static void logd(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
