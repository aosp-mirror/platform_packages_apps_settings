/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.display.AdaptiveSleepPreferenceController.hasSufficientPermission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

/**
 * The controller of Screen attention's permission warning preference. The preference appears when
 * the camera permission is missing for Screen Attention feature.
 */
public class AdaptiveSleepPermissionPreferenceController {
    @VisibleForTesting
    Preference mPreference;
    private PackageManager mPackageManager;

    public AdaptiveSleepPermissionPreferenceController(Context context) {
        final String packageName = context.getPackageManager().getAttentionServicePackageName();
        mPackageManager = context.getPackageManager();
        final Intent intent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        mPreference = new Preference(context);
        mPreference.setTitle(R.string.adaptive_sleep_title_no_permission);
        mPreference.setSummary(R.string.adaptive_sleep_summary_no_permission);
        mPreference.setIcon(R.drawable.ic_info_outline_24);
        mPreference.setOnPreferenceClickListener(p -> {
            context.startActivity(intent);
            return true;
        });
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(PreferenceScreen screen) {
        if (!hasSufficientPermission(mPackageManager)) {
            screen.addPreference(mPreference);
        }
    }

    /**
     * Refreshes the visibility of the preference.
     */
    public void updateVisibility() {
        mPreference.setVisible(!hasSufficientPermission(mPackageManager));
    }
}
