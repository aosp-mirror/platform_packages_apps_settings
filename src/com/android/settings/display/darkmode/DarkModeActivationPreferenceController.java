/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.MainSwitchPreference;

import java.time.LocalTime;

/**
 * Controller for activate/deactivate night mode button
 */
public class DarkModeActivationPreferenceController extends BasePreferenceController implements
        OnCheckedChangeListener {

    private final UiModeManager mUiModeManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private TimeFormatter mFormat;
    private MainSwitchPreference mPreference;

    public DarkModeActivationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mFormat = new TimeFormatter(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    public DarkModeActivationPreferenceController(Context context, String preferenceKey,
            TimeFormatter f) {
        this(context, preferenceKey);
        mFormat = f;
    }

    @Override
    public final void updateState(Preference preference) {
        final boolean active = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        mPreference.updateStatus(active);
    }

    @Override
    public CharSequence getSummary() {
        final boolean isActivated = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        final int mode = mUiModeManager.getNightMode();
        if (mode == UiModeManager.MODE_NIGHT_AUTO) {
            return mContext.getString(isActivated
                    ? R.string.dark_ui_summary_on_auto_mode_auto
                    : R.string.dark_ui_summary_off_auto_mode_auto);
        } else if (mode == UiModeManager.MODE_NIGHT_CUSTOM) {
            if (mUiModeManager.getNightModeCustomType()
                    == UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME) {
                return mContext.getString(isActivated
                        ? R.string.dark_ui_summary_on_auto_mode_custom_bedtime
                        : R.string.dark_ui_summary_off_auto_mode_custom_bedtime);
            }

            final LocalTime time = isActivated
                    ? mUiModeManager.getCustomNightModeEnd()
                    : mUiModeManager.getCustomNightModeStart();
            final String timeStr = mFormat.of(time);

            return mContext.getString(isActivated
                    ? R.string.dark_ui_summary_on_auto_mode_custom
                    : R.string.dark_ui_summary_off_auto_mode_custom, timeStr);
        } else {
            return mContext.getString(isActivated
                    ? R.string.dark_ui_summary_on_auto_mode_never
                    : R.string.dark_ui_summary_off_auto_mode_never);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mMetricsFeatureProvider.logClickedPreference(mPreference, getMetricsCategory());
        final boolean active = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        mUiModeManager.setNightModeActivated(!active);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }
}
