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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.PowerManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

// LINT.IfChange
public class DarkUIPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    public static final String PREF_DARK_MODE_DIALOG_SEEN = "dark_mode_dialog_seen";
    public static final int DIALOG_SEEN = 1;

    @VisibleForTesting
    Preference mPreference;

    private UiModeManager mUiModeManager;
    private PowerManager mPowerManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateEnabledStateIfNeeded();
        }
    };

    public DarkUIPreferenceController(Context context, String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    @Override
    public boolean isChecked() {
         return (mContext.getResources().getConfiguration().uiMode
                 & Configuration.UI_MODE_NIGHT_YES) != 0;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateEnabledStateIfNeeded();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mUiModeManager.setNightModeActivated(isChecked);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @VisibleForTesting
    void updateEnabledStateIfNeeded() {
        if (mPreference == null) {
            return;
        }
        boolean isBatterySaver = isPowerSaveMode();
        mPreference.setEnabled(!isBatterySaver);
        if (isBatterySaver) {
            int stringId = isChecked()
                    ? R.string.dark_ui_mode_disabled_summary_dark_theme_on
                    : R.string.dark_ui_mode_disabled_summary_dark_theme_off;
            mPreference.setSummary(mContext.getString(stringId));
        }
    }

    @VisibleForTesting
    boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
// LINT.ThenChange(darkmode/DarkModeScreen.kt)
