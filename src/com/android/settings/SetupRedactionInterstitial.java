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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.settings.notification.RedactionInterstitial;

/**
 * Setup Wizard's version of RedactionInterstitial screen. It inherits the logic and basic structure
 * from RedactionInterstitial class, and should remain similar to that behaviorally. This class
 * should only overload base methods for minor theme and behavior differences specific to Setup
 * Wizard. Other changes should be done to RedactionInterstitial class instead and let this class
 * inherit those changes.
 */
public class SetupRedactionInterstitial extends RedactionInterstitial {

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

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    public static class SetupRedactionInterstitialFragment extends RedactionInterstitialFragment {

        // Setup wizard specific UI customizations can be done here
    }
}
