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
 * limitations under the License
 */

package com.android.settings.display;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SettingsMainSwitchPreference;

/**
 * Controller that updates the adaptive brightness.
 */
public class AutoBrightnessDetailPreferenceController extends
        AutoBrightnessPreferenceController implements OnCheckedChangeListener {

    public AutoBrightnessDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        SettingsMainSwitchPreference pref = (SettingsMainSwitchPreference) screen.findPreference(
                getPreferenceKey());
        pref.addOnSwitchChangeListener(this);
        pref.setChecked(isChecked());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }
}
