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

import com.android.settings.overlay.FeatureFactory;

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
    protected Boolean mIs5gBandSupported;
    protected Boolean mIs5gAvailable;
    protected MutableLiveData<Boolean> m5gAvailable;
    protected Boolean mIs6gBandSupported;
    protected Boolean mIs6gAvailable;
    protected MutableLiveData<Boolean> m6gAvailable;
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
        update6gAvailable();
        update5gAvailable();
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
            log("getSpeedType():" + mSpeedType.getValue());
        }
        return mSpeedType;
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
        log("updateSpeedType(), getBand():" + keyBand);
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
        log("updateSpeedType(), keyBand:" + keyBand);
        mSpeedType.setValue(sSpeedMap.get(keyBand));
    }

    /**
     * Sets SpeedType
     *
     * @param speedType the Wi-Fi hotspot speed type.
     */
    public void setSpeedType(int speedType) {
        log("setSpeedType():" + speedType);
        if (mSpeedType == null) {
            getSpeedType();
        }
        if (speedType == mSpeedType.getValue()) {
            Log.w(TAG, "setSpeedType() is no changed! mSpeedType:" + mSpeedType.getValue());
            return;
        }
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSpeedType.setValue(SPEED_UNKNOWN);
            Log.e(TAG, "setSpeedType(), WifiManager#getSoftApConfiguration() return null!");
            return;
        }
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder(config);
        if (speedType == SPEED_6GHZ) {
            log("setSpeedType(), setBand(BAND_2GHZ_5GHZ_6GHZ)");
            configBuilder.setBand(BAND_2GHZ_5GHZ_6GHZ);
        } else if (speedType == SPEED_5GHZ) {
            log("setSpeedType(), setBand(BAND_2GHZ_5GHZ)");
            configBuilder.setBand(BAND_2GHZ_5GHZ);
        } else if (mIsDualBand) {
            log("setSpeedType(), setBands(BAND_2GHZ + BAND_2GHZ_5GHZ)");
            int[] bands = {BAND_2GHZ, BAND_2GHZ_5GHZ};
            configBuilder.setBands(bands);
        } else {
            log("setSpeedType(), setBand(BAND_2GHZ)");
            configBuilder.setBand(BAND_2GHZ);
        }
        setSoftApConfiguration(configBuilder.build());
    }

    /**
     * Return whether Wi-Fi Dual Band is supported or not.
     * @return {@code true} if Wi-Fi Dual Band is supported
     */
    public boolean isDualBand() {
        if (mIsDualBand == null) {
            mIsDualBand = mWifiManager.isBridgedApConcurrencySupported();
            log("isDualBand():" + mIsDualBand);
        }
        return mIsDualBand;
    }

    /**
     * Return whether Wi-Fi 5 GHz band is supported or not.
     * @return {@code true} if Wi-Fi 5 GHz Band is supported
     */
    public boolean is5GHzBandSupported() {
        if (mIs5gBandSupported == null) {
            mIs5gBandSupported = mWifiManager.is5GHzBandSupported();
            log("is5GHzBandSupported():" + mIs5gBandSupported);
        }
        return mIs5gBandSupported;
    }

    /**
     * Return whether Wi-Fi Hotspot 5 GHz band is available or not.
     * @return {@code true} if Wi-Fi Hotspot 5 GHz Band is available
     */
    public boolean is5gAvailable() {
        if (mIs5gAvailable == null) {
            // If Settings is unable to get available 5GHz SAP information, Wi-Fi Framework's
            // proposal is to assume that 5GHz is available. (See b/272450463#comment16)
            mIs5gAvailable = is5GHzBandSupported()
                    && isChannelAvailable(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS,
                    true /* defaultValue */);
            log("is5gAvailable():" + mIs5gAvailable);
        }
        return mIs5gAvailable;
    }

    /**
     * Gets is5gAvailable LiveData
     */
    public LiveData<Boolean> get5gAvailable() {
        if (m5gAvailable == null) {
            m5gAvailable = new MutableLiveData<>();
            m5gAvailable.setValue(is5gAvailable());
        }
        return m5gAvailable;
    }

    protected void update5gAvailable() {
        if (m5gAvailable != null) {
            m5gAvailable.setValue(is5gAvailable());
        }
    }

    /**
     * Return whether Wi-Fi 6 GHz band is supported or not.
     * @return {@code true} if Wi-Fi 6 GHz Band is supported
     */
    public boolean is6GHzBandSupported() {
        if (mIs6gBandSupported == null) {
            mIs6gBandSupported = mWifiManager.is6GHzBandSupported();
            log("is6GHzBandSupported():" + mIs6gBandSupported);
        }
        return mIs6gBandSupported;
    }

    /**
     * Return whether Wi-Fi Hotspot 6 GHz band is available or not.
     * @return {@code true} if Wi-Fi Hotspot 6 GHz Band is available
     */
    public boolean is6gAvailable() {
        if (mIs6gAvailable == null) {
            mIs6gAvailable = is6GHzBandSupported()
                    && isChannelAvailable(WifiScanner.WIFI_BAND_6_GHZ, false /* defaultValue */);
            log("is6gAvailable():" + mIs6gAvailable);
        }
        return mIs6gAvailable;
    }

    /**
     * Gets is6gAvailable LiveData
     */
    public LiveData<Boolean> get6gAvailable() {
        if (m6gAvailable == null) {
            m6gAvailable = new MutableLiveData<>();
            m6gAvailable.setValue(is6gAvailable());
        }
        return m6gAvailable;
    }

    protected void update6gAvailable() {
        if (m6gAvailable != null) {
            m6gAvailable.setValue(is6gAvailable());
        }
    }

    /**
     * Return whether the Hotspot channel is available or not.
     *
     * @param band         one of the following band constants defined in
     *                     {@code WifiScanner#WIFI_BAND_*} constants.
     *                     1. {@code WifiScanner#WIFI_BAND_5_GHZ_WITH_DFS}
     *                     2. {@code WifiScanner#WIFI_BAND_6_GHZ}
     * @param defaultValue returns the default value if WifiManager#getUsableChannels is
     *                     unavailable to get the SAP information.
     */
    protected boolean isChannelAvailable(int band, boolean defaultValue) {
        try {
            List<WifiAvailableChannel> channels = mWifiManager.getUsableChannels(band, OP_MODE_SAP);
            log("isChannelAvailable(), band:" + band + ", channels:" + channels);
            return (channels != null && channels.size() > 0);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Querying usable SAP channels failed, band:" + band);
        } catch (UnsupportedOperationException e) {
            // This is expected on some hardware.
            Log.e(TAG, "Querying usable SAP channels is unsupported, band:" + band);
        }
        return defaultValue;
    }

    protected void purgeRefreshData() {
        mIs5gAvailable = null;
        mIs6gAvailable = null;
    }

    protected void startAutoRefresh() {
        if (mActiveCountryCodeChangedCallback != null) {
            return;
        }
        log("startMonitorSoftApConfiguration()");
        mActiveCountryCodeChangedCallback = new ActiveCountryCodeChangedCallback();
        mWifiManager.registerActiveCountryCodeChangedCallback(mAppContext.getMainExecutor(),
                mActiveCountryCodeChangedCallback);
    }

    protected void stopAutoRefresh() {
        if (mActiveCountryCodeChangedCallback == null) {
            return;
        }
        log("stopMonitorSoftApConfiguration()");
        mWifiManager.unregisterActiveCountryCodeChangedCallback(mActiveCountryCodeChangedCallback);
        mActiveCountryCodeChangedCallback = null;
    }

    protected class ActiveCountryCodeChangedCallback implements
            WifiManager.ActiveCountryCodeChangedCallback {
        @Override
        public void onActiveCountryCodeChanged(String country) {
            log("onActiveCountryCodeChanged(), country:" + country);
            mCurrentCountryCode = country;
            purgeRefreshData();
            refresh();
        }

        @Override
        public void onCountryCodeInactive() {
            log("onCountryCodeInactive()");
            mCurrentCountryCode = null;
            purgeRefreshData();
            refresh();
        }
    }

    private void log(String msg) {
        FeatureFactory.getFactory(mAppContext).getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
