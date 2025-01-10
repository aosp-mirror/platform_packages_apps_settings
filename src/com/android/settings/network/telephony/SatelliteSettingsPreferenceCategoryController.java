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

import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ESOS_SUPPORTED_BOOL;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.network.CarrierConfigCache;

/** Preference controller for Satellite functions in mobile network settings. */
public class SatelliteSettingsPreferenceCategoryController
        extends TelephonyBasePreferenceController implements DefaultLifecycleObserver {
    private static final String TAG = "SatelliteSettingsPrefCategoryCon";

    private CarrierConfigCache mCarrierConfigCache;
    private SatelliteManager mSatelliteManager;
    private PreferenceCategory mPreferenceCategory;

    public SatelliteSettingsPreferenceCategoryController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mSatelliteManager = context.getSystemService(SatelliteManager.class);
    }

    /**
     * Set subId for Satellite Settings category .
     *
     * @param subId subscription ID.
     */
    public void init(int subId) {
        Log.d(TAG, "init(), subId=" + subId);
        mSubId = subId;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mPreferenceCategory.setTitle(R.string.category_title_satellite_connectivity);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!com.android.internal.telephony.flags.Flags.carrierEnabledSatelliteFlag()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mSatelliteManager == null) {
            return UNSUPPORTED_ON_DEVICE;
        }

        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        final boolean isSatelliteAttachSupported = carrierConfig.getBoolean(
                KEY_SATELLITE_ATTACH_SUPPORTED_BOOL);
        boolean isSatelliteSosSupported = false;
        if (Flags.satelliteOemSettingsUxMigration()) {
            isSatelliteSosSupported = carrierConfig.getBoolean(
                    KEY_SATELLITE_ESOS_SUPPORTED_BOOL);
        }

        return (isSatelliteAttachSupported || isSatelliteSosSupported)
                ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }
}
