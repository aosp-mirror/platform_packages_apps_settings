/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.settings.network.SubscriptionUtil;

/**
 * Shows information about disable a physical SIM.
 */
public class DisableSimFooterPreferenceController extends TelephonyBasePreferenceController {

    /**
     * Constructor
     */
    public DisableSimFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * re-init for SIM based on given subscription ID.
     * @param subId is the given subscription ID
     */
    public void init(int subId) {
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        SubscriptionManager subManager = mContext.getSystemService(SubscriptionManager.class);
        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (info.getSubscriptionId() == subId) {
                if (info.isEmbedded() || SubscriptionUtil.showToggleForPhysicalSim(subManager)) {
                    return CONDITIONALLY_UNAVAILABLE;
                }
                break;
            }
        }
        return AVAILABLE;
    }
}
