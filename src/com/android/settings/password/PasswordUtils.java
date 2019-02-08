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

package com.android.settings.password;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.Utils;

public final class PasswordUtils extends com.android.settingslib.Utils {

    private static final String TAG = "Settings";

    /**
     * Returns whether the uid which the activity with {@code activityToken} is launched from has
     * been granted the {@code permission}.
     */
    public static boolean isCallingAppPermitted(Context context, IBinder activityToken,
            String permission) {
        try {
            return context.checkPermission(permission, /* pid= */ -1,
                    ActivityManager.getService().getLaunchedFromUid(activityToken))
                    == PackageManager.PERMISSION_GRANTED;
        } catch (RemoteException e) {
            Log.v(TAG, "Could not talk to activity manager.", e);
            return false;
        }
    }

    /**
     * Returns the label of the package which the activity with {@code activityToken} is launched
     * from or {@code null} if it is launched from the settings app itself.
     */
    @Nullable
    public static CharSequence getCallingAppLabel(Context context, IBinder activityToken) {
        String pkg = getCallingAppPackageName(activityToken);
        if (pkg == null || pkg.equals(SETTINGS_PACKAGE_NAME)) {
            return null;
        }

        return Utils.getApplicationLabel(context, pkg);
    }

    /**
     * Returns the package name which the activity with {@code activityToken} is launched from.
     */
    @Nullable
    public static String getCallingAppPackageName(IBinder activityToken) {
        String pkg = null;
        try {
            pkg = ActivityManager.getService().getLaunchedFromPackage(activityToken);
        } catch (RemoteException e) {
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
        return pkg;
    }

    /** Crashes the calling application and provides it with {@code message}. */
    public static void crashCallingApplication(IBinder activityToken, String message) {
        IActivityManager am = ActivityManager.getService();
        try {
            int uid = am.getLaunchedFromUid(activityToken);
            int userId = UserHandle.getUserId(uid);
            am.crashApplication(
                    uid,
                    /* initialPid= */ -1,
                    getCallingAppPackageName(activityToken),
                    userId,
                    message);
        } catch (RemoteException e) {
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
    }
}
