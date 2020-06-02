/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * A controller helps enable or disable a developer setting which allows non system overlays on
 * Settings app.
 */
public class OverlaySettingsPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    public static final String SHARE_PERFS = "overlay_settings";
    private static final String KEY_OVERLAY_SETTINGS = "overlay_settings";

    public OverlaySettingsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_OVERLAY_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setOverlaySettingsEnabled(mContext, (Boolean) newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(isOverlaySettingsEnabled(mContext));
    }

    /**
     * Check if this setting is enabled or not.
     */
    public static boolean isOverlaySettingsEnabled(Context context) {
        final SharedPreferences editor = context.getSharedPreferences(SHARE_PERFS,
                Context.MODE_PRIVATE);
        return editor.getBoolean(SHARE_PERFS, false /* defValue */);
    }

    /**
     * Enable this setting.
     */
    @VisibleForTesting
    static void setOverlaySettingsEnabled(Context context, boolean enabled) {
        final SharedPreferences editor = context.getSharedPreferences(SHARE_PERFS,
                Context.MODE_PRIVATE);
        editor.edit().putBoolean(SHARE_PERFS, enabled).apply();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        setOverlaySettingsEnabled(mContext, false);
    }
}
