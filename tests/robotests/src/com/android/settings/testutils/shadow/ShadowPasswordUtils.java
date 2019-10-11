/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.content.Context;
import android.os.IBinder;

import com.android.settings.password.PasswordUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Implements(PasswordUtils.class)
public class ShadowPasswordUtils {

    private static String sCallingAppLabel;
    private static String sCallingAppPackageName;
    private static Set<String> sGrantedPermissions;

    public static void reset() {
        sCallingAppLabel = null;
        sGrantedPermissions = null;
        sCallingAppPackageName = null;
    }

    @Implementation
    protected static boolean isCallingAppPermitted(Context context, IBinder activityToken,
            String permission) {
        if (sGrantedPermissions == null) {
            return false;
        }
        return sGrantedPermissions.contains(permission);
    }

    public static void addGrantedPermission(String... permissions) {
        if (sGrantedPermissions == null) {
            sGrantedPermissions = new HashSet<>();
        }
        sGrantedPermissions.addAll(Arrays.asList(permissions));
    }

    @Implementation
    protected static String getCallingAppLabel(Context context, IBinder activityToken) {
        return sCallingAppLabel;
    }

    public static void setCallingAppLabel(String label) {
        sCallingAppLabel = label;
    }

    @Implementation
    protected static String getCallingAppPackageName(IBinder activityToken) {
        return sCallingAppPackageName;
    }

    public static void setCallingAppPackageName(String packageName) {
        sCallingAppPackageName = packageName;
    }
}
