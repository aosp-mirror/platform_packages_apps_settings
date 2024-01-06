/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SubscriptionManager;

import androidx.annotation.VisibleForTesting;

import com.android.settings.network.ResetNetworkOperationBuilder;

/**
 * A request which contains options required for resetting network.
 */
public class ResetNetworkRequest {

    /* Reset option - nothing get reset */
    public static final int RESET_NONE = 0x00;

    /* Reset option - reset ConnectivityManager */
    public static final int RESET_CONNECTIVITY_MANAGER = 0x01;

    /* Reset option - reset VpnManager */
    public static final int RESET_VPN_MANAGER = 0x02;

    /* Reset option - reset WiFiManager */
    public static final int RESET_WIFI_MANAGER = 0x04;

    /* Reset option - reset WifiP2pManager */
    public static final int RESET_WIFI_P2P_MANAGER = 0x08;

    /* Reset option - reset BluetoothManager */
    public static final int RESET_BLUETOOTH_MANAGER = 0x10;

    /* Reset option - reset IMS stack */
    public static final int RESET_IMS_STACK = 0x20;

    /* Reset option - reset phone process */
    public static final int RESET_PHONE_PROCESS = 0x40;

    /* Reset option - reset RILD */
    public static final int RESET_RILD = 0x80;

    /**
     *  Subscription ID indicates NOT resetting any of the components below:
     *  - TelephonyAndNetworkPolicy
     *  - APN
     *  - IMS
     */
    public static final int INVALID_SUBSCRIPTION_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /**
     *  Subscription ID indicates resetting components below for ALL subscriptions:
     *  - TelephonyAndNetworkPolicy
     *  - APN
     *  - IMS
     */
    public static final int ALL_SUBSCRIPTION_ID = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

    /* Key within Bundle. To store some connectivity options for reset */
    @VisibleForTesting
    protected static final String KEY_RESET_OPTIONS = "resetNetworkOptions";

    /* Key within Bundle. To store package name for resetting eSIM */
    @VisibleForTesting
    protected static final String KEY_ESIM_PACKAGE = "resetEsimPackage";

    /**
     * Key within Bundle. To store subscription ID for resetting
     * telephony manager and network and network policy manager.
     */
    @VisibleForTesting
    protected static final String KEY_TELEPHONY_NET_POLICY_MANAGER_SUBID =
            "resetTelephonyNetPolicySubId";

    /* Key within Bundle. To store subscription ID for resetting APN. */
    @VisibleForTesting
    protected static final String KEY_APN_SUBID = "resetApnSubId";

    /** Key within Bundle. To store subscription ID for resetting IMS. */
    protected  static final String KEY_RESET_IMS_SUBID = "resetImsSubId";

    private int mResetOptions = RESET_NONE;
    private String mResetEsimPackageName;
    private int mResetTelephonyManager = INVALID_SUBSCRIPTION_ID;
    private int mResetApn = INVALID_SUBSCRIPTION_ID;
    private int mSubscriptionIdToResetIms = INVALID_SUBSCRIPTION_ID;

    /**
     * Reconstruct based on keys stored within Bundle.
     * @param optionsFromBundle is a Bundle which previously stored through #writeIntoBundle()
     */
    public ResetNetworkRequest(Bundle optionsFromBundle) {
        if (optionsFromBundle == null) {
            return;
        }
        mResetOptions = optionsFromBundle.getInt(KEY_RESET_OPTIONS, RESET_NONE);
        mResetEsimPackageName = optionsFromBundle.getString(KEY_ESIM_PACKAGE);
        mResetTelephonyManager = optionsFromBundle.getInt(
                KEY_TELEPHONY_NET_POLICY_MANAGER_SUBID, INVALID_SUBSCRIPTION_ID);
        mResetApn = optionsFromBundle.getInt(KEY_APN_SUBID, INVALID_SUBSCRIPTION_ID);
        mSubscriptionIdToResetIms = optionsFromBundle.getInt(KEY_RESET_IMS_SUBID,
                INVALID_SUBSCRIPTION_ID);
    }

    /**
     * Construct of class
     * @param resetOptions is a binary combination(OR logic operation) of constants
     *         comes with RESET_ prefix. Which are the reset options comes within.
     */
    public ResetNetworkRequest(int resetOptions) {
        mResetOptions = resetOptions;
    }

    /**
     * Get the package name applied for resetting eSIM.
     * @return package name. {@code null} means resetting eSIM is not part of the
     *         option within this request.
     */
    public String getResetEsimPackageName() {
        return mResetEsimPackageName;
    }

    /**
     * Set the package name for resetting eSIM.
     * @param packageName is the package name for resetting eSIM.
     *        {@code null} will remove the resetting eSIM option out of this request.
     * @return this request
     */
    public ResetNetworkRequest setResetEsim(String packageName) {
        mResetEsimPackageName = packageName;
        return this;
    }

