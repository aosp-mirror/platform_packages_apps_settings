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
import android.content.pm.UserInfo;
import android.text.TextUtils;

import java.util.Objects;

/**
 * Simple class for bringing together information about application and user for which it was
 * installed.
 */
public class UserAppInfo {
    public final UserInfo userInfo;
    public final ApplicationInfo appInfo;

    public UserAppInfo(UserInfo mUserInfo, ApplicationInfo mAppInfo) {
        this.userInfo = mUserInfo;
        this.appInfo = mAppInfo;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final UserAppInfo that = (UserAppInfo) other;

        // As UserInfo and AppInfo do not support hashcode/equals contract, assume
        // equality based on corresponding identity fields.
        return that.userInfo.id == userInfo.id && TextUtils.equals(that.appInfo.packageName,
                appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo.id, appInfo.packageName);
    }
}
