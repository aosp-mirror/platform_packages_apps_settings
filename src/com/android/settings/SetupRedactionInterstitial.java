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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.settings.notification.RedactionInterstitial;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Setup Wizard's version of RedactionInterstitial screen. It inherits the logic and basic structure
 * from RedactionInterstitial class, and should remain similar to that behaviorally. This class
 * should only overload base methods for minor theme and behavior differences specific to Setup
 * Wizard. Other changes should be done to RedactionInterstitial class instead and let this class
 * inherit those changes.
 */
public class SetupRedactionInterstitial extends RedactionInterstitial {

    /**
     * Set the enabled state of SetupRedactionInterstitial activity to configure whether it is shown
     * as part of setup wizard's optional steps.
     */
    public static void setEnabled(Context context, boolean enabled) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, SetupRedactionInterstitial.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        // Only allow to start the activity from Setup Wizard.
        if (!WizardManagerHelper.isAnySetupWizard(getIntent())) {
            finish();
        }
        super.onCreate(savedInstance);
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT,
                SetupRedactionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupRedactionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static class SetupRedactionInterstitialFragment extends RedactionInterstitialFragment {

        // Setup wizard specific UI customizations can be done here
    }
}
