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
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureFlags;
import android.content.pm.FeatureFlagsImpl;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.ApplicationInfoFlags;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

public abstract class AppCounter extends AsyncTask<Void, Void, Integer> {

    protected final PackageManager mPm;
    protected final UserManager mUm;
    protected final FeatureFlags mFf;

    @VisibleForTesting
    AppCounter(@NonNull Context context, @NonNull PackageManager packageManager,
            @NonNull FeatureFlags featureFlags) {
        mPm = packageManager;
        mUm = context.getSystemService(UserManager.class);
        mFf = featureFlags;
    }

    public AppCounter(@NonNull Context context, @NonNull PackageManager packageManager) {
        this(context, packageManager, new FeatureFlagsImpl());
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int count = 0;
        for (UserInfo user : mUm.getProfiles(UserHandle.myUserId())) {
            long flags = PackageManager.GET_DISABLED_COMPONENTS
                    | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                    | (mFf.archiving() ? PackageManager.MATCH_ARCHIVED_PACKAGES : 0)
                    | (user.isAdmin() ? PackageManager.MATCH_ANY_USER : 0);
            ApplicationInfoFlags infoFlags = ApplicationInfoFlags.of(flags);
            final List<ApplicationInfo> list =
                    mPm.getInstalledApplicationsAsUser(infoFlags, user.id);
            for (ApplicationInfo info : list) {
                if (includeInCount(info)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    protected void onPostExecute(Integer count) {
        onCountComplete(count);
    }

    void executeInForeground() {
        onPostExecute(doInBackground());
    }

    protected abstract void onCountComplete(int num);

    protected abstract boolean includeInCount(ApplicationInfo info);
}
