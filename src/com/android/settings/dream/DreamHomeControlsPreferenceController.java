/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.dream.DreamBackend;

/**
 * Controller for the {@link androidx.preference.SwitchPreference} which controls if dream
 * overlays should be enabled.
 */
public class DreamHomeControlsPreferenceController extends TogglePreferenceController {
    private final DreamBackend mBackend;

    public static final String PREF_KEY = "dream_home_controls_toggle";

    public DreamHomeControlsPreferenceController(Context context,
            DreamBackend dreamBackend) {
        super(context, PREF_KEY);
        mBackend = dreamBackend;
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean supported =
                mBackend.getSupportedComplications()
                        .contains(DreamBackend.COMPLICATION_TYPE_HOME_CONTROLS);

        return controlsEnabledOnLockscreen() ? (supported ? AVAILABLE : CONDITIONALLY_UNAVAILABLE)
                : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public boolean isChecked() {
        return controlsEnabledOnLockscreen() && mBackend.getEnabledComplications().contains(
                DreamBackend.COMPLICATION_TYPE_HOME_CONTROLS);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setHomeControlsEnabled(isChecked);
        return true;
    }

    private boolean controlsEnabledOnLockscreen() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCKSCREEN_SHOW_CONTROLS, 0) != 0;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
