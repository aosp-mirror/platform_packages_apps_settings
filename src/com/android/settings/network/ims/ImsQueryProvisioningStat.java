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

package com.android.settings.network.ims;

import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;


/**
 * An {@link ImsQuery} for accessing IMS provision stat
 */
public class ImsQueryProvisioningStat implements ImsQuery {

    private static final String LOG_TAG = "QueryPrivisioningStat";

    private volatile int mSubId;
    private volatile int mCapability;
    private volatile int mTech;

    /**
     * Constructor
     * @param subId subscription id
     * @param capability {@link MmTelFeature.MmTelCapabilities#MmTelCapability}
     * @param tech {@link ImsRegistrationImplBase#ImsRegistrationTech}
     */
    public ImsQueryProvisioningStat(int subId,
            @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        mSubId = subId;
        mCapability = capability;
        mTech = tech;
    }

    /**
     * Implementation of interface {@link ImsQuery#query()}
     *
     * @return result of query
     */
    public boolean query() {
        try {
            final ProvisioningManager privisionManager =
                    ProvisioningManager.createForSubscriptionId(mSubId);
            return privisionManager.getProvisioningStatusForCapability(mCapability, mTech);
        } catch (IllegalArgumentException exception) {
            Log.w(LOG_TAG, "fail to get Provisioning stat. subId=" + mSubId, exception);
        }
        return false;
    }
}
