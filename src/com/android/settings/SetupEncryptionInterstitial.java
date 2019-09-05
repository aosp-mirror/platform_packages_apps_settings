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

import android.content.Context;
import android.content.Intent;

/**
 * Setup Wizard's version of EncryptionInterstitial screen. It inherits the logic and basic
 * structure from EncryptionInterstitial class, and should remain similar to that behaviorally. This
 * class should only overload base methods for minor theme and behavior differences specific to
 * Setup Wizard. Other changes should be done to EncryptionInterstitial class instead and let this
 * class inherit those changes.
 */
public class SetupEncryptionInterstitial extends EncryptionInterstitial {

    public static Intent createStartIntent(Context ctx, int quality,
            boolean requirePasswordDefault, Intent unlockMethodIntent) {
        Intent startIntent = EncryptionInterstitial.createStartIntent(ctx, quality,
                requirePasswordDefault, unlockMethodIntent);
        startIntent.setClass(ctx, SetupEncryptionInterstitial.class);
        startIntent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        return startIntent;
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT,
                SetupEncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitialFragment {
    }
}
