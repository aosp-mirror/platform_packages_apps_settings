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
import android.os.UserManager;

/**
 * Listens to {@link Intent.ACTION_BOOT_COMPLETED} and {@link Intent.ACTION_PRE_BOOT_COMPLETED} and
 * disables the launcher icon of the Settings app for a managed profile.
 */
public class LauncherIconToggler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                || intent.getAction().equals(Intent.ACTION_PRE_BOOT_COMPLETED)) {
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (Utils.isManagedProfile(um)) {
                ComponentName launcherActivity = new ComponentName(context, Settings.class);
                final PackageManager pm  = context.getPackageManager();
                pm.setComponentEnabledSetting(launcherActivity,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }
}
