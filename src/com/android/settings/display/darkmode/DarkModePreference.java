/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display.darkmode;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settings.widget.MasterSwitchPreference;

import java.time.LocalTime;

/**
 * component for the display settings dark ui summary*/
public class DarkModePreference extends MasterSwitchPreference {

    private UiModeManager mUiModeManager;
    private DarkModeObserver mDarkModeObserver;
    private PowerManager mPowerManager;
    private Runnable mCallback;

    private TimeFormatter mFormat;

    public DarkModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDarkModeObserver = new DarkModeObserver(context);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
        mFormat = new TimeFormatter(context);
        mCallback = () -> {
            final boolean batterySaver = mPowerManager.isPowerSaveMode();
            final boolean active = (getContext().getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_YES) != 0;
            setSwitchEnabled(!batterySaver);
            updateSummary(batterySaver, active);
        };
        mDarkModeObserver.subscribe(mCallback);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mDarkModeObserver.subscribe(mCallback);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mDarkModeObserver.unsubscribe();
    }

    private void updateSummary(boolean batterySaver, boolean active) {
        if (batterySaver) {
            final int stringId = active
                    ? R.string.dark_ui_mode_disabled_summary_dark_theme_on
                    : R.string.dark_ui_mode_disabled_summary_dark_theme_off;
            setSummary(getContext().getString(stringId));
            return;
        }
        final int mode = mUiModeManager.getNightMode();
        String detail;

        if (mode == UiModeManager.MODE_NIGHT_AUTO) {
            detail = getContext().getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_auto
                    : R.string.dark_ui_summary_off_auto_mode_auto);
        } else if (mode == UiModeManager.MODE_NIGHT_CUSTOM) {
            final LocalTime time = active
                    ? mUiModeManager.getCustomNightModeEnd()
                    : mUiModeManager.getCustomNightModeStart();
            final String timeStr = mFormat.of(time);
            detail = getContext().getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_custom
                    : R.string.dark_ui_summary_off_auto_mode_custom, timeStr);
        } else {
            detail = getContext().getString(active
                    ? R.string.dark_ui_summary_on_auto_mode_never
                    : R.string.dark_ui_summary_off_auto_mode_never);
        }
        String summary = getContext().getString(active
                ? R.string.dark_ui_summary_on
                : R.string.dark_ui_summary_off, detail);

        setSummary(summary);
    }
}
