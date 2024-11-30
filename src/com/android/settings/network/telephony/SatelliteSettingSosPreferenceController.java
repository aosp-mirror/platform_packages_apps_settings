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

import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ESOS_SUPPORTED_BOOL;

import android.content.Context;
import android.os.PersistableBundle;

import com.android.settings.flags.Flags;
import com.android.settings.network.CarrierConfigCache;

/** A controller for Satellite SOS entry preference. */
public class SatelliteSettingSosPreferenceController extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteSettingSosPrefController";

    public SatelliteSettingSosPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Setup the subscription Id for the UI with specific UI group. */
    public void init(int subId) {
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (Flags.satelliteOemSettingsUxMigration()) {
            CarrierConfigCache carrierConfigCache = CarrierConfigCache.getInstance(mContext);
            PersistableBundle bundle = carrierConfigCache.getConfigForSubId(subId);
            if (bundle == null) {
                return CONDITIONALLY_UNAVAILABLE;
            }
            boolean isCarrierSupport = bundle.getBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL);
            return isCarrierSupport ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
