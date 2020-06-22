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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;

import com.android.settings.core.TogglePreferenceController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TogglePreferenceController} that used by all preferences that requires subscription id.
 */
public abstract class TelephonyTogglePreferenceController extends TogglePreferenceController
        implements TelephonyAvailabilityCallback, TelephonyAvailabilityHandler {
    protected int mSubId;
    private AtomicInteger mAvailabilityStatus = new AtomicInteger(0);
    private AtomicInteger mSetSessionCount = new AtomicInteger(0);

    public TelephonyTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSetSessionCount.get() <= 0) {
            mAvailabilityStatus.set(MobileNetworkUtils
                    .getAvailability(mContext, mSubId, this::getAvailabilityStatus));
        }
        return mAvailabilityStatus.get();
    }

    @Override
    public void setAvailabilityStatus(int status) {
        mAvailabilityStatus.set(status);
        mSetSessionCount.getAndIncrement();
    }

    @Override
    public void unsetAvailabilityStatus() {
        mSetSessionCount.getAndDecrement();
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    /**
     * Get carrier config based on specific subscription id.
     *
     * @param subId is the subscription id
     * @return {@link PersistableBundle} of carrier config, or {@code null} when carrier config
     * is not available.
     */
    public PersistableBundle getCarrierConfigForSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return null;
        }
        final CarrierConfigManager carrierConfigMgr =
                mContext.getSystemService(CarrierConfigManager.class);
        return carrierConfigMgr.getConfigForSubId(subId);
    }
}
