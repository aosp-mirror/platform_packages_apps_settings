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

package com.android.settings.security;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class SimLockPreferenceController extends BasePreferenceController {

    private static final String KEY_SIM_LOCK = "sim_lock_settings";

    private final CarrierConfigManager mCarrierConfigManager;
    private final UserManager mUserManager;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public SimLockPreferenceController(Context context) {
        super(context, KEY_SIM_LOCK);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mCarrierConfigManager = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        mSubscriptionManager = (SubscriptionManager) context
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        final PersistableBundle b = mCarrierConfigManager.getConfig();
        final boolean IsAdmin = mUserManager.isAdminUser();
        if (!IsAdmin || !isSimIccReady() ||
                b.getBoolean(CarrierConfigManager.KEY_HIDE_SIM_LOCK_SETTINGS_BOOL)) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }
        // Disable SIM lock if there is no ready SIM card.
        preference.setEnabled(isSimReady());
    }

    /* Return true if a SIM is ready for locking.
     * TODO: consider adding to TelephonyManager or SubscritpionManasger.
     */
    private boolean isSimReady() {
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList(true);
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                final int simState = mTelephonyManager.getSimState(subInfo.getSimSlotIndex());
                if ((simState != TelephonyManager.SIM_STATE_ABSENT) &&
                        (simState != TelephonyManager.SIM_STATE_UNKNOWN)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return true if a there is a Slot that has Icc
     */
    private boolean isSimIccReady() {
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList(true);

        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (mTelephonyManager.hasIccCard(subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
        }
        return false;
    }
}
