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

import com.android.settings.core.TogglePreferenceController;

/**
 * {@link TogglePreferenceController} that used by all preferences that requires subscription id.
 */
public abstract class TelephonyTogglePreferenceController extends TogglePreferenceController
        implements TelephonyAvailabilityCallback {
    protected int mSubId;

    public TelephonyTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public int getAvailabilityStatus() {
        return MobileNetworkUtils.getAvailability(mContext, mSubId, this::getAvailabilityStatus);
    }
}
