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
import android.util.Log;

// This class provides methods that help dealing with app stores.
public class AppStoreUtil {
    private static final String LOG_TAG = "AppStoreUtil";

    private static Intent resolveIntent(Context context, Intent i) {
        ResolveInfo result = context.getPackageManager().resolveActivity(i, 0);
        return result != null ? new Intent(i.getAction())
                .setClassName(result.activityInfo.packageName, result.activityInfo.name) : null;
    }

    // Returns the package name of the app that we consider to be the user-visible 'installer'
    // of given packageName, if one is available.
    public static String getInstallerPackageName(Context context, String packageName) {
        String installerPackageName;
        try {
            InstallSourceInfo source =
                    context.getPackageManager().getInstallSourceInfo(packageName);
            // By default, use the installing package name.
            installerPackageName = source.getInstallingPackageName();
            // Use the recorded originating package name only if the initiating package is a system
            // app (eg. Package Installer). The originating package is not verified by the platform,
            // so we choose to ignore this when supplied by a non-system app.
            String originatingPackageName = source.getOriginatingPackageName();
            String initiatingPackageName = source.getInitiatingPackageName();
            if (originatingPackageName != null && initiatingPackageName != null) {
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
        return installerPackageName;
    }

    // Returns a link to the installer app store for a given package name.
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

    // Convenience method that looks up the installerPackageName for you.
    public static Intent getAppStoreLink(Context context, String packageName) {
      String installerPackageName = getInstallerPackageName(context, packageName);
      return getAppStoreLink(context, installerPackageName, packageName);
    }
}
