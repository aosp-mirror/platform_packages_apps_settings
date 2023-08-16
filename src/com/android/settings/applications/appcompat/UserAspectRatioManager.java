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

package com.android.settings.applications.appcompat;

import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE;
import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE;

import static java.lang.Boolean.FALSE;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.provider.DeviceConfig;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.Map;

/**
 * Helper class for handling app aspect ratio override
 * {@link PackageManager.UserMinAspectRatio} set by user
 */
public class UserAspectRatioManager {
    private static final Intent LAUNCHER_ENTRY_INTENT =
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

    // TODO(b/288142656): Enable user aspect ratio settings by default
    private static final boolean DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_SETTINGS = false;
    @VisibleForTesting
    static final String KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS =
            "enable_app_compat_user_aspect_ratio_settings";
    static final String KEY_ENABLE_USER_ASPECT_RATIO_FULLSCREEN =
            "enable_app_compat_user_aspect_ratio_fullscreen";
    private static final boolean DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_FULLSCREEN = true;

    private final Context mContext;
    private final IPackageManager mIPm;
    /** Apps that have launcher entry defined in manifest */
    private final List<ResolveInfo> mInfoHasLauncherEntryList;
    private final Map<Integer, String> mUserAspectRatioMap;

    public UserAspectRatioManager(@NonNull Context context) {
        mContext = context;
        mIPm = AppGlobals.getPackageManager();
        mInfoHasLauncherEntryList = mContext.getPackageManager().queryIntentActivities(
                UserAspectRatioManager.LAUNCHER_ENTRY_INTENT, PackageManager.GET_META_DATA);
        mUserAspectRatioMap = getUserMinAspectRatioMapping();
    }

    /**
     * Whether user aspect ratio settings is enabled for device.
     */
    public static boolean isFeatureEnabled(Context context) {
        final boolean isBuildTimeFlagEnabled = context.getResources().getBoolean(
                com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled);
        return getValueFromDeviceConfig(KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS,
                DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_SETTINGS) && isBuildTimeFlagEnabled;
    }

    /**
     * @return user-specific {@link PackageManager.UserMinAspectRatio} override for an app
     */
    @PackageManager.UserMinAspectRatio
    public int getUserMinAspectRatioValue(@NonNull String packageName, int uid)
            throws RemoteException {
        final int aspectRatio = mIPm.getUserMinAspectRatio(packageName, uid);
        return hasAspectRatioOption(aspectRatio, packageName)
                ? aspectRatio : PackageManager.USER_MIN_ASPECT_RATIO_UNSET;
    }

    /**
     * @return corresponding string for {@link PackageManager.UserMinAspectRatio} value
     */
    @NonNull
    public String getUserMinAspectRatioEntry(@PackageManager.UserMinAspectRatio int aspectRatio,
            String packageName) {
        if (!hasAspectRatioOption(aspectRatio, packageName))  {
            return mUserAspectRatioMap.get(PackageManager.USER_MIN_ASPECT_RATIO_UNSET);
        }
        return mUserAspectRatioMap.get(aspectRatio);
    }

    /**
     * @return corresponding aspect ratio string for package name and user
     */
    @NonNull
    public String getUserMinAspectRatioEntry(@NonNull String packageName, int uid)
            throws RemoteException {
        final int aspectRatio = getUserMinAspectRatioValue(packageName, uid);
        return getUserMinAspectRatioEntry(aspectRatio, packageName);
    }

    /**
     * Whether user aspect ratio option is specified in
     * {@link R.array.config_userAspectRatioOverrideValues}
     * and is enabled by device config
     */
    public boolean hasAspectRatioOption(@PackageManager.UserMinAspectRatio int option,
            String packageName) {
        if (option == PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN
                && !isFullscreenOptionEnabled(packageName)) {
            return false;
        }
        return mUserAspectRatioMap.containsKey(option);
    }

    /**
     * Sets user-specified {@link PackageManager.UserMinAspectRatio} override for an app
     */
    public void setUserMinAspectRatio(@NonNull String packageName, int uid,
            @PackageManager.UserMinAspectRatio int aspectRatio) throws RemoteException {
        mIPm.setUserMinAspectRatio(packageName, uid, aspectRatio);
    }

    /**
     * Whether an app's aspect ratio can be overridden by user. Only apps with launcher entry
     * will be overridable.
     */
    public boolean canDisplayAspectRatioUi(@NonNull ApplicationInfo app) {
        Boolean appAllowsUserAspectRatioOverride = readComponentProperty(
                mContext.getPackageManager(), app.packageName,
                PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE);
        boolean hasLauncherEntry = mInfoHasLauncherEntryList.stream()
                .anyMatch(info -> info.activityInfo.packageName.equals(app.packageName));
        return !FALSE.equals(appAllowsUserAspectRatioOverride) && hasLauncherEntry;
    }

