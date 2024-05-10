/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Preference controller for MainSwitchPreference.
 */
public abstract class SettingsMainSwitchPreferenceController extends
        TogglePreferenceController implements OnCheckedChangeListener {

    protected MainSwitchPreference mSwitchPreference;

    public SettingsMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null && pref instanceof MainSwitchPreference) {
            mSwitchPreference = (MainSwitchPreference) pref;
            mSwitchPreference.addOnSwitchChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSwitchPreference.setChecked(isChecked);
        setChecked(isChecked);
    }
}
