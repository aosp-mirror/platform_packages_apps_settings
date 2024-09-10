/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.util.Log;

/** Broadcast receiver for enabling/disabling Private Space Root Activity. */
public class PrivateSpaceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "PrivateSpaceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (android.multiuser.Flags.enablePrivateSpaceFeatures()
                && android.multiuser.Flags.blockPrivateSpaceCreation()) {
            Log.d(TAG, "Received Intent: " + intent.getAction());
            PrivateSpaceMaintainer privateSpaceMaintainer =
                    PrivateSpaceMaintainer.getInstance(context);
            // Disable the PrivateSpaceAuthenticationActivity when
            // -Private Profile is not present and
            // -Private Profile cannot be added.
            final int enableState = privateSpaceMaintainer.doesPrivateSpaceExist()
                    || context.getSystemService(UserManager.class).canAddPrivateProfile()
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            ComponentName privateSpaceAuth = new ComponentName(context,
                    PrivateSpaceAuthenticationActivity.class);
            Log.d(TAG, "Setting component " + privateSpaceAuth + " state: " + enableState);
            context.getPackageManager().setComponentEnabledSetting(
                    privateSpaceAuth,
                    enableState,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
