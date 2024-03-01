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
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.network.CarrierConfigCache;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Set;

/**
 * Preference controller for "Satellite Setting"
 */
public class SatelliteSettingPreferenceController extends
        TelephonyBasePreferenceController implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "SatelliteSettingPreferenceController";

    CarrierConfigCache mCarrierConfigCache;
    SatelliteManager mSatelliteManager;
    @Nullable private Boolean mIsSatelliteEligible = null;

    public SatelliteSettingPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mSatelliteManager = context.getSystemService(SatelliteManager.class);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!Flags.carrierEnabledSatelliteFlag()) {
            logd("getAvailabilityStatus() : carrierEnabledSatelliteFlag is disabled");
            return UNSUPPORTED_ON_DEVICE;
        }

        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        final boolean isSatelliteAttachSupported = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL);

        return isSatelliteAttachSupported ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            updateSummary(preference);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            // This activity runs in phone process, we must use intent to start
            final Intent intent = new Intent(Settings.ACTION_SATELLITE_SETTING);
            // This will setup the Home and Search affordance
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            intent.putExtra(SatelliteSetting.SUB_ID, mSubId);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }

    /**
     * Set subId for Satellite Settings page.
     * @param subId subscription ID.
     */
    public void init(int subId) {
        logd("init(), subId=" + subId);
        mSubId = subId;
    }

    private void updateSummary(Preference preference) {
        try {
            Set<Integer> restrictionReason =
                    mSatelliteManager.getAttachRestrictionReasonsForCarrier(mSubId);
            boolean isSatelliteEligible = !restrictionReason.contains(
                    SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT);
            if (mIsSatelliteEligible == null || mIsSatelliteEligible != isSatelliteEligible) {
                mIsSatelliteEligible = isSatelliteEligible;
                String summary = mContext.getString(
                        mIsSatelliteEligible ? R.string.satellite_setting_enabled_summary
                                : R.string.satellite_setting_disabled_summary);
                preference.setSummary(summary);
            }
        } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) {
            loge(ex.toString());
            preference.setSummary(R.string.satellite_setting_disabled_summary);
        }
    }

    private static void logd(String message) {
        Log.d(TAG, message);
    }

    private static void loge(String message) {
        Log.e(TAG, message);
    }
}
