/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for logic pertaining to the security type of Wi-Fi tethering.
 */
public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController
        implements WifiManager.SoftApCallback {

    private static final String PREF_KEY = "wifi_tether_security";

    private Map<Integer, String> mSecurityMap = new LinkedHashMap<Integer, String>();
    private int mSecurityValue;
    @VisibleForTesting
    boolean mIsWpa3Supported = true;
    @VisibleForTesting
    boolean mShouldHidePreference;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        // If the Wi-Fi Hotspot Speed Feature available, then hide this controller.
        mShouldHidePreference = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository().isSpeedFeatureAvailable();
        Log.d(TAG, "shouldHidePreference():" + mShouldHidePreference);
        if (mShouldHidePreference) {
            return;
        }
        final String[] securityNames = mContext.getResources().getStringArray(
                R.array.wifi_tether_security);
        final String[] securityValues = mContext.getResources().getStringArray(
                R.array.wifi_tether_security_values);
        for (int i = 0; i < securityNames.length; i++) {
            mSecurityMap.put(Integer.parseInt(securityValues[i]), securityNames[i]);
        }
        mWifiManager.registerSoftApCallback(context.getMainExecutor(), this);
    }

    @Override
    public boolean isAvailable() {
        if (mShouldHidePreference) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        // The mPreference will be ready when the fragment calls displayPreference(). Since the
        // capability of WPA3 hotspot callback will update the preference list here, add null point
        // checking to avoid the mPreference is not ready when the fragment is loading for settings
        // keyword searching only.
        if (mPreference == null) {
            return;
        }
        final ListPreference preference = (ListPreference) mPreference;
        // If the device is not support WPA3 then remove the WPA3 options.
        if (!mIsWpa3Supported && mSecurityMap.keySet()
                .removeIf(key -> key > SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
        }

        final int securityType = mWifiManager.getSoftApConfiguration().getSecurityType();
        mSecurityValue = mSecurityMap.get(securityType) != null
                ? securityType : SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;

        preference.setSummary(mSecurityMap.get(mSecurityValue));
        preference.setValue(String.valueOf(mSecurityValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSecurityValue = Integer.parseInt((String) newValue);
        preference.setSummary(mSecurityMap.get(mSecurityValue));
        if (mListener != null) {
            mListener.onTetherConfigUpdated(this);
        }
        return true;
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        final boolean isWpa3Supported =
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_SAE);
        if (!isWpa3Supported) {
            Log.i(PREF_KEY, "WPA3 SAE is not supported on this device");
        }
        if (mIsWpa3Supported != isWpa3Supported) {
            mIsWpa3Supported = isWpa3Supported;
            updateDisplay();
        }
        mWifiManager.unregisterSoftApCallback(this);
    }

    public int getSecurityType() {
        return mSecurityValue;
    }
}
