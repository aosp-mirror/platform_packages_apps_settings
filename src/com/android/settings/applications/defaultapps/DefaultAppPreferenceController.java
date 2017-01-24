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

package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.core.PreferenceController;

public abstract class DefaultAppPreferenceController extends PreferenceController {

    private static final String TAG = "DefaultAppPrefControl";

    protected final PackageManagerWrapper mPackageManager;
    protected final UserManager mUserManager;

    protected int mUserId;

    public DefaultAppPreferenceController(Context context) {
        super(context);
        mPackageManager = new PackageManagerWrapperImpl(context.getPackageManager());
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mUserId = UserHandle.myUserId();
    }

    @Override
    public void updateState(Preference preference) {
        final DefaultAppInfo app = getDefaultAppInfo();
        CharSequence defaultAppLabel = null;
        if (app != null) {
            defaultAppLabel = app.loadLabel(mPackageManager.getPackageManager());
        }
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            preference.setSummary(defaultAppLabel);
        } else {
            Log.d(TAG, "No default app");
        }
    }

    protected abstract DefaultAppInfo getDefaultAppInfo();
}
