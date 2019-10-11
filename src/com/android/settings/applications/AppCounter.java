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
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.List;

public abstract class AppCounter extends AsyncTask<Void, Void, Integer> {

    protected final PackageManager mPm;
    protected final UserManager mUm;

    public AppCounter(Context context, PackageManager packageManager) {
        mPm = packageManager;
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int count = 0;
        for (UserInfo user : mUm.getProfiles(UserHandle.myUserId())) {
            final List<ApplicationInfo> list =
                    mPm.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                            | (user.isAdmin() ? PackageManager.MATCH_ANY_USER : 0),
                            user.id);
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
