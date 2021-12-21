/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.applications.appinfo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppInfoBase;

/** Activity for the entry of {@link #AppLocaleDetails} from outside Settings app. */
public class AppLocalePickerActivity extends SettingsActivity {
    private static final String TAG = "AppLocalePickerActivity";

    @Override
    protected void onCreate(Bundle savedState) {
        Intent intent = getEntryIntent(getIntent());
        if (intent == null) {
            finish();
            return;
        }
        setIntent(intent);
        super.onCreate(savedState);
    }

    @VisibleForTesting
    Intent getEntryIntent(Intent intent) {
        String callingPackage = getCallingPackage();
        if (callingPackage == null || callingPackage.isEmpty()) {
            Log.d(TAG, "No calling package name is found.");
            return null;
        }
        final Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, callingPackage);
        return intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, fragmentArgs);
    }
}
