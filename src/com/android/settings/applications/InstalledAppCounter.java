/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureFlags;
import android.content.pm.FeatureFlagsImpl;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

public abstract class InstalledAppCounter extends AppCounter {

    /**
     * Count all installed packages, irrespective of install reason.
     */
    public static final int IGNORE_INSTALL_REASON = -1;

    private final int mInstallReason;

    public InstalledAppCounter(@NonNull Context context, int installReason,
            @NonNull PackageManager packageManager) {
        this(context, installReason, packageManager, new FeatureFlagsImpl());
    }

    @VisibleForTesting
    InstalledAppCounter(@NonNull Context context, int installReason,
            @NonNull PackageManager packageManager, @NonNull FeatureFlags featureFlags) {
        super(context, packageManager, featureFlags);
        mInstallReason = installReason;
    }

    @Override
    protected boolean includeInCount(ApplicationInfo info) {
        return includeInCount(mInstallReason, mPm, info);
    }

    public static boolean includeInCount(int installReason, PackageManager pm,
            ApplicationInfo info) {
        final int userId = UserHandle.getUserId(info.uid);
        if (installReason != IGNORE_INSTALL_REASON
                && pm.getInstallReason(info.packageName,
                        new UserHandle(userId)) != installReason) {
            return false;
        }
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true;
        }
        if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(info.packageName);
        List<ResolveInfo> intents = pm.queryIntentActivitiesAsUser(
                launchIntent,
                PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE,
                userId);
        return intents != null && intents.size() != 0;
    }
}
