/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.app.UiModeManager.MODE_ATTENTION_THEME_OVERLAY_NIGHT;
import static android.app.UiModeManager.MODE_NIGHT_AUTO;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Flags;
import android.app.UiModeManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import java.time.LocalTime;
import java.util.List;

class AutoDarkTheme {

    static String getStatus(Context context, boolean active) {
        UiModeManager uiModeManager = checkNotNull(context.getSystemService(UiModeManager.class));
        final int mode = uiModeManager.getNightMode();

        if (mode == MODE_NIGHT_AUTO) {
            return context.getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_auto
                    : R.string.dark_ui_summary_off_auto_mode_auto);
        }

        if (Flags.modesUi()) {
            if (active && uiModeManager.getAttentionModeThemeOverlay()
                    == MODE_ATTENTION_THEME_OVERLAY_NIGHT) {
                List<String> modes = getActiveModesThatChangeDarkTheme(context);
                if (!modes.isEmpty()) {
                    return context.getString(R.string.dark_ui_summary_on_auto_mode_modes,
                            modes.get(0));
                }
            } else if (!active) {
                List<String> modes = getModesThatChangeDarkTheme(context);
                if (!modes.isEmpty()) {
                    return context.getString(R.string.dark_ui_summary_off_auto_mode_modes,
                            modes.get(0));
                }
            }
        }

        if (mode == UiModeManager.MODE_NIGHT_CUSTOM) {
            int modeCustomType = uiModeManager.getNightModeCustomType();
            if (!Flags.modesUi() && modeCustomType == MODE_NIGHT_CUSTOM_TYPE_BEDTIME) {
                return context.getString(active
                        ? R.string.dark_ui_summary_on_auto_mode_custom_bedtime
                        : R.string.dark_ui_summary_off_auto_mode_custom_bedtime);
            }
            if (modeCustomType == MODE_NIGHT_CUSTOM_TYPE_SCHEDULE) {
                final LocalTime time = active
                        ? uiModeManager.getCustomNightModeEnd()
                        : uiModeManager.getCustomNightModeStart();
                final String timeStr = new TimeFormatter(context).of(time);
                return context.getString(active
                        ? R.string.dark_ui_summary_on_auto_mode_custom
                        : R.string.dark_ui_summary_off_auto_mode_custom, timeStr);
            }
        }

        return context.getString(active
                ? R.string.dark_ui_summary_on_auto_mode_never
                : R.string.dark_ui_summary_off_auto_mode_never);
    }

    static List<String> getModesThatChangeDarkTheme(Context context) {
        return ZenModesBackend.getInstance(context)
                .getModes().stream()
                .filter(m -> m.getDeviceEffects().shouldUseNightMode())
                .map(ZenMode::getName)
                .toList();
    }

    static List<String> getActiveModesThatChangeDarkTheme(Context context) {
        return ZenModesBackend.getInstance(context)
                .getModes().stream()
                .filter(ZenMode::isActive)
                .filter(m -> m.getDeviceEffects().shouldUseNightMode())
                .map(ZenMode::getName)
                .toList();
    }
}
