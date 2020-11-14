/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Preference controller for emergency gesture setting
 */
public class EmergencyGesturePreferenceController extends BasePreferenceController implements
        SwitchBar.OnSwitchChangeListener {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    private static final String SECURE_KEY = Settings.Secure.EMERGENCY_GESTURE_ENABLED;

    private SwitchBar mSwitchBar;

    public EmergencyGesturePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isConfigEnabled = mContext.getResources()
                .getBoolean(R.bool.config_show_emergency_gesture_settings);

        if (!isConfigEnabled) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference pref = screen.findPreference(mPreferenceKey);
        mSwitchBar = pref.findViewById(R.id.switch_bar);
        mSwitchBar.setSwitchBarText(R.string.emergency_gesture_screen_title,
                R.string.emergency_gesture_screen_title);
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.setChecked(isChecked());
        mSwitchBar.show();
    }

    @VisibleForTesting
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SECURE_KEY, OFF) == ON;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY, isChecked ? ON : OFF);
    }
}
