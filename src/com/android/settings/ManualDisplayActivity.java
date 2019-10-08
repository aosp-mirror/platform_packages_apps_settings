/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * The "dialog" that shows from "Manual" in the Settings app.
 */
public class ManualDisplayActivity extends Activity {
    private static final String TAG = "SettingsManualActivity";

    private static final String MANUAL_PATH = "/system/etc/MANUAL.html.gz";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = getResources();

        if (!resources.getBoolean(R.bool.config_show_manual)) {
            finish();   // No manual to display for this device
        }

        final File file = new File(MANUAL_PATH);
        if (!file.exists() || file.length() == 0) {
            Log.e(TAG, "Manual file " + MANUAL_PATH + " does not exist");
            showErrorAndFinish();
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "text/html");

        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_manual_activity_title));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.htmlviewer");

        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to find viewer", e);
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_manual_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }
}
