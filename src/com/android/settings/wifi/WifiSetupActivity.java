/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.settings.wifi;

import com.android.settings.ButtonBarHandler;

import android.content.res.Resources;

public class WifiSetupActivity extends WifiPickerActivity implements ButtonBarHandler {
    // Extra containing the resource name of the theme to be used
    private static final String EXTRA_THEME = "theme";
    private static final String THEME_HOLO = "holo";
    private static final String THEME_HOLO_LIGHT = "holo_light";

    // Style resources containing theme settings
    private static final String RESOURCE_THEME_DARK = "SetupWizardWifiTheme";
    private static final String RESOURCE_THEME_LIGHT = "SetupWizardWifiTheme.Light";

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        String themeName = getIntent().getStringExtra(EXTRA_THEME);
        if (themeName != null && themeName.equalsIgnoreCase(THEME_HOLO_LIGHT)) {
            resid = getResources().getIdentifier(RESOURCE_THEME_LIGHT, "style",
                    getPackageName());
        }
        super.onApplyThemeResource(theme, resid, first);
    }
}
