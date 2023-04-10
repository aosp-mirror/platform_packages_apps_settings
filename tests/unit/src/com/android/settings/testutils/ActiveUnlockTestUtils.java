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

package com.android.settings.testutils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.provider.DeviceConfig;
import android.provider.Settings;

import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;

import java.util.ArrayList;

/** Utilities class to enable or disable the Active Unlock flag in tests. */
public final class ActiveUnlockTestUtils {

    public static final String TARGET = "com.active.unlock.target";
    public static final String PROVIDER = "com.active.unlock.provider";
    public static final String TARGET_SETTING = "active_unlock_target";
    public static final String PROVIDER_SETTING = "active_unlock_provider";

    public static void enable(Context context) {
        ActiveUnlockTestUtils.enable(context, ActiveUnlockStatusUtils.UNLOCK_INTENT_LAYOUT);
    }

    public static void enable(Context context, String flagValue) {
        Settings.Secure.putString(
                context.getContentResolver(), TARGET_SETTING, TARGET);
        Settings.Secure.putString(
                context.getContentResolver(), PROVIDER_SETTING, PROVIDER);

        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.applicationInfo = applicationInfo;
        when(packageManager.resolveActivity(any(), anyInt())).thenReturn(resolveInfo);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = applicationInfo;
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = PROVIDER;
        providerInfo.applicationInfo = applicationInfo;
        packageInfo.providers = new ProviderInfo[] { providerInfo };
        ArrayList<PackageInfo> packageInfos = new ArrayList<>();
        packageInfos.add(packageInfo);
        when(packageManager.getInstalledPackages(any())).thenReturn(packageInfos);

        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_REMOTE_AUTH,
                ActiveUnlockStatusUtils.CONFIG_FLAG_NAME,
                flagValue,
                false /* makeDefault */);
    }

    public static void disable(Context context) {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_REMOTE_AUTH,
                ActiveUnlockStatusUtils.CONFIG_FLAG_NAME,
                null /* value */,
                false /* makeDefault */);
    }
}
