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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_DATA;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.network.CarrierConfigCache;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Preference controller for "Satellite Setting"
 */
public class SatelliteSettingPreferenceController extends
        TelephonyBasePreferenceController implements DefaultLifecycleObserver {
    private static final String TAG = "SatelliteSettingPreferenceController";
    @VisibleForTesting
    final CarrierRoamingNtnModeCallback mCarrierRoamingNtnModeCallback =
            new CarrierRoamingNtnModeCallback();
    CarrierConfigCache mCarrierConfigCache;
    SatelliteManager mSatelliteManager;
    private TelephonyManager mTelephonyManager = null;
    @Nullable
    private Boolean mIsSatelliteEligible = null;
    private boolean mIsServiceDataType = false;
    private boolean mIsSatelliteSmsAvailableForManualType = false;
    private boolean mIsCarrierSatelliteAttachSupported = false;
    private boolean mIsCarrierRoamingNtnConnectedTypeManual = false;

    public SatelliteSettingPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mSatelliteManager = context.getSystemService(SatelliteManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    private static void logd(String message) {
        Log.d(TAG, message);
    }

    private static void loge(String message) {
        Log.e(TAG, message);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!Flags.carrierEnabledSatelliteFlag()) {
            logd("getAvailabilityStatus() : carrierEnabledSatelliteFlag is disabled");
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mSatelliteManager == null) {
            return UNSUPPORTED_ON_DEVICE;
        }

        int availabilityStatus = mIsCarrierSatelliteAttachSupported
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        if (availabilityStatus == AVAILABLE && mIsCarrierRoamingNtnConnectedTypeManual
                && !mIsSatelliteSmsAvailableForManualType) {
            availabilityStatus = CONDITIONALLY_UNAVAILABLE;
        }
        return availabilityStatus;
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                    mCarrierRoamingNtnModeCallback);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            mTelephonyManager.unregisterTelephonyCallback(mCarrierRoamingNtnModeCallback);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        updateState(screen.findPreference(getPreferenceKey()));
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            mCarrierRoamingNtnModeCallback.mPref = preference;
            updateSummary(preference);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            // This activity runs in phone process, we must use intent to start
            final Intent intent = new Intent(Settings.ACTION_SATELLITE_SETTING)
                    .setPackage(mContext.getPackageName());
            // This will setup the Home and Search affordance
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            intent.putExtra(SatelliteSetting.SUB_ID, mSubId);
            intent.putExtra(SatelliteSetting.EXTRA_IS_SERVICE_DATA_TYPE, mIsServiceDataType);
            intent.putExtra(SatelliteSetting.EXTRA_IS_SMS_AVAILABLE_FOR_MANUAL_TYPE,
                    mIsSatelliteSmsAvailableForManualType);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }

    /**
     * Set subId for Satellite Settings page.
     *
     * @param subId subscription ID.
     */
    public void init(int subId) {
        logd("init(), subId=" + subId);
        mSubId = subId;
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);

        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        if (carrierConfig == null) {
            logd("init(), no carrier config data");
            return;
        }
        mIsCarrierSatelliteAttachSupported = carrierConfig.getBoolean(
                KEY_SATELLITE_ATTACH_SUPPORTED_BOOL);
        mIsCarrierRoamingNtnConnectedTypeManual =
                CARRIER_ROAMING_NTN_CONNECT_MANUAL == carrierConfig.getInt(
                        KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                        CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
    }

    private void updateSummary(Preference preference) {
        if (preference == null) {
            logd("updateSummary - no Preference");
            return;
        }
        if (mSatelliteManager == null) {
            logd("updateSummary - no SatelliteManager");
            return;
        }

        if (mIsCarrierRoamingNtnConnectedTypeManual) {
            preference.setSummary(mIsSatelliteSmsAvailableForManualType
                    ? R.string.satellite_setting_enabled_summary
                    : R.string.satellite_setting_disabled_summary);
        } else {
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
    }

    @VisibleForTesting
    class CarrierRoamingNtnModeCallback extends TelephonyCallback implements
            TelephonyCallback.CarrierRoamingNtnListener {
        Preference mPref = null;

        @Override
        public void onCarrierRoamingNtnAvailableServicesChanged(int[] availableServices) {
            CarrierRoamingNtnListener.super.onCarrierRoamingNtnAvailableServicesChanged(
                    availableServices);
            List<Integer> availableServicesList = Arrays.stream(availableServices).boxed().toList();
            boolean isSmsAvailable = availableServicesList.contains(SERVICE_TYPE_SMS);
            boolean isDataAvailable = availableServicesList.contains(SERVICE_TYPE_DATA);
            logd("isSmsAvailable : " + isSmsAvailable
                    + " / isDataAvailable " + isDataAvailable);
            if (mPref == null) {
                logd("Satellite preference is not initialized yet");
                return;
            }
            mIsServiceDataType = isDataAvailable;
            mIsSatelliteSmsAvailableForManualType = isSmsAvailable;
            mPref.setTitle(isDataAvailable ? R.string.title_satellite_setting_connectivity
                    : R.string.satellite_setting_title);
            updateSummary(mPref);
        }

        @Override
        public void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {
            // Do nothing
        }

        @Override
        public void onCarrierRoamingNtnModeChanged(boolean active) {
            // Do nothing
        }

        @Override
        public void onCarrierRoamingNtnSignalStrengthChanged(NtnSignalStrength ntnSignalStrength) {
            // Do nothing
        }
    }
}
