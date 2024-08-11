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

package com.android.settings.network.ims;

import static android.telephony.ims.ProvisioningManager.KEY_VOIMS_OPT_IN_STATUS;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

/**
 * Controller class for querying Volte status
 */
public class VolteQueryImsState extends ImsQueryController {

    private static final String LOG_TAG = "VolteQueryImsState";

    private Context mContext;
    private int mSubId;

    /**
     * Constructor
     *
     * @param context {@link Context}
     * @param subId subscription's id
     */
    public VolteQueryImsState(Context context, int subId) {
        super(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        mContext = context;
        mSubId = subId;
    }

    /**
     * Implementation of ImsQueryController#isEnabledByUser(int subId)
     */
    @VisibleForTesting
    boolean isEnabledByUser(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        return (new ImsQueryEnhanced4gLteModeUserSetting(subId)).query();
    }

    /**
     * Check whether VoLTE has been provisioned or not on this subscription
     *
     * @return true when VoLTE has been enabled, otherwise false
     */
    public boolean isVoLteProvisioned() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        if (!isProvisionedOnDevice(mSubId)) {
            return false;
        }
        try {
            return isEnabledByPlatform(mSubId);
        } catch (InterruptedException | IllegalArgumentException | ImsException exception) {
            Log.w(LOG_TAG, "fail to get VoLte supporting status. subId=" + mSubId, exception);
        }
        return false;
    }

    /**
     * Check whether VoLTE can be perform or not on this subscription
     *
     * @return true when VoLTE can be performed, otherwise false
     */
    public boolean isReadyToVoLte() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        if (!isVoLteProvisioned()) {
            return false;
        }
        try {
            return isServiceStateReady(mSubId);
        } catch (InterruptedException | IllegalArgumentException | ImsException exception) {
            Log.w(LOG_TAG, "fail to get VoLte service status. subId=" + mSubId, exception);
        }
        return false;
    }

    /**
     * Get allowance status for user to alter configuration
     *
     * @return true when changing configuration by user is allowed.
     */
    public boolean isAllowUserControl() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }

        return ((!isTtyEnabled(mContext))
                || (isTtyOnVolteEnabled(mSubId)));
    }

    @VisibleForTesting
    boolean isTtyEnabled(Context context) {
        final TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        return (telecomManager.getCurrentTtyMode() != TelecomManager.TTY_MODE_OFF);
    }

    /**
     * Get user's configuration
     *
     * @return true when user's configuration is ON otherwise false.
     */
    public boolean isEnabledByUser() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        return isEnabledByUser(mSubId);
    }

    /**
     * Get VoIMS opt-in configuration.
     *
     * @return true when VoIMS opt-in has been enabled, otherwise false
     */
    public boolean isVoImsOptInEnabled() {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS)) {
            // If the device does not have PackageManager.FEATURE_TELEPHONY_IMS,
            // ProvisioningManager.getProvisioningIntValue() could not be called.
            return false;
        }
        int voImsOptInStatus = ProvisioningManager.createForSubscriptionId(mSubId)
                .getProvisioningIntValue(KEY_VOIMS_OPT_IN_STATUS);
        return voImsOptInStatus == ProvisioningManager.PROVISIONING_VALUE_ENABLED;
    }
}
