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
import android.telephony.SubscriptionManager;

import androidx.lifecycle.LifecycleOwner;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

public class CallsDefaultSubscriptionController extends DefaultSubscriptionController {

    public CallsDefaultSubscriptionController(Context context, String preferenceKey,
            Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
        super(context, preferenceKey, lifecycle, lifecycleOwner);
    }

    public CallsDefaultSubscriptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected int getDefaultSubscriptionId() {
        int defaultCallSubId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        for (SubscriptionInfoEntity subInfo : mSubInfoEntityList) {
            int subId = subInfo.getSubId();
            if (subInfo.isActiveSubscriptionId && subId == defaultCallSubId) {
                return subId;
            }
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    protected void setDefaultSubscription(int subscriptionId) {
        mManager.setDefaultVoiceSubscriptionId(subscriptionId);
    }

    @Override
    public CharSequence getSummary() {
        return MobileNetworkUtils.getPreferredStatus(isRtlMode(), mContext, true,
                mSubInfoEntityList);
    }
}
