/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.Settings.PrivacySettingsActivity;

/**
 * A trampoline activity used to launch the configured Backup activity.
 * This activity used the theme NoDisplay to minimize the flicker that might be seen for the launch-
 * finsih transition.
 */
public class BackupSettingsActivity extends Activity {
    private static final String TAG = "BackupSettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackupSettingsHelper backupHelper = new BackupSettingsHelper();
        if (backupHelper.isIntentProvidedByTransport(getPackageManager())) {
            Intent intent = backupHelper.getIntentForBackupSettings();
            if (intent != null) {
                // use startActivityForResult to let the activity check the caller signature
                startActivityForResult(intent, -1);
            }
        } else {
            // This should never happen, because isIntentProvidedByTransport() is called before
            // starting this activity.
            Log.e(TAG, "Backup transport has not provided an intent"
                    + " or the component for the intent is not found!");
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(getPackageName(), PrivacySettingsActivity.class.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            startActivityForResult(new Intent(this, PrivacySettingsActivity.class), -1);
        }
        finish();
    }
}