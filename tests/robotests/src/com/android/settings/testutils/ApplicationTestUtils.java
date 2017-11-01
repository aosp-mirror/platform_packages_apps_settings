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

package com.android.settings.testutils;

import android.content.pm.ApplicationInfo;

/**
 * Helper for mocking installed applications.
 */
public class ApplicationTestUtils {
    /**
     * Create and populate an {@link android.content.pm.ApplicationInfo} object that describes an
     * installed app.
     *
     * @param uid The app's uid
     * @param packageName The app's package name.
     * @param flags Flags describing the app. See {@link android.content.pm.ApplicationInfo#flags}
     *         for possible values.
     * @param targetSdkVersion The app's target SDK version
     *
     * @see android.content.pm.ApplicationInfo
     */
    public static ApplicationInfo buildInfo(int uid, String packageName, int flags,
            int targetSdkVersion) {
        final ApplicationInfo info = new ApplicationInfo();
        info.uid = uid;
        info.packageName = packageName;
        info.flags = flags;
        info.targetSdkVersion = targetSdkVersion;
        return info;
    }
}
