/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.gup;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class GupEnableForAllAppsPreferenceController
        extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    public static final int GUP_DEFAULT = 0;
    public static final int GUP_ALL_APPS = 1;

    private final ContentResolver mContentResolver;

    public GupEnableForAllAppsPreferenceController(Context context, String key) {
        super(context, key);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                ? AVAILABLE
                : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SwitchPreference switchPreference =
                (SwitchPreference) screen.findPreference(getPreferenceKey());
        if (switchPreference == null) {
            return;
        }

        switchPreference.setChecked(Settings.Global.getInt(mContentResolver,
                                            Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT)
                == GUP_ALL_APPS);
        switchPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // When developer option is present, always overwrite GUP_DEV_ALL_APPS.
        Settings.Global.putInt(mContentResolver, Settings.Global.GUP_DEV_ALL_APPS,
                (boolean) newValue ? GUP_ALL_APPS : GUP_DEFAULT);

        return true;
    }
}
