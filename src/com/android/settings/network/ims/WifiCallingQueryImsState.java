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

import android.content.Context;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.SubscriptionManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import androidx.annotation.VisibleForTesting;

import com.android.ims.ImsManager;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.MobileNetworkUtils;

/**
 * Controller class for querying Wifi calling status
 */
public class WifiCallingQueryImsState extends ImsQueryController  {

    private Context mContext;
    private int mSubId;

    /**
     * Constructor
     *
     * @param context {@link Context}
     * @param subId subscription's id
     */
    public WifiCallingQueryImsState(Context context, int subId) {
        super(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN,
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
        mContext = context;
        mSubId = subId;
    }

    /**
     * Implementation of ImsQueryController#isEnabledByUser(int subId)
     */
    @VisibleForTesting
    boolean isEnabledByUser(int subId) {
        return (new ImsQueryWfcUserSetting(subId)).query();
    }

    @VisibleForTesting
    ImsManager getImsManager(int subId) {
        return ImsManager.getInstance(mContext,
                SubscriptionUtil.getPhoneId(mContext, subId));
    }

    /**
     * Check whether Wifi Calling has been provisioned or not on this subscription
     *
     * @return true when Wifi Calling has been enabled, otherwise false
     */
    public boolean isWifiCallingProvisioned() {
        final ImsManager imsManager = getImsManager(mSubId);
        if (imsManager == null) {
            return false;
        }

        return imsManager.isWfcEnabledByPlatform()
                && isProvisionedOnDevice(mSubId);
    }

    /**
     * Check whether Wifi Calling can be perform or not on this subscription
     *
     * @return true when Wifi Calling can be performed, otherwise false
     */
    public boolean isReadyToWifiCalling() {
        return isWifiCallingProvisioned()
                && MobileNetworkUtils.isImsServiceStateReady(getImsManager(mSubId));
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
}
