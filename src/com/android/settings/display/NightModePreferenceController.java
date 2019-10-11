/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
import android.content.Context;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class NightModePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "NightModePrefContr";
    private static final String KEY_NIGHT_MODE = "night_mode";

    public NightModePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NIGHT_MODE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (!isAvailable()) {
            setVisible(screen, KEY_NIGHT_MODE, false /* visible */);
            return;
        }
        final ListPreference mNightModePreference = screen.findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager =
                    (UiModeManager) mContext.getSystemService(UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            final int value = Integer.parseInt((String) newValue);
            final UiModeManager uiManager =
                    (UiModeManager) mContext.getSystemService(UI_MODE_SERVICE);
            uiManager.setNightMode(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist night mode setting", e);
            return false;
        }
        return true;
    }
}
