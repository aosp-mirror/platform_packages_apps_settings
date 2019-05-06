/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_FIRST_RUN;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;

import android.content.Intent;
import android.os.Bundle;
import android.sysprop.SetupWizardProperties;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.Arrays;


public class SetupWizardUtils {

    public static String getThemeString(Intent intent) {
        String theme = intent.getStringExtra(WizardManagerHelper.EXTRA_THEME);
        if (theme == null) {
            theme = SetupWizardProperties.theme().orElse("");
        }
        return theme;
    }

    public static int getTheme(Intent intent) {
        String theme = getThemeString(intent);
        // TODO(yukl): Move to ThemeResolver and add any additional required attributes in
        // onApplyThemeResource using Theme overlays
        if (theme != null) {
            if (WizardManagerHelper.isAnySetupWizard(intent)) {
                switch (theme) {
                    case ThemeHelper.THEME_GLIF_V3_LIGHT:
                        return R.style.GlifV3Theme_Light;
                    case ThemeHelper.THEME_GLIF_V3:
                        return R.style.GlifV3Theme;
                    case ThemeHelper.THEME_GLIF_V2_LIGHT:
                        return R.style.GlifV2Theme_Light;
                    case ThemeHelper.THEME_GLIF_V2:
                        return R.style.GlifV2Theme;
                    case ThemeHelper.THEME_GLIF_LIGHT:
                        return R.style.GlifTheme_Light;
                    case ThemeHelper.THEME_GLIF:
                        return R.style.GlifTheme;
                }
            } else {
                switch (theme) {
                    case ThemeHelper.THEME_GLIF_V3_LIGHT:
                    case ThemeHelper.THEME_GLIF_V3:
                        return R.style.GlifV3Theme;
                    case ThemeHelper.THEME_GLIF_V2_LIGHT:
                    case ThemeHelper.THEME_GLIF_V2:
                        return R.style.GlifV2Theme;
                    case ThemeHelper.THEME_GLIF_LIGHT:
                    case ThemeHelper.THEME_GLIF:
                        return R.style.GlifTheme;
                }
            }
        }
        return R.style.GlifTheme;
    }

    public static int getTransparentTheme(Intent intent) {
        final int suwTheme = getTheme(intent);
        int transparentTheme = R.style.GlifV2Theme_Light_Transparent;
        if (suwTheme == R.style.GlifV3Theme) {
            transparentTheme = R.style.GlifV3Theme_Transparent;
        } else if (suwTheme == R.style.GlifV3Theme_Light) {
            transparentTheme = R.style.GlifV3Theme_Light_Transparent;
        } else if (suwTheme == R.style.GlifV2Theme) {
            transparentTheme = R.style.GlifV2Theme_Transparent;
        } else if (suwTheme == R.style.GlifTheme_Light) {
            transparentTheme = R.style.SetupWizardTheme_Light_Transparent;
        } else if (suwTheme == R.style.GlifTheme) {
            transparentTheme = R.style.SetupWizardTheme_Transparent;
        }
        return transparentTheme;
    }

    public static void copySetupExtras(Intent fromIntent, Intent toIntent) {
        WizardManagerHelper.copyWizardManagerExtras(fromIntent, toIntent);
    }

    public static Bundle copyLifecycleExtra(Bundle srcBundle, Bundle dstBundle) {
        for (String key :
                Arrays.asList(
                        EXTRA_IS_FIRST_RUN,
                        EXTRA_IS_SETUP_FLOW)) {
            dstBundle.putBoolean(key, srcBundle.getBoolean(key, false));
        }
        return dstBundle;
    }
}
