/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;

public class AdvancedAppInfoPreferenceCategoryController extends PreferenceCategoryController {

    private ApplicationsState.AppEntry mAppEntry;

    public AdvancedAppInfoPreferenceCategoryController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null && !AppUtils.isAppInstalled(mAppEntry)) {
            preference.setEnabled(false);
        }
        super.displayPreference(screen);
    }

    public void setAppEntry(ApplicationsState.AppEntry appEntry) {
        mAppEntry = appEntry;
    }
}
