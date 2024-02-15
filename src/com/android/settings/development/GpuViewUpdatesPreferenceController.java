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
import android.os.SystemProperties;
import android.view.ThreadedRenderer;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class GpuViewUpdatesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String SHOW_HW_SCREEN_UPDATES_KEY = "show_hw_screen_updates";

    public GpuViewUpdatesPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return SHOW_HW_SCREEN_UPDATES_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                isEnabled ? "true" : null);
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = SystemProperties.getBoolean(
                ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY, false /* default */);
        ((TwoStatePreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY, null);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
