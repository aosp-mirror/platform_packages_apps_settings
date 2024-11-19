/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.network.CarrierConfigCache;

/** Preference controller for Satellite functions in mobile network settings.*/
public class SatelliteSettingsPreferenceCategoryController
        extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteSettingsPrefCategoryCon";

    private CarrierConfigCache mCarrierConfigCache;
    private SatelliteManager mSatelliteManager;

    public SatelliteSettingsPreferenceCategoryController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mSatelliteManager = context.getSystemService(SatelliteManager.class);
    }

    /**
     * Set subId for Satellite Settings category .
     * @param subId subscription ID.
     */
    public void init(int subId) {
        Log.d(TAG, "init(), subId=" + subId);
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!Flags.carrierEnabledSatelliteFlag()) {
            Log.d(TAG, "getAvailabilityStatus(" + subId + ") : carrierEnabledSatelliteFlag "
                    + "is disabled");
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mSatelliteManager == null) {
            Log.d(TAG, "getAvailabilityStatus(" + subId + ") : SatelliteManager is null");
            return UNSUPPORTED_ON_DEVICE;
        }

        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        final boolean isSatelliteAttachSupported = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL);

        return isSatelliteAttachSupported ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
