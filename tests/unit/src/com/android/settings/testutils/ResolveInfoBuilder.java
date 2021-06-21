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

package com.android.settings.testutils;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;

import com.google.common.base.Preconditions;

/**
 * Helper for building {@link ResolveInfo}s to be used in Robolectric tests.
 *
 * <p>The resulting {@link PackageInfo}s should typically be added to {@link
 * org.robolectric.shadows.ShadowPackageManager#addResolveInfoForIntent(Intent, ResolveInfo)}.
 */
public final class ResolveInfoBuilder {

    private final String mPackageName;
    private ActivityInfo mActivityInfo;
    private ProviderInfo mProviderInfo;

    public ResolveInfoBuilder(String packageName) {
        this.mPackageName = Preconditions.checkNotNull(packageName);
    }

    public ResolveInfoBuilder setActivity(String packageName, String className) {
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = packageName;
        mActivityInfo.name = className;
        return this;
    }

    public ResolveInfoBuilder setProvider(
            String packageName, String className, String authority, boolean isSystemApp) {
        mProviderInfo = new ProviderInfo();
        mProviderInfo.authority = authority;
        mProviderInfo.applicationInfo = new ApplicationInfo();
        if (isSystemApp) {
            mProviderInfo.applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
        mProviderInfo.packageName = mPackageName;
        mProviderInfo.applicationInfo.packageName  = mPackageName;
        mProviderInfo.name = className;
        return this;
    }

    public ResolveInfo build() {
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = mActivityInfo;
        info.resolvePackageName = mPackageName;
        info.providerInfo = mProviderInfo;
        return info;
    }
}
