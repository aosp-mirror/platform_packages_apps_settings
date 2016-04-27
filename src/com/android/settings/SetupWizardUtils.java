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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.util.WizardManagerHelper;

public class SetupWizardUtils {

    public static int getTheme(Intent intent) {
        if (WizardManagerHelper.isLightTheme(intent, true)) {
            return R.style.SetupWizardTheme_Light;
        } else {
            return R.style.SetupWizardTheme;
        }
    }

    public static int getTransparentTheme(Intent intent) {
        if (WizardManagerHelper.isLightTheme(intent, true)) {
            return R.style.SetupWizardTheme_Light_Transparent;
        } else {
            return R.style.SetupWizardTheme_Transparent;
        }
    }

    /**
     * Sets the immersive mode related flags based on the extra in the intent which started the
     * activity.
     */
    public static void setImmersiveMode(Activity activity) {
        final boolean useImmersiveMode = activity.getIntent().getBooleanExtra(
                WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, false);
        if (useImmersiveMode) {
            SystemBarHelper.hideSystemBars(activity.getWindow());
        }
    }

    public static void applyImmersiveFlags(final Dialog dialog) {
        SystemBarHelper.hideSystemBars(dialog);
    }

    public static void copySetupExtras(Intent fromIntent, Intent toIntent) {
        toIntent.putExtra(WizardManagerHelper.EXTRA_THEME,
                fromIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME));
        toIntent.putExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE,
                fromIntent.getBooleanExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, false));
    }
}
