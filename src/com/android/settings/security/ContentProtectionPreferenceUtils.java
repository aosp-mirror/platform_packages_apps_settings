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
package com.android.settings.security;

import static android.view.contentprotection.flags.Flags.manageDevicePolicyEnabled;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.view.contentcapture.ContentCaptureManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.Utils;

/** Util class for content protection preference. */
public class ContentProtectionPreferenceUtils {

    /**
     * Whether or not the content protection setting page is available.
     */
    public static boolean isAvailable(@NonNull Context context) {
        if (!settingUiEnabled() || getContentProtectionServiceComponentName(context) == null) {
            return false;
        }
        return true;
    }

    private static String getContentProtectionServiceFlatComponentName(@NonNull Context context) {
        return context.getString(config_defaultContentProtectionService);
    }

    @Nullable
    private static ComponentName getContentProtectionServiceComponentName(@NonNull Context context) {
        String flatComponentName = getContentProtectionServiceFlatComponentName(context);
        if (flatComponentName == null) {
            return null;
        }
        return ComponentName.unflattenFromString(flatComponentName);
    }

    /**
     * Whether or not the content protection UI is enabled.
     */
    private static boolean settingUiEnabled() {
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                ContentCaptureManager.DEFAULT_ENABLE_CONTENT_PROTECTION_RECEIVER);
    }

    /** Returns the managed profile or null if none exists. */
    @Nullable
    public static UserHandle getManagedProfile(@NonNull Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }
        return Utils.getManagedProfile(userManager);
    }

    /** Returns the current content protection policy. */
    @DevicePolicyManager.ContentProtectionPolicy
    public static int getContentProtectionPolicy(
            @NonNull Context context, @Nullable UserHandle managedProfile) {
        if (!manageDevicePolicyEnabled()) {
            return DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        }
        Context policyContext = createContentProtectionPolicyContext(context, managedProfile);
        return getContentProtectionPolicyWithGivenContext(policyContext);
    }

    @NonNull
    private static Context createContentProtectionPolicyContext(
            @NonNull Context context, @Nullable UserHandle managedProfile) {
        if (managedProfile == null) {
            return context;
        }
        try {
            return context.createPackageContextAsUser(
                    context.getPackageName(), /* flags= */ 0, managedProfile);
        } catch (PackageManager.NameNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DevicePolicyManager.ContentProtectionPolicy
    private static int getContentProtectionPolicyWithGivenContext(@NonNull Context context) {
        DevicePolicyManager devicePolicyManager =
                context.getSystemService(DevicePolicyManager.class);
        if (devicePolicyManager == null) {
            return DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        }
        return devicePolicyManager.getContentProtectionPolicy(/* admin= */ null);
    }
}
