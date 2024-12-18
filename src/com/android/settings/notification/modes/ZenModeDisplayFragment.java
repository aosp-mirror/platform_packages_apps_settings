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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page that shows what device effects/notification visuals will change when this mode
 * is on.
 */
public class ZenModeDisplayFragment extends ZenModeFragmentBase {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(new ZenModeNotifVisLinkPreferenceController(
                context, "notification_visibility", mHelperBackend));
        prefControllers.add(new ZenModeDisplayEffectPreferenceController(
                context, "effect_greyscale", mBackend));
        prefControllers.add(new ZenModeDisplayEffectPreferenceController(
                context, "effect_aod", mBackend));
        prefControllers.add(new ZenModeDisplayEffectPreferenceController(
                context, "effect_wallpaper", mBackend));
        prefControllers.add(new ZenModeDisplayEffectPreferenceController(
                context, "effect_dark_theme", mBackend));
        return prefControllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_display_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_MODE_DISPLAY_SETTINGS;
    }
}
