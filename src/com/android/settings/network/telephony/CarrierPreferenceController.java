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

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller for "Carrier Settings"
 */
public class CarrierPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    CarrierConfigManager mCarrierConfigManager;
    private int mSubId;

    public CarrierPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = new CarrierConfigManager(context);
    }

    public void init(int subId) {
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus() {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        // Return available if it is in CDMA or GSM mode, and the flag is on
        return carrierConfig != null
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL)
                && (MobileNetworkUtils.isCdmaOptions(mContext, mSubId)
                || MobileNetworkUtils.isGsmOptions(mContext, mSubId))
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            //TODO(b/117651939): start carrier settings activity
            return true;
        }

        return false;
    }
}
