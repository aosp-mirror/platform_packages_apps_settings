/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.LocaleConfig;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.List;

/**
 * This class provides methods that help dealing with per app locale.
 */
public class AppLocaleUtil {
    private static final String TAG = AppLocaleUtil.class.getSimpleName();

    public static final Intent LAUNCHER_ENTRY_INTENT =
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

    @VisibleForTesting
    static LocaleConfig sLocaleConfig;

    /**
     * Decides the UI display of per app locale.
     */
    public static boolean canDisplayLocaleUi(
            @NonNull Context context,
            @NonNull ApplicationInfo app,
            @NonNull List<ResolveInfo> infos) {
        boolean isDisallowedPackage = isDisallowedPackage(context, app.packageName);
        boolean hasLauncherEntry = hasLauncherEntry(app.packageName, infos);
        boolean isSignedWithPlatformKey = app.isSignedWithPlatformKey();
        boolean canDisplay = !isDisallowedPackage
                && !isSignedWithPlatformKey
                && hasLauncherEntry
                && isAppLocaleSupported(context, app.packageName);

        Log.i(TAG, "Can display preference - [" + app.packageName + "] :"
                + " isDisallowedPackage : " + isDisallowedPackage
                + " / isSignedWithPlatformKey : " + isSignedWithPlatformKey
                + " / hasLauncherEntry : " + hasLauncherEntry
                + " / canDisplay : " + canDisplay + " / 1.1");
        return canDisplay;
    }

    private static boolean isDisallowedPackage(Context context, String packageName) {
        final String[] disallowedPackages = context.getResources().getStringArray(
                R.array.config_disallowed_app_localeChange_packages);
        for (String disallowedPackage : disallowedPackages) {
            if (TextUtils.equals(packageName, disallowedPackage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLauncherEntry(String packageName, List<ResolveInfo> infos) {
        return infos.stream()
                .anyMatch(info -> info.activityInfo.packageName.equals(packageName));
    }

    /**
     * Check the function of per app language is supported by current application.
     */
    public static boolean isAppLocaleSupported(Context context, String packageName) {
        LocaleList localeList;
        if (sLocaleConfig != null) {
            localeList = getPackageLocales(sLocaleConfig);
        } else {
            localeList = getPackageLocales(context, packageName);
        }

        if (localeList != null) {
            return localeList.size() > 0;
        }

        if (FeatureFlagUtils.isEnabled(
                context, FeatureFlagUtils.SETTINGS_APP_LOCALE_OPT_IN_ENABLED)) {
            return false;
        }

        return getAssetLocales(context, packageName).length > 0;
    }

    /**
     * Get locales fron AssetManager.
     */
    public static String[] getAssetLocales(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String[] locales = packageManager.getResourcesForApplication(packageName)
                    .getAssets().getNonSystemLocales();
            if (locales == null) {
                Log.i(TAG, "[" + packageName + "] locales are null.");
            }
            if (locales.length <= 0) {
                Log.i(TAG, "[" + packageName + "] locales length is 0.");
                return new String[0];
            }
            String locale = locales[0];
            Log.i(TAG, "First asset locale - [" + packageName + "] " + locale);
            return locales;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Can not found the package name : " + packageName + " / " + e);
        }
        return new String[0];
    }

    @VisibleForTesting
    static LocaleList getPackageLocales(LocaleConfig localeConfig) {
        if (localeConfig.getStatus() == LocaleConfig.STATUS_SUCCESS) {
            return localeConfig.getSupportedLocales();
        }
        return null;
    }

    /**
     * Get locales from LocaleConfig.
     */
    public static LocaleList getPackageLocales(Context context, String packageName) {
        try {
            LocaleConfig localeConfig =
                    new LocaleConfig(context.createPackageContext(packageName, 0));
            return getPackageLocales(localeConfig);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Can not found the package name : " + packageName + " / " + e);
        }
        return null;
    }
}
