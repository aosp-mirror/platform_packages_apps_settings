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

package com.android.settings.network;

import android.content.Context;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executor;

/**
 * {@link TelephonyCallback} to listen to Allowed Network Types changed
 *
 * @deprecated Please use {@link com.android.settings.network.telephony.AllowedNetworkTypesFlowKt}
 * instead.
 */
@Deprecated
public class AllowedNetworkTypesListener extends TelephonyCallback implements
        TelephonyCallback.AllowedNetworkTypesListener {
    private static final String LOG_TAG = "NetworkModeListener";

    @VisibleForTesting
    OnAllowedNetworkTypesListener mListener;
    private Executor mExecutor;

    public AllowedNetworkTypesListener(Executor executor) {
        super();
        mExecutor = executor;
    }

    public void setAllowedNetworkTypesListener(OnAllowedNetworkTypesListener lsn) {
        mListener = lsn;
    }

    /**
     * Register a TelephonyCallback for Allowed Network Types changed.
     * @param context the Context
     * @param subId the subscription id.
     */
    public void register(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.registerTelephonyCallback(mExecutor, this);
    }

    /**
     * Unregister a TelephonyCallback for Allowed Network Types changed.
     * @param context the Context
     * @param subId the subscription id.
     */
    public void unregister(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.unregisterTelephonyCallback(this);
    }

    @Override
    public void onAllowedNetworkTypesChanged(int reason, long newAllowedNetworkType) {
        if (reason != TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
                && reason != TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER) {
            return;
        }
        if (mListener != null) {
            mListener.onAllowedNetworkTypesChanged();
            Log.d(LOG_TAG, "onAllowedNetworkChanged: " + newAllowedNetworkType);
        }
    }

    /**
     * Listener for update of Preferred Network Mode change
     */
    public interface OnAllowedNetworkTypesListener {
        /**
         * Notify the allowed network type changed.
         */
        void onAllowedNetworkTypesChanged();
    }
}
