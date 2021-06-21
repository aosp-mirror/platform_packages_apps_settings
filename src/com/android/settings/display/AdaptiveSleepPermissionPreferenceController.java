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

import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of Screen attention's permission warning preference. The preference appears when
 * the camera permission is missing for Screen Attention feature.
 */
public class AdaptiveSleepPermissionPreferenceController {
    @VisibleForTesting
    BannerMessagePreference mPreference;
    private final PackageManager mPackageManager;
    private final Context mContext;

    public AdaptiveSleepPermissionPreferenceController(Context context) {
        mPackageManager = context.getPackageManager();
        mContext = context;
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(PreferenceScreen screen) {
        initializePreference();
        if (!hasSufficientPermission(mPackageManager)) {
            screen.addPreference(mPreference);
        }
    }

    /**
     * Refreshes the visibility of the preference.
     */
    public void updateVisibility() {
        initializePreference();
        mPreference.setVisible(!hasSufficientPermission(mPackageManager));
    }

    private void initializePreference() {
        if (mPreference == null) {
            final String packageName =
                    mContext.getPackageManager().getAttentionServicePackageName();
            final Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            mPreference = new BannerMessagePreference(mContext);
            mPreference.setTitle(R.string.adaptive_sleep_title_no_permission);
            mPreference.setSummary(R.string.adaptive_sleep_summary_no_permission);
            mPreference.setPositiveButtonText(R.string.adaptive_sleep_manage_permission_button);
            mPreference.setPositiveButtonOnClickListener(p -> {
                mContext.startActivity(intent);
            });
        }
    }

}
