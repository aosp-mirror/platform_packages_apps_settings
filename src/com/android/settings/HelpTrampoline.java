/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.HelpUtils;

public class HelpTrampoline extends Activity {
    private static final String TAG = "HelpTrampoline";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            final String name = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (TextUtils.isEmpty(name)) {
                finishAndRemoveTask();
                return;
            }

            final int id = getResources().getIdentifier(name, "string", getPackageName());
            final String value = getResources().getString(id);

            final Intent intent = HelpUtils.getHelpIntent(this, value, null);
            if (intent != null) {
                /*
                 * TODO: b/38230998.
                 * Move to startActivity once the HelpUtils.getHelpIntent is refactored
                 */
                startActivityForResult(intent, 0);
            }

        } catch (Resources.NotFoundException | ActivityNotFoundException e) {
            Log.w(TAG, "Failed to resolve help", e);
        }

        finish();
    }
}
