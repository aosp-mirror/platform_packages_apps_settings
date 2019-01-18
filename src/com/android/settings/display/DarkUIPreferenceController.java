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

package com.android.settings.display;

import android.app.UiModeManager;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class DarkUIPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private UiModeManager mUiModeManager;

    public DarkUIPreferenceController(Context context, String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
    }

    @VisibleForTesting
    void setUiModeManager(UiModeManager uiModeManager) {
        mUiModeManager = uiModeManager;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        int value = mUiModeManager.getNightMode();
        ListPreference preference = screen.findPreference(getPreferenceKey());
        preference.setValue(modeToString(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mUiModeManager.setNightMode(modeToInt((String) newValue));
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return modeToDescription(mUiModeManager.getNightMode());
    }

    private String modeToDescription(int mode) {
        String[] values = mContext.getResources().getStringArray(R.array.dark_ui_mode_entries);
        switch (mode) {
            case UiModeManager.MODE_NIGHT_YES:
                return values[0];
            case UiModeManager.MODE_NIGHT_NO:
            case UiModeManager.MODE_NIGHT_AUTO:
            default:
                return values[1];

        }
    }

    private String modeToString(int mode) {
        switch (mode) {
            case UiModeManager.MODE_NIGHT_YES:
                return "yes";
            case UiModeManager.MODE_NIGHT_NO:
            case UiModeManager.MODE_NIGHT_AUTO:
            default:
                return "no";

        }
    }

    private int modeToInt(String mode) {
        switch (mode) {
            case "yes":
                return UiModeManager.MODE_NIGHT_YES;
            case "no":
            case "auto":
            default:
                return UiModeManager.MODE_NIGHT_NO;
        }
    }
}
