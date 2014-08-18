/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.List;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.GET_RESOLVED_FILTER;

/**
 * Listens to {@link Intent.ACTION_BOOT_COMPLETED} and {@link Intent.ACTION_PRE_BOOT_COMPLETED}
 * performs setup steps for a managed profile (disables the launcher icon of the Settings app and
 * adds cross-profile intent filters for the appropriate Settings activities).
 */
public class ManagedProfileSetup extends BroadcastReceiver {
    private static final String TAG = "Settings";
    private static final String PRIMARY_PROFILE_SETTING =
            "com.android.settings.PRIMARY_PROFILE_CONTROLLED";

    @Override
    public void onReceive(Context context, Intent broadcast) {
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (!Utils.isManagedProfile(um)) {
            return;
        }
        Log.i(TAG, "Received broadcast: " + broadcast.getAction()
                + ". Setting up intent forwarding for managed profile.");
        final PackageManager pm  = context.getPackageManager();
        // Clear any previous intent forwarding we set up
        pm.clearCrossProfileIntentFilters(UserHandle.myUserId());

        // Set up intent forwarding for implicit intents
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage(context.getPackageName());

        // Resolves activities for the managed profile (which we're running as)
        List<ResolveInfo> resolvedIntents = pm.queryIntentActivities(intent,
                GET_ACTIVITIES | GET_META_DATA | GET_RESOLVED_FILTER);
        final int count = resolvedIntents.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = resolvedIntents.get(i);
            if (info.filter != null && info.activityInfo != null
                    && info.activityInfo.metaData != null) {
                boolean shouldForward = info.activityInfo.metaData.getBoolean(
                        PRIMARY_PROFILE_SETTING);
                if (shouldForward) {
                    pm.addCrossProfileIntentFilter(info.filter, UserHandle.myUserId(),
                        UserHandle.USER_OWNER, PackageManager.SKIP_CURRENT_PROFILE);
                }
            }
        }

        // Disable launcher icon
        // Note: This needs to happen after forwarding intents, otherwise the main Settings
        // intent gets lost
        ComponentName settingsComponentName = new ComponentName(context, Settings.class);
        pm.setComponentEnabledSetting(settingsComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
