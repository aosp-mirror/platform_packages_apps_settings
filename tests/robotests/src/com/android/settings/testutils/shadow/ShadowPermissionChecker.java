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

package com.android.settings.testutils.shadow;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class provides shadow for API that is not supported in current roboletric
 */
@Implements(PermissionChecker.class)
public class ShadowPermissionChecker {

    private static Map<PermissionInfo, Integer> sPermissions = new HashMap<>();

    public static void clear() {
        sPermissions.clear();
    }

    public static void addPermission(String permission, int pid, int uid, String packageName,
            int permissionValue) {
        sPermissions.put(new PermissionInfo(permission, pid, uid, packageName), permissionValue);
    }

    @Implementation
    public static int checkPermission(@NonNull Context context, @NonNull String permission,
            int pid, int uid, String packageName) {
        return sPermissions.getOrDefault(new PermissionInfo(permission, pid, uid, packageName),
                PackageManager.PERMISSION_DENIED);
    }

    private static class PermissionInfo {
        private final int mPid;
        private final int mUid;
        private final String mPackageName;
        private final String mPermission;

        public PermissionInfo(String permission, int pid, int uid, String packageName) {
            mPid = pid;
            mUid = uid;
            mPackageName = packageName;
            mPermission = permission;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PermissionInfo)) {
                return false;
            }

            final PermissionInfo other = (PermissionInfo) obj;
            return mPid == other.mPid
                    && mUid == other.mUid
                    && TextUtils.equals(mPackageName, other.mPackageName)
                    && TextUtils.equals(mPermission, other.mPermission);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mPid, mUid, mPackageName, mPermission);
        }
    }
}
