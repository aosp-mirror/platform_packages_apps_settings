/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** This class provides methods that help dealing with app stores. */
public class AppStoreUtil {
    private static final String LOG_TAG = "AppStoreUtil";

    private static Intent resolveIntent(Context context, Intent i) {
        ResolveInfo result = context.getPackageManager().resolveActivity(i, 0);
        return result != null ? new Intent(i.getAction())
                .setClassName(result.activityInfo.packageName, result.activityInfo.name) : null;
    }

    /**
     * Returns a {@link Pair pair result}. The first item is the package name of the app that we
     * consider to be the user-visible 'installer' of given packageName, if one is available. The
     * second item is the {@link InstallSourceInfo install source info} of the given package.
     */
    @NonNull
    public static Pair<String, InstallSourceInfo> getInstallerPackageNameAndInstallSourceInfo(
            @NonNull Context context, @NonNull String packageName) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(packageName);

        String installerPackageName;
        InstallSourceInfo source = null;
        try {
            source = context.getPackageManager().getInstallSourceInfo(packageName);
            // By default, use the installing package name.
            installerPackageName = source.getInstallingPackageName();
            // Use the recorded originating package name only if the initiating package is a system
            // app (eg. Package Installer). The originating package is not verified by the platform,
            // so we choose to ignore this when supplied by a non-system app.
            String originatingPackageName = source.getOriginatingPackageName();
            String initiatingPackageName = source.getInitiatingPackageName();
            if (originatingPackageName != null && initiatingPackageName != null
                    && !initiatingPackageName.equals("com.android.shell")) {
                ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                        initiatingPackageName, 0);
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    installerPackageName = originatingPackageName;
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(LOG_TAG, "Exception while retrieving the package installer of " + packageName, e);
            installerPackageName = null;
        }
        return new Pair<>(installerPackageName, source);
    }

    /**
     * Returns the package name of the app that we consider to be the user-visible 'installer'
     * of given packageName, if one is available.
     */
    @Nullable
    public static String getInstallerPackageName(@NonNull Context context,
            @NonNull String packageName) {
        return getInstallerPackageNameAndInstallSourceInfo(context, packageName).first;
    }

    /** Returns a link to the installer app store for a given package name. */
    @Nullable
    public static Intent getAppStoreLink(Context context, String installerPackageName,
            String packageName) {
        Intent intent = new Intent(Intent.ACTION_SHOW_APP_INFO)
                .setPackage(installerPackageName);
        final Intent result = resolveIntent(context, intent);
        if (result != null) {
            result.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
            return result;
        }
        return null;
    }

    /** Convenience method that looks up the installerPackageName for you. */
    @Nullable
    public static Intent getAppStoreLink(Context context, String packageName) {
      String installerPackageName = getInstallerPackageName(context, packageName);
      return getAppStoreLink(context, installerPackageName, packageName);
    }

    /**
     * Returns {@code true} when the initiating package is different from installing package
     * for the given {@link InstallSourceInfo install source}. Otherwise, returns {@code false}.
     * If the {@code source} is null, also return {@code false}.
     */
    public static boolean isInitiatedFromDifferentPackage(@Nullable InstallSourceInfo source) {
        if (source == null) {
            return false;
        }
        final String initiatingPackageName = source.getInitiatingPackageName();
        return initiatingPackageName != null
                && !TextUtils.equals(source.getInstallingPackageName(), initiatingPackageName);
    }
}
