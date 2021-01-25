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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.Executor;


/**
 * {@link PhoneStateListener} to listen to Allowed Network Types changed
 */
public class AllowedNetworkTypesListener extends PhoneStateListener implements
        PhoneStateListener.AllowedNetworkTypesChangedListener {
    private static final String LOG_TAG = "NetworkModeListener";

    @VisibleForTesting
    AllowedNetworkTypesListener.OnAllowedNetworkTypesChangedListener mListener;
    private long mAllowedNetworkType = -1;
    private Executor mExecutor;

    public AllowedNetworkTypesListener(Executor executor) {
        super();
        mExecutor = executor;
    }

    public void setAllowedNetworkTypesChangedListener(OnAllowedNetworkTypesChangedListener lsn) {
        mListener = lsn;
    }

    /**
     * Register a PhoneStateListener for Allowed Network Types changed.
     * @param context the Context
     * @param subId the subscription id.
     */
    public void register(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.registerPhoneStateListener(mExecutor, this);
    }

    /**
     * Unregister a PhoneStateListener for Allowed Network Types changed.
     * @param context the Context
     * @param subId the subscription id.
     */
    public void unregister(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.unregisterPhoneStateListener(this);
    }

    @Override
    public void onAllowedNetworkTypesChanged(Map<Integer, Long> allowedNetworkTypesList) {
        long newAllowedNetworkType = allowedNetworkTypesList.get(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        if (mListener != null && mAllowedNetworkType != newAllowedNetworkType) {
            mListener.onAllowedNetworkTypesChanged();
            Log.d(LOG_TAG, "onAllowedNetworkChanged: " + mAllowedNetworkType);
        }
        mAllowedNetworkType = newAllowedNetworkType;
    }

    /**
     * Listener for update of Preferred Network Mode change
     */
    public interface OnAllowedNetworkTypesChangedListener {
        /**
         * Notify the allowed network type changed.
         */
        void onAllowedNetworkTypesChanged();
    }
}
