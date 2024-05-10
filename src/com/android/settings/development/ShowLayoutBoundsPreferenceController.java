/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.sysprop.DisplayProperties;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class ShowLayoutBoundsPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String DEBUG_LAYOUT_KEY = "debug_layout";

    public ShowLayoutBoundsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return DEBUG_LAYOUT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        DisplayProperties.debug_layout(isEnabled);
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = DisplayProperties.debug_layout().orElse(false);
        ((TwoStatePreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        DisplayProperties.debug_layout(false);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
