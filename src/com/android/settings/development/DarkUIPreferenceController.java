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
 * limitations under the License
 */

package com.android.settings.development;

import android.app.UiModeManager;
import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DarkUIPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String DARK_UI_KEY = "dark_ui_mode";
    private final UiModeManager mUiModeManager;

    public DarkUIPreferenceController(Context context) {
        this(context, context.getSystemService(UiModeManager.class));
    }

    @VisibleForTesting
    DarkUIPreferenceController(Context context, UiModeManager uiModeManager) {
        super(context);
        mUiModeManager = uiModeManager;
    }

    @Override
    public String getPreferenceKey() {
        return DARK_UI_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mUiModeManager.setNightMode(modeToInt((String) newValue));
        updateSummary(preference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference);
    }

    private void updateSummary(Preference preference) {
        int mode = mUiModeManager.getNightMode();
        ((ListPreference) preference).setValue(modeToString(mode));
        preference.setSummary(modeToDescription(mode));
    }

    private String modeToDescription(int mode) {
        String[] values = mContext.getResources().getStringArray(R.array.dark_ui_mode_entries);
        switch (mode) {
            case UiModeManager.MODE_NIGHT_AUTO:
                return values[0];
            case UiModeManager.MODE_NIGHT_YES:
                return values[1];
            case UiModeManager.MODE_NIGHT_NO:
            default:
                return values[2];

        }
    }

    private String modeToString(int mode) {
        switch (mode) {
            case UiModeManager.MODE_NIGHT_AUTO:
                return "auto";
            case UiModeManager.MODE_NIGHT_YES:
                return "yes";
            case UiModeManager.MODE_NIGHT_NO:
            default:
                return "no";

        }
    }

    private int modeToInt(String mode) {
        switch (mode) {
            case "auto":
                return UiModeManager.MODE_NIGHT_AUTO;
            case "yes":
                return UiModeManager.MODE_NIGHT_YES;
            case "no":
            default:
                return UiModeManager.MODE_NIGHT_NO;
        }
    }
}
