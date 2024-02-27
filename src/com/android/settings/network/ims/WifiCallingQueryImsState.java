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
import android.telephony.ims.ImsException;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.network.telephony.wificalling.WifiCallingRepository;

/**
 * Controller class for querying Wifi calling status
 */
public class WifiCallingQueryImsState extends ImsQueryController  {

    private static final String LOG_TAG = "WifiCallingQueryImsState";

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
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        return (new ImsQueryWfcUserSetting(subId)).query();
    }

    /**
     * Check whether Wifi Calling is a supported feature on this subscription
     *
     * @return true when Wifi Calling is a supported feature, otherwise false
     */
    public boolean isWifiCallingSupported() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        try {
            return isEnabledByPlatform(mSubId);
        } catch (InterruptedException | IllegalArgumentException | ImsException exception) {
            Log.w(LOG_TAG, "fail to get WFC supporting status. subId=" + mSubId, exception);
        }
        return false;
    }

    /**
     * Check whether Wifi Calling has been provisioned or not on this subscription
     *
     * @return true when Wifi Calling has been enabled, otherwise false
     */
    public boolean isWifiCallingProvisioned() {
        return isWifiCallingSupported() && isProvisionedOnDevice(mSubId);
    }

    /**
     * Check whether Wifi Calling can be perform or not on this subscription
     *
     * @return true when Wifi Calling can be performed, otherwise false
     * @deprecated Use {@link WifiCallingRepository#wifiCallingReadyFlow()} instead.
     */
    @Deprecated
    public boolean isReadyToWifiCalling() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        if (!isWifiCallingProvisioned()) {
            return false;
        }
        try {
            return isServiceStateReady(mSubId);
        } catch (InterruptedException | IllegalArgumentException | ImsException exception) {
            Log.w(LOG_TAG, "fail to get WFC service status. subId=" + mSubId, exception);
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
}
