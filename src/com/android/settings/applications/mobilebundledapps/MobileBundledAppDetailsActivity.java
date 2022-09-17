/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.mobilebundledapps;

import android.content.Intent;
import android.net.Uri;

import com.android.settings.SettingsActivity;
/**
 * An activity that is used to parse and display mobile-bundled apps application metadata xml file.
 */
public class MobileBundledAppDetailsActivity extends SettingsActivity {
    public static final String ACTION_TRANSPARENCY_METADATA =
            "android.settings.TRANSPARENCY_METADATA";

    public MobileBundledAppDetailsActivity() {
        super();
    }

    @Override
    public Intent getIntent() {
        final Intent modIntent = new Intent(super.getIntent());
        modIntent.setData(Uri.parse("package:"
                + super.getIntent().getExtra(Intent.EXTRA_PACKAGE_NAME).toString()));
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, MobileBundledAppsDetailsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return MobileBundledAppsDetailsFragment.class.getName().equals(fragmentName);
    }
}
