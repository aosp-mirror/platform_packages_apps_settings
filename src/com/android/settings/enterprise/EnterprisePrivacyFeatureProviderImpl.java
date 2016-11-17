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

import com.android.settings.applications.PackageManagerWrapper;

public class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {

    private final DevicePolicyManagerWrapper mDpm;
    private final PackageManagerWrapper mPm;

    public EnterprisePrivacyFeatureProviderImpl(DevicePolicyManagerWrapper dpm,
            PackageManagerWrapper pm) {
        mDpm = dpm;
        mPm = pm;
    }

    @Override
    public boolean hasDeviceOwner() {
        if (!mPm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            return false;
        }
        return mDpm.getDeviceOwnerComponentOnAnyUser() != null;
    }
}