    /**
     * Get the subscription ID applied for resetting Telephony and NetworkPolicy.
     * @return subscription ID.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means
     *         resetting Telephony and NetworkPolicy is not part of the option
     *         within this request.
     */
    public int getResetTelephonyAndNetworkPolicyManager() {
        return mResetTelephonyManager;
    }

    /**
     * Set the subscription ID applied for resetting Telephony and NetworkPolicy.
     * @param subscriptionId is the subscription ID referenced fron SubscriptionManager.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means resetting Telephony and NetworkPolicy
     *         will not take place.
     * @return this request
     */
    public ResetNetworkRequest setResetTelephonyAndNetworkPolicyManager(int subscriptionId) {
        mResetTelephonyManager = subscriptionId;
        return this;
    }

    /**
     * Get the subscription ID applied for resetting APN.
     * @return subscription ID.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means resetting APN
     *         is not part of the option within this request.
     */
    public int getResetApnSubId() {
        return mResetApn;
    }

    /**
     * Set the subscription ID applied for resetting APN.
     * @param subscriptionId is the subscription ID referenced fron SubscriptionManager.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means resetting APN will not take place.
     * @return this request
     */
    public ResetNetworkRequest setResetApn(int subscriptionId) {
        mResetApn = subscriptionId;
        return this;
    }

    /**
     * Get the subscription ID applied for resetting IMS.
     * @return subscription ID.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means resetting IMS
     *         is not part of the option within this request.
     */
    public int getResetImsSubId() {
        return mSubscriptionIdToResetIms;
    }

    /**
     * Set the subscription ID applied for resetting APN.
     * @param subId is the subscription ID referenced from SubscriptionManager.
     *         {@code ALL_SUBSCRIPTION_ID} for applying to all subscriptions.
     *         {@code INVALID_SUBSCRIPTION_ID} means resetting IMS will not take place.
     * @return this
     */
    public ResetNetworkRequest setResetImsSubId(int subId) {
        mSubscriptionIdToResetIms = subId;
        return this;
    }

    /**
     * Store a copy of this request into Bundle given.
     * @param writeToBundle is a Bundle for storing configurations of this request.
     * @return this request
     */
    public ResetNetworkRequest writeIntoBundle(Bundle writeToBundle) {
        writeToBundle.putInt(KEY_RESET_OPTIONS, mResetOptions);
        writeToBundle.putString(KEY_ESIM_PACKAGE, mResetEsimPackageName);
        writeToBundle.putInt(KEY_TELEPHONY_NET_POLICY_MANAGER_SUBID, mResetTelephonyManager);
        writeToBundle.putInt(KEY_APN_SUBID, mResetApn);
        writeToBundle.putInt(KEY_RESET_IMS_SUBID, mSubscriptionIdToResetIms);
        return this;
    }

    /**
     * Build a ResetNetworkOperationBuilder based on configurations within this request.
     * @param context required by ResetNetworkOperationBuilder
     * @param looper required by ResetNetworkOperationBuilder for callback support
     * @return a ResetNetworkOperationBuilder
     */
    public ResetNetworkOperationBuilder toResetNetworkOperationBuilder(Context context,
            Looper looper) {
        // Follow specific order based on previous design within file ResetNetworkConfirm.java
        ResetNetworkOperationBuilder builder = new ResetNetworkOperationBuilder(context);
        if ((mResetOptions & RESET_CONNECTIVITY_MANAGER) != 0) {
            builder.resetConnectivityManager();
        }
        if ((mResetOptions & RESET_VPN_MANAGER) != 0) {
            builder.resetVpnManager();
        }
        if ((mResetOptions & RESET_WIFI_MANAGER) != 0) {
            builder.resetWifiManager();
        }
        if ((mResetOptions & RESET_WIFI_P2P_MANAGER) != 0) {
            builder.resetWifiP2pManager(looper);
        }
        if (mResetEsimPackageName != null) {
            builder.resetEsim(mResetEsimPackageName);
        }
        if (mResetTelephonyManager != INVALID_SUBSCRIPTION_ID) {
            builder.resetTelephonyAndNetworkPolicyManager(mResetTelephonyManager);
        }
        if ((mResetOptions & RESET_BLUETOOTH_MANAGER) != 0) {
            builder.resetBluetoothManager();
        }
        if (mResetApn != INVALID_SUBSCRIPTION_ID) {
            builder.resetApn(mResetApn);
        }
        if ((mResetOptions & RESET_IMS_STACK) != 0) {
            builder.resetIms(mSubscriptionIdToResetIms);
        }
        if ((mResetOptions & RESET_PHONE_PROCESS) != 0) {
            builder.restartPhoneProcess();
        }
        if ((mResetOptions & RESET_RILD) != 0) {
            builder.restartRild();
        }
        return builder;
    }
}
