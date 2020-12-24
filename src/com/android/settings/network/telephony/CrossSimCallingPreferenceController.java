/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import java.util.Objects;

/**
 * Preference controller for "Cross SIM Calling"
 **/
public class CrossSimCallingPreferenceController extends TelephonyTogglePreferenceController {

    private static final String LOG_TAG = "CrossSimCallingPrefCtrl";

    private int mSubId;
    private Preference mPreference;

    /**
     * Class constructor of cross sim calling.
     *
     * @param context of settings
     * @param key assigned within UI entry of XML file
     **/
    public CrossSimCallingPreferenceController(Context context, String key) {
        super(context, key);
    }

    /**
     * Initialization based on given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     **/
    public CrossSimCallingPreferenceController init(int subId) {
        mSubId = subId;
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return hasCrossSimCallingFeature(subId) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Implementation of abstract methods
     **/
    public boolean setChecked(boolean isChecked) {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
        if (imsMmTelMgr == null) {
            return false;
        }
        try {
            imsMmTelMgr.setCrossSimCallingEnabled(isChecked);
        } catch (ImsException exception) {
            Log.w(LOG_TAG, "fail to change cross SIM calling configuration: " + isChecked,
                    exception);
            return false;
        }
        return true;
    }

    /**
     * Implementation of abstract methods
     **/
    public boolean isChecked() {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
        if (imsMmTelMgr == null) {
            return false;
        }
        try {
            return imsMmTelMgr.isCrossSimCallingEnabledByUser();
        } catch (ImsException exception) {
            Log.w(LOG_TAG, "fail to get cross SIM calling configuration", exception);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if ((preference == null) || (!(preference instanceof SwitchPreference))) {
            return;
        }
        mPreference = preference;

        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setChecked(isChecked());

        updateSummary(getLatestSummary());
    }

    private String getLatestSummary() {
        SubscriptionInfo subInfo = getSubscriptionInfo();
        return Objects.toString((subInfo == null) ? null : subInfo.getDisplayName(), "");
    }

    private void updateSummary(String displayName) {
        Preference preference = mPreference;
        if (preference == null) {
            return;
        }
        String summary = displayName;
        String finalText = String.format(
                getResourcesForSubId().getString(R.string.cross_sim_calling_setting_summary),
                summary)
                .toString();
        preference.setSummary(finalText);
    }

    private boolean hasCrossSimCallingFeature(int subscriptionId) {
        PersistableBundle carrierConfig = getCarrierConfigForSubId(subscriptionId);
        return (carrierConfig != null)
                && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, false);
    }

    private ImsMmTelManager getImsMmTelManager() {
        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return null;
        }
        ImsManager imsMgr = mContext.getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(mSubId);
    }

    private SubscriptionInfo getSubscriptionInfo() {
        SubscriptionManager subInfoMgr = mContext.getSystemService(SubscriptionManager.class);
        if (subInfoMgr == null) {
            return null;
        }
        return subInfoMgr.getActiveSubscriptionInfo(mSubId);
    }
}
