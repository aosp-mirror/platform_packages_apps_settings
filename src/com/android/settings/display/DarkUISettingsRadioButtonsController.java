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

import android.app.UiModeManager;
import android.content.Context;
import androidx.preference.Preference;
import com.android.settings.R;
import androidx.annotation.VisibleForTesting;

public class DarkUISettingsRadioButtonsController {

    public static final String KEY_DARK = "key_dark_ui_settings_dark";
    public static final String KEY_LIGHT = "key_dark_ui_settings_light";

    @VisibleForTesting
    UiModeManager mManager;

    private Preference mFooter;

    public DarkUISettingsRadioButtonsController(Context context, Preference footer) {
        mManager = context.getSystemService(UiModeManager.class);
        mFooter = footer;
    }

    public String getDefaultKey() {
        final int mode = mManager.getNightMode();
        updateFooter();
        return mode == UiModeManager.MODE_NIGHT_YES ? KEY_DARK : KEY_LIGHT;
    }

    public boolean setDefaultKey(String key) {
        switch(key) {
            case KEY_DARK:
                mManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                break;
            case KEY_LIGHT:
                mManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
                break;
            default:
                throw new IllegalStateException(
                        "Not a valid key for " + this.getClass().getSimpleName() + ": " + key);
        }
        updateFooter();
        return true;
    }

    public void updateFooter() {
        final int mode = mManager.getNightMode();
        switch (mode) {
            case UiModeManager.MODE_NIGHT_YES:
                mFooter.setSummary(R.string.dark_ui_settings_dark_summary);
                break;
            case UiModeManager.MODE_NIGHT_NO:
            case UiModeManager.MODE_NIGHT_AUTO:
            default:
                mFooter.setSummary(R.string.dark_ui_settings_light_summary);
        }
    }

    public static String modeToDescription(Context context, int mode) {
        final String[] values = context.getResources().getStringArray(R.array.dark_ui_mode_entries);
        switch (mode) {
            case UiModeManager.MODE_NIGHT_YES:
                return values[0];
            case UiModeManager.MODE_NIGHT_NO:
            case UiModeManager.MODE_NIGHT_AUTO:
            default:
                return values[1];
        }
    }
}
