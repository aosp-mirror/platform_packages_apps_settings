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
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(new ZenModeHeaderController(context, "header", this, mBackend));
        prefControllers.add(new ZenModeButtonPreferenceController(context, "activate", mBackend));
        prefControllers.add(new ZenModePeopleLinkPreferenceController(
                context, "zen_mode_people", mBackend));
        prefControllers.add(new ZenModeAppsLinkPreferenceController(
                context, "zen_mode_apps", mBackend));
        prefControllers.add(new ZenModeOtherLinkPreferenceController(
                context, "zen_other_settings", mBackend));
        prefControllers.add(new ZenModeDisplayLinkPreferenceController(
                context, "mode_display_settings", mBackend));
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
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }
}
