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
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * A Class monitoring the availability of subscription IDs provided within reset request.
 *
 * This is to detect the situation when user changing SIM card during the presenting of
 * confirmation UI.
 */
public class ResetSubscriptionContract implements AutoCloseable {
    private static final String TAG = "ResetSubscriptionContract";

    private final Context mContext;
    private ExecutorService mExecutorService;
    private final int [] mResetSubscriptionIds;
    @VisibleForTesting
    protected OnSubscriptionsChangedListener mSubscriptionsChangedListener;
    private AtomicBoolean mSubscriptionsUpdateNotify = new AtomicBoolean();

    /**
     * Constructor
     * @param context Context
     * @param resetRequest the request object for perform network reset operation.
     */
    public ResetSubscriptionContract(Context context, ResetNetworkRequest resetRequest) {
        mContext = context;
        // Only keeps specific subscription ID required to perform reset operation
        IntStream subIdStream = IntStream.of(
                resetRequest.getResetTelephonyAndNetworkPolicyManager(),
                resetRequest.getResetApnSubId(), resetRequest.getResetImsSubId());
        mResetSubscriptionIds = subIdStream.sorted().distinct()
                .filter(id -> SubscriptionManager.isUsableSubscriptionId(id))
                .toArray();

        if (mResetSubscriptionIds.length <= 0) {
            return;
        }

        // Monitoring callback through background thread
        mExecutorService = Executors.newSingleThreadExecutor();
        startMonitorSubscriptionChange();
    }

    /**
     * A method for detecting if there's any subscription under monitor no longer active.
     * @return subscription ID which is no longer active.
     */
    public Integer getAnyMissingSubscriptionId() {
        if (mResetSubscriptionIds.length <= 0) {
            return null;
        }
        SubscriptionManager mgr = getSubscriptionManager();
        if (mgr == null) {
            Log.w(TAG, "Fail to access subscription manager");
            return mResetSubscriptionIds[0];
        }
        for (int idx = 0; idx < mResetSubscriptionIds.length; idx++) {
            int subId = mResetSubscriptionIds[idx];
            if (mgr.getActiveSubscriptionInfo(subId) == null) {
                Log.w(TAG, "SubId " + subId + " no longer active.");
                return subId;
            }
        }
        return null;
    }

    /**
     * Async callback when detecting if there's any subscription under monitor no longer active.
     * @param subscriptionId subscription ID which is no longer active.
     */
    public void onSubscriptionInactive(int subscriptionId) {}

    @VisibleForTesting
    protected SubscriptionManager getSubscriptionManager() {
        return mContext.getSystemService(SubscriptionManager.class);
    }

    @VisibleForTesting
    protected OnSubscriptionsChangedListener getChangeListener() {
        return new OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                /**
                 * Reducing the processing time on main UI thread through a flag.
                 * Once flag get into false, which means latest callback has been
                 * processed.
                 */
                mSubscriptionsUpdateNotify.set(true);

                // Back to main UI thread
                mContext.getMainExecutor().execute(() -> {
                    // Remove notifications and perform checking.
                    if (mSubscriptionsUpdateNotify.getAndSet(false)) {
                        Integer subId = getAnyMissingSubscriptionId();
                        if (subId != null) {
                            onSubscriptionInactive(subId);
                        }
                    }
                });
            }
        };
    }

    private void startMonitorSubscriptionChange() {
        SubscriptionManager mgr = getSubscriptionManager();
        if (mgr == null) {
            return;
        }
        // update monitor listener
        mSubscriptionsChangedListener = getChangeListener();

        mgr.addOnSubscriptionsChangedListener(
                mExecutorService, mSubscriptionsChangedListener);
    }

    // Implementation of AutoCloseable
    public void close() {
        if (mExecutorService == null) {
            return;
        }
        // Stop monitoring subscription change
        SubscriptionManager mgr = getSubscriptionManager();
        if (mgr != null) {
            mgr.removeOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        }
        // Release Executor
        mExecutorService.shutdownNow();
        mExecutorService = null;
    }
}
