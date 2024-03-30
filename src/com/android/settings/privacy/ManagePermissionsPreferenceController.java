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

package com.android.settings.privacy;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * The preference controller for managing permissions
 */
public class ManagePermissionsPreferenceController extends BasePreferenceController {

    public ManagePermissionsPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setIntent(new Intent(Intent.ACTION_MANAGE_PERMISSIONS)
                    .setPackage(mContext.getPackageManager().getPermissionControllerPackageName()));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
