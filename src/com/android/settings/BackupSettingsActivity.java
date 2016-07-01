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
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;

import java.net.URISyntaxException;

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
        String backup = getResources().getString(R.string.config_backup_settings_intent);
        if (!TextUtils.isEmpty(backup)) {
            try {
                Intent intent = Intent.parseUri(backup, 0);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    // use startActivityForResult to let the activity check the caller signature
                    startActivityForResult(intent, -1);
                } else {
                    Log.e(TAG, "Backup component not found!");
                }
            } catch (URISyntaxException e) {
                Log.e(TAG, "Invalid backup component URI!", e);
            }
        }
        finish();
    }
}