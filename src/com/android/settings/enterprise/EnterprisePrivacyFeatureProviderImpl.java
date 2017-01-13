/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.vpn2.ConnectivityManagerWrapper;
import com.android.settings.vpn2.VpnUtils;

import java.util.Date;
import java.util.List;

public class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {

    private final DevicePolicyManagerWrapper mDpm;
    private final PackageManagerWrapper mPm;
    private final UserManager mUm;
    private final ConnectivityManagerWrapper mCm;

    private static final int MY_USER_ID = UserHandle.myUserId();

    public EnterprisePrivacyFeatureProviderImpl(DevicePolicyManagerWrapper dpm,
            PackageManagerWrapper pm, UserManager um, ConnectivityManagerWrapper cm) {
        mDpm = dpm;
        mPm = pm;
        mUm = um;
        mCm = cm;
    }

    @Override
    public boolean hasDeviceOwner() {
        if (!mPm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            return false;
        }
        return mDpm.getDeviceOwnerComponentOnAnyUser() != null;
    }

    private int getManagedProfileUserId() {
        for (final UserInfo userInfo : mUm.getProfiles(MY_USER_ID)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -1;
    }

    @Override
    public boolean isInCompMode() {
        return hasDeviceOwner() && getManagedProfileUserId() != -1;
    }

    @Override
    public Date getLastSecurityLogRetrievalTime() {
        final long timestamp = mDpm.getLastSecurityLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastBugReportRequestTime() {
        final long timestamp = mDpm.getLastBugReportRequestTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastNetworkLogRetrievalTime() {
        final long timestamp = mDpm.getLastNetworkLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public boolean isAlwaysOnVpnSetInPrimaryUser() {
        return VpnUtils.isAlwaysOnVpnSet(mCm, MY_USER_ID);
    }

    @Override
    public boolean isAlwaysOnVpnSetInManagedProfile() {
        final int managedProfileUserId = getManagedProfileUserId();
        return managedProfileUserId != -1 &&
                VpnUtils.isAlwaysOnVpnSet(mCm, managedProfileUserId);
    }

    @Override
    public boolean isGlobalHttpProxySet() {
        return mCm.getGlobalProxy() != null;
    }
}
