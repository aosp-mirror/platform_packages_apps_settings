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

import android.content.Context;
import android.telephony.ims.ImsException;

/**
 * Controller class for mock Wifi calling status
 */
public class MockWifiCallingQueryImsState extends WifiCallingQueryImsState {

    private Boolean mIsTtyOnVolteEnabled;
    private Boolean mIsEnabledOnPlatform;
    private Boolean mIsProvisionedOnDevice;
    private Boolean mIsServiceStateReady;
    private Boolean mIsEnabledByUser;

    /**
     * Constructor
     *
     * @param context {@code Context}
     * @param subId subscription's id
     */
    public MockWifiCallingQueryImsState(Context context, int subId) {
        super(context, subId);
    }

    public void setIsTtyOnVolteEnabled(boolean enabled) {
        mIsTtyOnVolteEnabled = enabled;
    }

    @Override
    boolean isTtyOnVolteEnabled(int subId) {
        if (mIsTtyOnVolteEnabled != null) {
            return mIsTtyOnVolteEnabled;
        }
        return super.isTtyOnVolteEnabled(subId);
    }


    public void setIsEnabledByPlatform(boolean isEnabled) {
        mIsEnabledOnPlatform = isEnabled;
    }

    @Override
    boolean isEnabledByPlatform(int subId) throws InterruptedException, ImsException,
            IllegalArgumentException {
        if (mIsEnabledOnPlatform != null) {
            return mIsEnabledOnPlatform;
        }
        return super.isEnabledByPlatform(subId);
    }

    public void setIsProvisionedOnDevice(boolean isProvisioned) {
        mIsProvisionedOnDevice = isProvisioned;
    }

    @Override
    boolean isProvisionedOnDevice(int subId) {
        if (mIsProvisionedOnDevice != null) {
            return mIsProvisionedOnDevice;
        }
        return super.isProvisionedOnDevice(subId);
    }

    public void setServiceStateReady(boolean isReady) {
        mIsServiceStateReady = isReady;
    }

    @Override
    boolean isServiceStateReady(int subId) throws InterruptedException, ImsException,
            IllegalArgumentException {
        if (mIsServiceStateReady != null) {
            return mIsServiceStateReady;
        }
        return super.isServiceStateReady(subId);
    }

    public void setIsEnabledByUser(boolean enabled) {
        mIsEnabledByUser = enabled;
    }

    @Override
    boolean isEnabledByUser(int subId) {
        if (mIsEnabledByUser != null) {
            return mIsEnabledByUser;
        }
        return super.isEnabledByUser(subId);
    }

}