    /**
     * Whether fullscreen option in per-app user aspect ratio settings is enabled
     */
    @VisibleForTesting
    boolean isFullscreenOptionEnabled(String packageName) {
        Boolean appAllowsFullscreenOption = readComponentProperty(mContext.getPackageManager(),
                packageName, PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE);
        final boolean isBuildTimeFlagEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_appCompatUserAppAspectRatioFullscreenIsEnabled);
        return !FALSE.equals(appAllowsFullscreenOption) && isBuildTimeFlagEnabled
                && getValueFromDeviceConfig(KEY_ENABLE_USER_ASPECT_RATIO_FULLSCREEN,
                    DEFAULT_VALUE_ENABLE_USER_ASPECT_RATIO_FULLSCREEN);
    }

    private static boolean getValueFromDeviceConfig(String name, boolean defaultValue) {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_WINDOW_MANAGER, name, defaultValue);
    }

    @NonNull
    private Map<Integer, String> getUserMinAspectRatioMapping() {
        final String[] userMinAspectRatioStrings = mContext.getResources().getStringArray(
                R.array.config_userAspectRatioOverrideEntries);
        final int[] userMinAspectRatioValues = mContext.getResources().getIntArray(
                R.array.config_userAspectRatioOverrideValues);
        if (userMinAspectRatioStrings.length != userMinAspectRatioValues.length) {
            throw new RuntimeException(
                    "config_userAspectRatioOverride options cannot be different length");
        }

        final Map<Integer, String> userMinAspectRatioMap = new ArrayMap<>();
        for (int i = 0; i < userMinAspectRatioValues.length; i++) {
            final int aspectRatioVal = userMinAspectRatioValues[i];
            final String aspectRatioString = getAspectRatioStringOrDefault(
                    userMinAspectRatioStrings[i], aspectRatioVal);
            switch (aspectRatioVal) {
                // Only map known values of UserMinAspectRatio and ignore unknown entries
                case PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN:
                case PackageManager.USER_MIN_ASPECT_RATIO_UNSET:
                case PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN:
                case PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE:
                case PackageManager.USER_MIN_ASPECT_RATIO_4_3:
                case PackageManager.USER_MIN_ASPECT_RATIO_16_9:
                case PackageManager.USER_MIN_ASPECT_RATIO_3_2:
                    userMinAspectRatioMap.put(aspectRatioVal, aspectRatioString);
            }
        }
        if (!userMinAspectRatioMap.containsKey(PackageManager.USER_MIN_ASPECT_RATIO_UNSET)) {
            throw new RuntimeException("config_userAspectRatioOverrideValues options must have"
                    + " USER_MIN_ASPECT_RATIO_UNSET value");
        }
        return userMinAspectRatioMap;
    }

    @NonNull
    private String getAspectRatioStringOrDefault(@Nullable String aspectRatioString,
            @PackageManager.UserMinAspectRatio int aspectRatioVal) {
        if (aspectRatioString != null) {
            return aspectRatioString;
        }
        // Options are customized per device and if strings are set to @null, use default
        switch (aspectRatioVal) {
            case PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN:
                return mContext.getString(R.string.user_aspect_ratio_fullscreen);
            case PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN:
                return mContext.getString(R.string.user_aspect_ratio_half_screen);
            case PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE:
                return mContext.getString(R.string.user_aspect_ratio_device_size);
            case PackageManager.USER_MIN_ASPECT_RATIO_4_3:
                return mContext.getString(R.string.user_aspect_ratio_4_3);
            case PackageManager.USER_MIN_ASPECT_RATIO_16_9:
                return mContext.getString(R.string.user_aspect_ratio_16_9);
            case PackageManager.USER_MIN_ASPECT_RATIO_3_2:
                return mContext.getString(R.string.user_aspect_ratio_3_2);
            default:
                return mContext.getString(R.string.user_aspect_ratio_app_default);
        }
    }

    @Nullable
    private static Boolean readComponentProperty(PackageManager pm, String packageName,
            String propertyName) {
        try {
            return pm.getProperty(propertyName, packageName).getBoolean();
        } catch (PackageManager.NameNotFoundException e) {
            // No such property name
        }
        return null;
    }

    @VisibleForTesting
    void addInfoHasLauncherEntry(@NonNull ResolveInfo infoHasLauncherEntry) {
        mInfoHasLauncherEntryList.add(infoHasLauncherEntry);
    }
}
