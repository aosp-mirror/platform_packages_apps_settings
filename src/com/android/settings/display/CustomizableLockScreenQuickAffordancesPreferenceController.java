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

package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

/**
 * Preference for accessing an experience to customize lock screen quick affordances.
 */
public class CustomizableLockScreenQuickAffordancesPreferenceController extends
        BasePreferenceController implements PreferenceControllerMixin {

    public CustomizableLockScreenQuickAffordancesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return CustomizableLockScreenUtils.isFeatureEnabled(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                final String packageName =
                        mContext.getString(R.string.config_wallpaper_picker_package);
                if (!TextUtils.isEmpty(packageName)) {
                    intent.setPackage(packageName);
                }
                intent.putExtra("destination", "quick_affordances");
                mContext.startActivity(intent);
                return true;
            });
            refreshSummary(preference);
        }
    }

    @Override
    public CharSequence getSummary() {
        return CustomizableLockScreenUtils.getQuickAffordanceSummary(mContext);
    }
}
