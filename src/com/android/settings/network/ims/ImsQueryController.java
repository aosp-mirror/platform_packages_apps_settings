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

import android.telephony.AccessNetworkConstants;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller class for querying IMS status
 */
abstract class ImsQueryController {

    private static final long TIMEOUT_MILLIS = 2000;

    private volatile int mCapability;
    private volatile int mTech;
    private volatile int mTransportType;

    /**
     * Constructor for query IMS status
     *
     * @param capability {@link MmTelFeature.MmTelCapabilities#MmTelCapability}
     * @param tech {@link ImsRegistrationImplBase#ImsRegistrationTech}
     * @param transportType {@link AccessNetworkConstants#TransportType}
     */
    ImsQueryController(
            @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int tech,
            @AccessNetworkConstants.TransportType int transportType) {
        mCapability = capability;
        mTech = tech;
        mTransportType = transportType;
    }

    abstract boolean isEnabledByUser(int subId);

    @VisibleForTesting
    boolean isTtyOnVolteEnabled(int subId) {
        return (new ImsQueryTtyOnVolteStat(subId)).query();
    }

    @VisibleForTesting
    boolean isEnabledByPlatform(int subId) throws InterruptedException, ImsException,
            IllegalArgumentException {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }

        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
        // TODO: have a shared thread pool instead of create ExecutorService
        //       everytime to improve performance.
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final BooleanConsumer booleanResult = new BooleanConsumer();
        imsMmTelManager.isSupported(mCapability, mTransportType, executor, booleanResult);
        // get() will be blocked until end of execution(isSupported()) within thread(executor)
        // or timeout after TIMEOUT_MILLIS milliseconds
        return booleanResult.get(TIMEOUT_MILLIS);
    }

    @VisibleForTesting
    boolean isProvisionedOnDevice(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        return (new ImsQueryProvisioningStat(subId, mCapability, mTech)).query();
    }

    @VisibleForTesting
    boolean isServiceStateReady(int subId) throws InterruptedException, ImsException,
            IllegalArgumentException {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }

        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
        // TODO: have a shared thread pool instead of create ExecutorService
        //       everytime to improve performance.
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final IntegerConsumer intResult = new IntegerConsumer();
        imsMmTelManager.getFeatureState(executor, intResult);
        return (intResult.get(TIMEOUT_MILLIS) == ImsFeature.STATE_READY);
    }
}
