/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SimulateColorSpacePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String SIMULATE_COLOR_SPACE = "simulate_color_space";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;

    public SimulateColorSpacePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return SIMULATE_COLOR_SPACE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSimulateColorSpace(newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSimulateColorSpace();
    }

    @Override
    public void onDeveloperOptionsDisabled() {
        super.onDeveloperOptionsDisabled();
        if (usingDevelopmentColorSpace()) {
            writeSimulateColorSpace(-1);
        }
    }

    private void updateSimulateColorSpace() {
        final ContentResolver cr = mContext.getContentResolver();
        final boolean enabled = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_OFF)
                != SETTING_VALUE_OFF;
        final ListPreference listPreference = (ListPreference) mPreference;
        if (enabled) {
            final String mode = Integer.toString(Settings.Secure.getInt(
                    cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                    AccessibilityManager.DALTONIZER_DISABLED));
            listPreference.setValue(mode);
            final int index = listPreference.findIndexOfValue(mode);
            if (index < 0) {
                final Resources res = mContext.getResources();
                // We're using a mode controlled by accessibility preferences.
                listPreference.setSummary(
                        res.getString(com.android.settingslib.R.string.daltonizer_type_overridden,
                        res.getString(com.android.settingslib.R
                                .string.accessibility_display_daltonizer_preference_title)));
            } else {
                listPreference.setSummary("%s");
            }
        } else {
            listPreference.setValue(
                    Integer.toString(AccessibilityManager.DALTONIZER_DISABLED));
        }
    }

    private void writeSimulateColorSpace(Object value) {
        final ContentResolver cr = mContext.getContentResolver();
        final int newMode = Integer.parseInt(value.toString());
        if (newMode < 0) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                    SETTING_VALUE_OFF);
        } else {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                    SETTING_VALUE_ON);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, newMode);
        }
    }

    /**
     * @return <code>true</code> if the color space preference is currently
     * controlled by development settings
     */
    private boolean usingDevelopmentColorSpace() {
        final ContentResolver cr = mContext.getContentResolver();
        final boolean enabled = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_OFF)
                != SETTING_VALUE_OFF;
        if (enabled) {
            final String mode = Integer.toString(Settings.Secure.getInt(
                    cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                    AccessibilityManager.DALTONIZER_DISABLED));
            final int index = ((ListPreference) mPreference).findIndexOfValue(mode);
            if (index >= 0) {
                // We're using a mode controlled by developer preferences.
                return true;
            }
        }
        return false;
    }
}
