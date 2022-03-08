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
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.settings.Utils;

public class SmsDefaultSubscriptionController extends DefaultSubscriptionController {

    private final boolean mIsAskEverytimeSupported;

    public SmsDefaultSubscriptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mIsAskEverytimeSupported = mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_sms_ask_every_time_support);
    }

    @Override
    protected SubscriptionInfo getDefaultSubscriptionInfo() {
        return mManager.getActiveSubscriptionInfo(getDefaultSubscriptionId());
    }

    @Override
    protected int getDefaultSubscriptionId() {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    @Override
    protected void setDefaultSubscription(int subscriptionId) {
        mManager.setDefaultSmsSubId(subscriptionId);
    }

    @Override
    protected boolean isAskEverytimeSupported() {
        return mIsAskEverytimeSupported;
    }

    @Override
    public PhoneAccountHandle getDefaultCallingAccountHandle() {
        // Not supporting calling account override by VoIP
        return null;
    }

    @Override
    public CharSequence getSummary() {
        return MobileNetworkUtils.getPreferredStatus(isRtlMode(), mContext, mManager, false);
    }
}
