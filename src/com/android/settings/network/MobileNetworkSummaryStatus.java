/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.helper.SelectableSubscriptions;
import com.android.settings.network.helper.SubscriptionAnnotation;
import com.android.settings.network.helper.SubscriptionGrouping;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This one keeps the information required by MobileNetworkSummaryController.
 */
public class MobileNetworkSummaryStatus {
    private static final String LOG_TAG = "MobileNetworkSummaryStatus";

    private Future<Map<Integer, CharSequence>> mUniqueNameMapping;
    private Map<Integer, CharSequence> mUniqueNameMappingCache;

    private Future<Boolean> mIsEuiccConfiguable;
    private Boolean mIsEuiccConfiguableCache;

    private Future<Boolean> mIsPsimDisableSupported;
    private Boolean mIsPsimDisableSupportedCache;

    private List<SubscriptionAnnotation> mSubscriptionList;

    private boolean mDisableReEntranceUpdate;

    // Constructor
    public MobileNetworkSummaryStatus() {}

    /**
     * Update the status
     * @param context
     * @param andThen Consumer which always performed by the end of #update()
     *                and avoid from repeated queries.
     */
    public void update(Context context, Consumer<MobileNetworkSummaryStatus> andThen) {
        if (mDisableReEntranceUpdate) {
            Log.d(LOG_TAG, "network summary query ignored");
            if (andThen != null) {
                andThen.accept(this);
            }
            return;
        }
        mDisableReEntranceUpdate = true;
        Log.d(LOG_TAG, "network summary query");

        // Query Euicc in background
        mIsEuiccConfiguable = (Future<Boolean>)
                ThreadUtils.postOnBackgroundThread(() -> isEuiccConfiguable(context));

        // Query display name in background
        mUniqueNameMapping = (Future<Map<Integer, CharSequence>>)
                ThreadUtils.postOnBackgroundThread(() -> getUniqueNameForDisplay(context));

        // Query support status of pSIM disable feature
        mIsPsimDisableSupported = (Future<Boolean>) ThreadUtils.postOnBackgroundThread(()
                -> isPhysicalSimDisableSupported(context));

        // Query subscription
        mSubscriptionList = getSubscriptions(context);

        if (andThen != null) {
            andThen.accept(this);
        }
        mDisableReEntranceUpdate = false;
    }

    /**
     * Get the subscription information
     * @return a list of SubscriptionAnnotation
     */
    public List<SubscriptionAnnotation> getSubscriptionList() {
        return mSubscriptionList;
    }

    /**
     * Get unique display name for a specific subscription
     * @param subscriptionId subscription ID
     * @return display name for that subscription
     */
    public CharSequence getDisplayName(int subscriptionId) {
        if (mUniqueNameMapping != null) {
            try {
                mUniqueNameMappingCache = mUniqueNameMapping.get();
            } catch (Exception exception) {
                Log.w(LOG_TAG, "Fail to get display names", exception);
            }
            mUniqueNameMapping = null;
        }
        if (mUniqueNameMappingCache == null) {
            return null;
        }
        return mUniqueNameMappingCache.get(subscriptionId);
    }

    // Check if Euicc is currently available
    public boolean isEuiccConfigSupport() {
        if (mIsEuiccConfiguable != null) {
            try {
                mIsEuiccConfiguableCache = mIsEuiccConfiguable.get();
            } catch (Exception exception) {
                Log.w(LOG_TAG, "Fail to get euicc config status", exception);
            }
            mIsEuiccConfiguable = null;
        }
        return (mIsEuiccConfiguableCache == null) ?
                false : mIsEuiccConfiguableCache.booleanValue();
    }

    // Check if disable physical SIM is supported
    public boolean isPhysicalSimDisableSupport() {
        if (mIsPsimDisableSupported != null) {
            try {
                mIsPsimDisableSupportedCache = mIsPsimDisableSupported.get();
            } catch (Exception exception) {
                Log.w(LOG_TAG, "Fail to get pSIM disable support", exception);
            }
            mIsPsimDisableSupported = null;
        }
        return (mIsPsimDisableSupportedCache == null) ?
                false : mIsPsimDisableSupportedCache.booleanValue();
    }

    private List<SubscriptionAnnotation> getSubscriptions(Context context) {
        return (new SelectableSubscriptions(context, true))

                // To maintain the consistency with SubscriptionUtil#getAvailableSubscriptions().
                .addFinisher(new SubscriptionGrouping())

                .call()
                .stream()
                .filter(SubscriptionAnnotation::isDisplayAllowed)
                .collect(Collectors.toList());
    }

    private Map<Integer, CharSequence> getUniqueNameForDisplay(Context context) {
        return SubscriptionUtil.getUniqueSubscriptionDisplayNames(context);
    }

    private boolean isPhysicalSimDisableSupported(Context context) {
        SubscriptionManager subMgr = context.getSystemService(SubscriptionManager.class);
        return SubscriptionUtil.showToggleForPhysicalSim(subMgr);
    }

    private boolean isEuiccConfiguable(Context context) {
        return MobileNetworkUtils.showEuiccSettingsDetecting(context);
    }
}
