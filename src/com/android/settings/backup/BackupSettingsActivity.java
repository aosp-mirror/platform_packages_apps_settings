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

package com.android.settings.backup;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;


/**
 * The activity used to launch the configured Backup activity or the preference screen
 * if the manufacturer provided their backup settings.
 */
public class BackupSettingsActivity extends Activity {
    private static final String TAG = "BackupSettingsActivity";
    private FragmentManager mFragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackupSettingsHelper backupHelper = new BackupSettingsHelper(this);

        if (!backupHelper.isBackupProvidedByManufacturer()) {
            // If manufacturer specific backup settings are not provided then launch
            // the backup settings provided by backup transport or the default one directly.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "No manufacturer settings found, launching the backup settings directly");
            }
            Intent intent = backupHelper.getIntentForBackupSettings();
            // enable the activity before launching it
            getPackageManager().setComponentEnabledSetting(intent.getComponent(),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            // use startActivityForResult to let the activity check the caller signature
            startActivityForResult(intent, 1);
            finish();
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Manufacturer provided backup settings, showing the preference screen");
            }
            // mFragmentManager can be set by {@link #setFragmentManager()} for testing
            if (mFragmentManager == null) {
                mFragmentManager = getFragmentManager();
            }
            mFragmentManager.beginTransaction()
                    .replace(android.R.id.content, new BackupSettingsFragment())
                    .commit();
        }
    }

    @VisibleForTesting
    void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }
}