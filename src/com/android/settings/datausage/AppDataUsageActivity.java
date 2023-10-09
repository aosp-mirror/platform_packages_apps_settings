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

package com.android.settings.datausage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settingslib.AppItem;

/**
 * Standalone activity used to launch {@link AppDataUsage} from a
 * {@link Settings#ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS} intent.
 */
public class AppDataUsageActivity extends SettingsActivity {

    private static final boolean DEBUG = false;
    private static final String TAG = "AppDataUsageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Intent intent = getIntent();
        final String packageName = intent.getData().getSchemeSpecificPart();
        final PackageManager pm = getPackageManager();
        final int uid;
        try {
            uid = pm.getPackageUid(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "invalid package: " + packageName);
            try {
                // Activity lifecycle still requires calling onCreate()
                super.onCreate(savedInstanceState);
            } catch (Exception e2) {
                // Ignore - most likely caused by SettingsActivity because of invalid fragment
                if (DEBUG) Log.d(TAG, "onCreate() exception", e);
            } finally {
                finish();
            }
            return;
        }
        if (DEBUG) Log.d(TAG, "Package: " + packageName + " UID: " + uid);

        final Bundle args = new Bundle();
        final AppItem appItem = new AppItem(uid);
        appItem.addUid(uid);
        args.putParcelable(AppDataUsage.ARG_APP_ITEM, appItem);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT, AppDataUsage.class.getName());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AppDataUsage.class.getName().equals(fragmentName);
    }
}
