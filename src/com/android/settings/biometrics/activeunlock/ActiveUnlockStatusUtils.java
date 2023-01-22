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

package com.android.settings.biometrics.activeunlock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.BasePreferenceController.AvailabilityStatus;

import java.util.List;

/** Utilities for active unlock details shared between Security Settings and Safety Center. */
public class ActiveUnlockStatusUtils {

    /** The flag to determining whether active unlock in settings is enabled. */
    public static final String CONFIG_FLAG_NAME = "active_unlock_in_settings";

    /** Flag value that represents the layout for unlock intent should be used. */
    public static final String UNLOCK_INTENT_LAYOUT = "unlock_intent_layout";

    /** Flag value that represents the layout for biometric failure should be used. */
    public static final String BIOMETRIC_FAILURE_LAYOUT = "biometric_failure_layout";

    private static final String ACTIVE_UNLOCK_PROVIDER = "active_unlock_provider";
    private static final String ACTIVE_UNLOCK_TARGET = "active_unlock_target";

    private static final String TAG = "ActiveUnlockStatusUtils";

    private final Context mContext;
    private final ContentResolver mContentResolver;

    public ActiveUnlockStatusUtils(@NonNull Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    /** Returns whether the active unlock settings entity should be shown. */
    public boolean isAvailable() {
        return getAvailability() == BasePreferenceController.AVAILABLE;
    }

    /**
     * Returns whether the active unlock layout with the unlock on intent configuration should be
     * used.
     */
    public boolean useUnlockIntentLayout() {
        return isAvailable() && UNLOCK_INTENT_LAYOUT.equals(getFlagState());
    }

    /**
     *
     * Returns whether the active unlock layout with the unlock on biometric failure configuration
     * should be used.
     */
    public boolean useBiometricFailureLayout() {
        return isAvailable() && BIOMETRIC_FAILURE_LAYOUT.equals(getFlagState());
    }

    /**
     * Returns the authority used to fetch dynamic active unlock content.
     */
    @Nullable
    public String getAuthority() {
        final String authority = Settings.Secure.getString(
                mContext.getContentResolver(), ACTIVE_UNLOCK_PROVIDER);
        if (authority == null) {
            Log.i(TAG, "authority not set");
            return null;
        }
        final List<PackageInfo> packageInfos =
                mContext.getPackageManager().getInstalledPackages(
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PROVIDERS));
        for (PackageInfo packageInfo : packageInfos) {
            final ProviderInfo[] providers = packageInfo.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    if (authority.equals(provider.authority) && isSystemApp(provider)) {
                        return authority;
                    }
                }
            }
        }
        Log.e(TAG, "authority not valid");
        return null;
    }

    private static boolean isSystemApp(ComponentInfo componentInfo) {
        final ApplicationInfo applicationInfo = componentInfo.applicationInfo;
        if (applicationInfo == null) {
            Log.e(TAG, "application info is null");
            return false;
        }
        return applicationInfo.isSystemApp();
    }

    /**
     * Returns the intent used to launch the active unlock activity.
     */
    @Nullable
    public Intent getIntent() {
        final String targetAction = Settings.Secure.getString(
                mContentResolver, ACTIVE_UNLOCK_TARGET);
        if (targetAction == null) {
            Log.i(TAG, "Target action not set");
            return null;
        }
        final Intent intent = new Intent(targetAction);
        final ActivityInfo activityInfo = intent.resolveActivityInfo(
                mContext.getPackageManager(), PackageManager.MATCH_ALL);
        if (activityInfo == null) {
            Log.e(TAG, "Target activity not found");
            return null;
        }
        if (!isSystemApp(activityInfo)) {
            Log.e(TAG, "Target application is not system");
            return null;
        }
        Log.i(TAG, "Target application is valid");
        return intent;
    }

    /** Returns the availability status of the active unlock feature. */
    @AvailabilityStatus
    int getAvailability() {
        if (!Utils.hasFingerprintHardware(mContext) && !Utils.hasFaceHardware(mContext)) {
            return BasePreferenceController.UNSUPPORTED_ON_DEVICE;
        }
        if (!UNLOCK_INTENT_LAYOUT.equals(getFlagState())
                  && !BIOMETRIC_FAILURE_LAYOUT.equals(getFlagState())) {
            return BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
        }
        if (getAuthority() != null && getIntent() != null) {
            return BasePreferenceController.AVAILABLE;
        }
        return BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
    }

    private static String getFlagState() {
        return DeviceConfig.getProperty(DeviceConfig.NAMESPACE_REMOTE_AUTH, CONFIG_FLAG_NAME);
    }
}
