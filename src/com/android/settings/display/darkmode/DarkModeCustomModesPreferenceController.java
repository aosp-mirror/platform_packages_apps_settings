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
import static android.app.settings.SettingsEnums.DARK_UI_SETTINGS;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Flags;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.modes.ZenModesListFragment;
import com.android.settingslib.widget.FooterPreference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Controller for the dark theme Modes / Bedtime custom footer. */
public class DarkModeCustomModesPreferenceController extends BasePreferenceController {
    private final UiModeManager mUiModeManager;
    private final BedtimeSettings mBedtimeSettings;

    public DarkModeCustomModesPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mBedtimeSettings = new BedtimeSettings(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return (Flags.modesUi() || mBedtimeSettings.getBedtimeSettingsIntent() != null)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference footerPreference = checkNotNull(screen.findPreference(getPreferenceKey()));
        if (Flags.modesUi()) {
            List<String> modesUsingDarkTheme = AutoDarkTheme.getModesThatChangeDarkTheme(
                    screen.getContext());

            MessageFormat titleFormat = new MessageFormat(
                    mContext.getString(R.string.dark_ui_modes_footer_summary),
                    Locale.getDefault());
            Map<String, Object> args = new HashMap<>();
            args.put("count", modesUsingDarkTheme.size());
            for (int i = 0; i < modesUsingDarkTheme.size() && i < 3; i++) {
                args.put("mode_" + (i + 1), modesUsingDarkTheme.get(i));
            }
            footerPreference.setTitle(titleFormat.format(args));

            footerPreference.setLearnMoreAction(
                    v -> new SubSettingLauncher(v.getContext())
                            .setDestination(ZenModesListFragment.class.getName())
                            .setSourceMetricsCategory(DARK_UI_SETTINGS)
                            .launch());
            footerPreference.setLearnMoreText(
                    mContext.getString(R.string.dark_ui_modes_footer_action));
        } else {
            footerPreference.setTitle(R.string.dark_ui_bedtime_footer_summary);
            footerPreference.setLearnMoreAction(
                    v -> {
                        Intent bedtimeSettingsIntent = mBedtimeSettings.getBedtimeSettingsIntent();
                        if (bedtimeSettingsIntent != null) {
                            v.getContext().startActivity(bedtimeSettingsIntent);
                        }
                    });
            footerPreference.setLearnMoreText(
                    mContext.getString(R.string.dark_ui_bedtime_footer_action));
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        if (!Flags.modesUi()) {
            if (mUiModeManager.getNightModeCustomType() != MODE_NIGHT_CUSTOM_TYPE_BEDTIME) {
                preference.setVisible(false);
                return;
            }
            preference.setVisible(true);
        }
    }
}
