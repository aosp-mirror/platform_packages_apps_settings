/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Fragment;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.view.View;

import java.util.List;

public class ApplicationFeatureProviderImpl implements ApplicationFeatureProvider {

    private final Context mContext;
    private final PackageManagerWrapper mPm;
    private final UserManager mUm;

    public ApplicationFeatureProviderImpl(Context context, PackageManagerWrapper pm) {
        mContext = context.getApplicationContext();
        mPm = pm;
        mUm = UserManager.get(mContext);
    }

    @Override
    public AppHeaderController newAppHeaderController(Fragment fragment, View appHeader) {
        return new AppHeaderController(mContext, fragment, appHeader);
    }

    @Override
    public void calculateNumberOfInstalledApps(int installReason,
            NumberOfInstalledAppsCallback callback) {
        new AllUserInstalledAppCounter(installReason, callback).execute();
    }

    private class AllUserInstalledAppCounter extends InstalledAppCounter {
        private NumberOfInstalledAppsCallback mCallback;

        AllUserInstalledAppCounter(int installReason, NumberOfInstalledAppsCallback callback) {
            super(mContext, installReason, ApplicationFeatureProviderImpl.this.mPm);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfInstalledAppsResult(num);
        }

        @Override
        protected List<UserInfo> getUsersToCount() {
            return mUm.getUsers(true /* excludeDying */);
        }
    }
}
