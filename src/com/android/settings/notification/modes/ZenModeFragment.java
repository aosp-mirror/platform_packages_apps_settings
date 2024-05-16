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

package com.android.settings.notification.modes;

import android.app.AutomaticZenRule;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ZenModeFragment extends ZenModeFragmentBase {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_rule_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        // TODO: fill in with all the elements of this page. Each should be an instance of
        //       {@link AbstractZenModePreferenceController}.
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        return prefControllers;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set title for the entire screen
        ZenMode mode = getMode();
        AutomaticZenRule azr = getAZR();
        if (mode == null || azr == null) {
            return;
        }
        getActivity().setTitle(azr.getName());

        // TODO: b/308819292 - implement the real screen!
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }

        Preference tmpPref = screen.findPreference("zen_mode_test");
        if (tmpPref == null) {
            return;
        }
        tmpPref.setTitle(azr.getTriggerDescription());
        tmpPref.setSummary("active?: " + mode.isActive());
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }
}
