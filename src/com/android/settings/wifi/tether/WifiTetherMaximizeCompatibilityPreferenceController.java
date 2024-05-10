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
import android.net.wifi.SoftApConfiguration;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

/**
 * This controller helps to manage the state of maximize compatibility switch preference.
 */
public class WifiTetherMaximizeCompatibilityPreferenceController extends
        WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherMaximizeCompatibilityPref";
    public static final String PREF_KEY = "wifi_tether_maximize_compatibility";

    private boolean mIsChecked;
    @VisibleForTesting
    boolean mShouldHidePreference;

    public WifiTetherMaximizeCompatibilityPreferenceController(Context context,
            WifiTetherBasePreferenceController.OnTetherConfigUpdateListener listener) {
        super(context, listener);
        // If the Wi-Fi Hotspot Speed Feature available, then hide this controller.
        mShouldHidePreference = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository().isSpeedFeatureAvailable();
        Log.d(TAG, "mShouldHidePreference:" + mShouldHidePreference);
        if (mShouldHidePreference) {
            return;
        }
        mIsChecked = isMaximizeCompatibilityEnabled();
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
        if (mPreference == null) {
            return;
        }
        mPreference.setEnabled(is5GhzBandSupported());
        ((TwoStatePreference) mPreference).setChecked(mIsChecked);
        mPreference.setSummary(mWifiManager.isBridgedApConcurrencySupported()
                ? R.string.wifi_hotspot_maximize_compatibility_dual_ap_summary
                : R.string.wifi_hotspot_maximize_compatibility_single_ap_summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mIsChecked = (Boolean) newValue;
        if (mListener != null) {
            mListener.onTetherConfigUpdated(this);
        }
        return true;
    }

    private boolean is5GhzBandSupported() {
        if (mWifiManager == null) {
            return false;
        }
        if (!mWifiManager.is5GHzBandSupported() || mWifiManager.getCountryCode() == null) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    boolean isMaximizeCompatibilityEnabled() {
        if (mWifiManager == null) {
            return false;
        }
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            return false;
        }
        if (mWifiManager.isBridgedApConcurrencySupported()) {
            final boolean isEnabled = config.isBridgedModeOpportunisticShutdownEnabled();
            Log.d(TAG, "isBridgedModeOpportunisticShutdownEnabled:" + isEnabled);
            // Because the return value defined by the Wi-Fi framework API is opposite to the UI.
            //   Compatibility on:  isBridgedModeOpportunisticShutdownEnabled() = false
            //   Compatibility off: isBridgedModeOpportunisticShutdownEnabled() = true
            // Need to return the reverse value.
            return !isEnabled;
        }

        // If the BridgedAp Concurrency is not supported in early Pixel devices (e.g. Pixel 2~5),
        // show toggle on when band is 2.4G only.
        final int band = config.getBand();
        Log.d(TAG, "getBand:" + band);
        return band == SoftApConfiguration.BAND_2GHZ;
    }

    /**
     * Setup the Maximize Compatibility setting to the SoftAp Configuration
     *
     * @param builder The builder to build the SoftApConfiguration.
     */
    public void setupMaximizeCompatibility(SoftApConfiguration.Builder builder) {
        if (builder == null) {
            return;
        }
        final boolean enabled = mIsChecked;
        if (mWifiManager.isBridgedApConcurrencySupported()) {
            int[] bands = {
                    SoftApConfiguration.BAND_2GHZ,
                    SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ};
            builder.setBands(bands);
            Log.d(TAG, "setBridgedModeOpportunisticShutdownEnabled:" + enabled);
            // Because the defined value by the Wi-Fi framework API is opposite to the UI.
            //   Compatibility on:  setBridgedModeOpportunisticShutdownEnabled(false)
            //   Compatibility off: setBridgedModeOpportunisticShutdownEnabled(true)
            // Need to set the reverse value.
            builder.setBridgedModeOpportunisticShutdownEnabled(!enabled);
        } else {
            int band = enabled
                    ? SoftApConfiguration.BAND_2GHZ
                    : SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ;
            Log.d(TAG, "setBand:" + band);
            builder.setBand(band);
        }
    }
}
