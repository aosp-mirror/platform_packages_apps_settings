/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

/**
 * List of Phone-specific settings screens.
 */
public class CdmaOptions {
    private static final String LOG_TAG = "CdmaOptions";

    private CarrierConfigManager mCarrierConfigManager;
    private Preference mButtonCarrierSettings;

    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";

    private PreferenceFragmentCompat mPrefFragment;
    private PreferenceScreen mPrefScreen;
    private int mSubId;

    public CdmaOptions(PreferenceFragmentCompat prefFragment, PreferenceScreen prefScreen,
            int subId) {
        mPrefFragment = prefFragment;
        mPrefScreen = prefScreen;
        mPrefFragment.addPreferencesFromResource(R.xml.cdma_options);
        mCarrierConfigManager = new CarrierConfigManager(prefFragment.getContext());

        // Initialize preferences.
        mButtonCarrierSettings = mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);

        updateSubscriptionId(subId);
    }

    protected void updateSubscriptionId(int subId) {
        mSubId = subId;

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        // Read platform settings for carrier settings
        boolean addCarrierSettings =
                carrierConfig.getBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL);

        // Making no assumptions of whether they are added or removed at this point.
        // Calling add or remove explicitly to make sure they are updated.


        if (addCarrierSettings) {
            mPrefScreen.addPreference(mButtonCarrierSettings);
        } else {
            mPrefScreen.removePreference(mButtonCarrierSettings);
        }
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        }
        if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
        return false;
    }

    protected void log(String s) {
        android.util.Log.d(LOG_TAG, s);
    }
}
