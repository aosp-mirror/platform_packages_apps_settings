/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.Manifest;
import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.content.pm.ICrossProfileApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Implements(CrossProfileApps.class)
public class ShadowCrossProfileApps extends org.robolectric.shadows.ShadowCrossProfileApps {
    private static final Set<String> configurableInteractAcrossProfilePackages = new HashSet<>();
    private Context mContext;
    private PackageManager mPackageManager;

    @Implementation
    protected void __constructor__(Context context, ICrossProfileApps service) {
        super.__constructor__(context, service);
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }
    public void addCrossProfilePackage(String packageName) {
        configurableInteractAcrossProfilePackages.add(packageName);
    }

    @Implementation
    protected boolean canConfigureInteractAcrossProfiles(@NonNull String packageName) {
        return configurableInteractAcrossProfilePackages.contains(packageName);
    }

    @Implementation
    protected boolean canUserAttemptToConfigureInteractAcrossProfiles(@NonNull String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = mPackageManager.getPackageInfo(packageName, /* flags= */ 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        if (packageInfo == null || packageInfo.requestedPermissions == null) {
            return false;
        }
        return Arrays.asList(packageInfo.requestedPermissions).contains(
                Manifest.permission.INTERACT_ACROSS_PROFILES);
    }
}
