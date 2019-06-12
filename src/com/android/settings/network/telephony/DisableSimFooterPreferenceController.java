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
import android.telephony.TelephonyManager;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;

public class DisableSimFooterPreferenceController extends BasePreferenceController {
    private int mSubId;

    public DisableSimFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    public void init(int subId) {
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (info.getSubscriptionId() == mSubId) {
                if (info.isEmbedded()) {
                    return CONDITIONALLY_UNAVAILABLE;
                }
                break;
            }
        }
        return AVAILABLE;
    }
}
