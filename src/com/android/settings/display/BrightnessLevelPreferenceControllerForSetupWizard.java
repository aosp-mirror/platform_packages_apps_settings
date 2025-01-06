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

package com.android.settings.display;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.accessibility.Flags;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.RestrictedPreferenceHelperProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * The top-level preference controller that updates the adaptive brightness level in the
 * SetupWizard.
 */
public class BrightnessLevelPreferenceControllerForSetupWizard extends
        BrightnessLevelPreferenceController {

    private RestrictedPreferenceHelper mRestrictedPreferenceHelper;

    public BrightnessLevelPreferenceControllerForSetupWizard(@NonNull Context context,
            @Nullable Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    private boolean isRestricted() {
        if (mRestrictedPreferenceHelper == null) {
            return false;
        }
        return mRestrictedPreferenceHelper.isDisabledByAdmin()
                || mRestrictedPreferenceHelper.isDisabledByEcm();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof RestrictedPreferenceHelperProvider helperProvider) {
            mRestrictedPreferenceHelper = helperProvider.getRestrictedPreferenceHelper();
            preference.setVisible(!isRestricted());
        }
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if (!Flags.addBrightnessSettingsInSuw() || isRestricted()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return super.getAvailabilityStatus();
    }
}
