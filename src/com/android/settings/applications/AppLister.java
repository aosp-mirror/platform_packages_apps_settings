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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists apps for current user that fit some criteria specified by includeInCount method
 * implementation.
 * This class is similar to {@link AppCounter} class, but but builds actual list of apps instead
 * of just counting them.
 */
public abstract class AppLister extends AsyncTask<Void, Void, List<UserAppInfo>> {
    protected final PackageManager mPm;
    protected final UserManager mUm;

    public AppLister(PackageManager packageManager, UserManager userManager) {
        mPm = packageManager;
        mUm = userManager;
    }

    @Override
    protected List<UserAppInfo> doInBackground(Void... params) {
        final List<UserAppInfo> result = new ArrayList<>();
        for (UserInfo user : mUm.getProfiles(UserHandle.myUserId())) {
            final List<ApplicationInfo> list =
                    mPm.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                            | (user.isAdmin() ? PackageManager.MATCH_ANY_USER : 0),
                            user.id);
            for (ApplicationInfo info : list) {
                if (includeInCount(info)) {
                    result.add(new UserAppInfo(user, info));
                }
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(List<UserAppInfo> list) {
        onAppListBuilt(list);
    }

    protected abstract void onAppListBuilt(List<UserAppInfo> list);
    protected abstract boolean includeInCount(ApplicationInfo info);
}
