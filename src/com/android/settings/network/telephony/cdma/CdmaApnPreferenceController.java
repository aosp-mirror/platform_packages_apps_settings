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

package com.android.settings.network.telephony.cdma;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.network.ApnSettings;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * Preference controller for "CDMA Apn"
 */
public class CdmaApnPreferenceController extends CdmaBasePreferenceController {

    private static final String CATEGORY_KEY = "category_cdma_apn_key";
    @VisibleForTesting
    CarrierConfigManager mCarrierConfigManager;

    public CdmaApnPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = new CarrierConfigManager(context);
    }

    @Override
    public int getAvailabilityStatus() {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        return carrierConfig != null
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_APN_SETTING_CDMA_BOOL)
                && MobileNetworkUtils.isCdmaOptions(mContext, mSubId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            ((RestrictedPreference) mPreference).setDisabledByAdmin(
                    MobileNetworkUtils.isDpcApnEnforced(mContext)
                            ? RestrictedLockUtilsInternal.getDeviceOwner(mContext)
                            : null);
        } else {
            screen.findPreference(CATEGORY_KEY).setVisible(false);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            // This activity runs in phone process, we must use intent to start
            final Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
            // This will setup the Home and Search affordance
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            intent.putExtra(ApnSettings.SUB_ID, mSubId);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }
}
