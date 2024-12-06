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

import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_DATA;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import android.content.Context;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.satellite.NtnSignalStrength;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.flags.Flags;

import java.util.Arrays;
import java.util.List;

/** Preference controller for Satellite functions in mobile network settings. */
public class SatelliteSettingsPreferenceCategoryController
        extends TelephonyBasePreferenceController implements DefaultLifecycleObserver {
    private static final String TAG = "SatelliteSettingsPrefCategoryCon";

    private PreferenceCategory mPreferenceCategory;
    private TelephonyManager mTelephonyManager = null;

    @VisibleForTesting
    final CarrierRoamingNtnModeCallback mCarrierRoamingNtnModeCallback =
            new CarrierRoamingNtnModeCallback();

    public SatelliteSettingsPreferenceCategoryController(Context context, String key) {
        super(context, key);
        setAvailabilityStatus(UNSUPPORTED_ON_DEVICE);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    /**
     * Set subId for Satellite Settings category .
     *
     * @param subId subscription ID.
     */
    public void init(int subId) {
        Log.d(TAG, "init(), subId=" + subId);
        mSubId = subId;
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory.getPreferenceCount() > 0) {
            for (int i = 0; i < mPreferenceCategory.getPreferenceCount(); i++) {
                if (mPreferenceCategory.getPreference(i).isVisible()) {
                    setAvailabilityStatus(AVAILABLE_UNSEARCHABLE);
                    break;
                }
            }
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return isAvailable() ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (Flags.satelliteOemSettingsUxMigration()) {
            mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                    mCarrierRoamingNtnModeCallback);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (Flags.satelliteOemSettingsUxMigration()) {
            mTelephonyManager.unregisterTelephonyCallback(mCarrierRoamingNtnModeCallback);
        }
    }

    @VisibleForTesting
    class CarrierRoamingNtnModeCallback extends TelephonyCallback implements
            TelephonyCallback.CarrierRoamingNtnModeListener {
        @Override
        public void onCarrierRoamingNtnAvailableServicesChanged(int[] availableServices) {
            CarrierRoamingNtnModeListener.super.onCarrierRoamingNtnAvailableServicesChanged(
                    availableServices);
            List<Integer> availableServicesList = Arrays.stream(availableServices).boxed().toList();
            boolean isSmsAvailable = availableServicesList.contains(SERVICE_TYPE_SMS);
            boolean isDataAvailable = availableServicesList.contains(SERVICE_TYPE_DATA);
            Log.i(TAG, "isSmsAvailable : " + isSmsAvailable
                    + " / isDataAvailable " + isDataAvailable);
            if (mPreferenceCategory == null) {
                Log.d(TAG, "Satellite preference category is not initialized yet");
                return;
            }
            if (isDataAvailable) {
                mPreferenceCategory.setTitle(R.string.category_title_satellite_connectivity);
            } else if (isSmsAvailable) {
                mPreferenceCategory.setTitle(R.string.satellite_setting_title);
            }
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
