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

package com.android.settings.display.darkmode;

import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.FooterPreference;

/** Controller for the night mode bedtime custom mode footer. */
public class DarkModeCustomBedtimePreferenceController extends BasePreferenceController {
    private final UiModeManager mUiModeManager;
    private FooterPreference mFooterPreference;
    private BedtimeSettings mBedtimeSettings;

    public DarkModeCustomBedtimePreferenceController(Context context, String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mBedtimeSettings = new BedtimeSettings(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mBedtimeSettings.getBedtimeSettingsIntent() == null
                ? UNSUPPORTED_ON_DEVICE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mFooterPreference = screen.findPreference(getPreferenceKey());
        mFooterPreference.setLearnMoreAction(
                v -> {
                    Intent bedtimeSettingsIntent = mBedtimeSettings.getBedtimeSettingsIntent();
                    if (bedtimeSettingsIntent != null) {
                        v.getContext().startActivity(bedtimeSettingsIntent);
                    }
                });
        mFooterPreference.setLearnMoreText(
                mContext.getString(R.string.dark_ui_bedtime_footer_action));
    }

    @Override
    public void updateState(Preference preference) {
        if (mUiModeManager.getNightModeCustomType() != MODE_NIGHT_CUSTOM_TYPE_BEDTIME) {
            preference.setVisible(false);
            return;
        }
        preference.setVisible(true);
    }
}
