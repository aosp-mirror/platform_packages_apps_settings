/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.core;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SliceData;
import com.android.settings.onboarding.OnboardingFeatureProvider;
import com.android.settings.widget.TwoStateButtonPreference;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.instrumentation.SettingsJankMonitor;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Abstract class that consolidates logic for updating toggle controllers.
 * It automatically handles the getting and setting of the switch UI element.
 * Children of this class implement methods to get and set the underlying value of the setting.
 */
public abstract class TogglePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TogglePrefController";

    public TogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @return {@code true} if the Setting is enabled.
     */
    public abstract boolean isChecked();

    /**
     * Set the Setting to {@param isChecked}
     *
     * @param isChecked Is {@code true} when the setting should be enabled.
     * @return {@code true} if the underlying setting is updated.
     */
    public abstract boolean setChecked(boolean isChecked);

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof MainSwitchPreference) {
            ((MainSwitchPreference) preference).addOnSwitchChangeListener((switchView, isChecked) ->
                    SettingsJankMonitor.detectToggleJank(getPreferenceKey(), switchView));
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof TwoStatePreference) {
            ((TwoStatePreference) preference).setChecked(isChecked());
        } else if (preference instanceof PrimarySwitchPreference) {
            ((PrimarySwitchPreference) preference).setChecked(isChecked());
        } else if (preference instanceof TwoStateButtonPreference) {
            ((TwoStateButtonPreference) preference).setChecked(isChecked());
        } else {
            refreshSummary(preference);
        }
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        // TwoStatePreference is a regular preference and can be handled by DashboardFragment
        if (preference instanceof PrimarySwitchPreference
                || preference instanceof TwoStateButtonPreference) {
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .logClickedPreference(preference, getMetricsCategory());
        }
        OnboardingFeatureProvider onboardingFeatureProvider =
                FeatureFactory.getFeatureFactory().getOnboardingFeatureProvider();
        if (onboardingFeatureProvider != null) {
            onboardingFeatureProvider.markPreferenceHasChanged(mContext, mPreferenceKey);
        }
        return setChecked((boolean) newValue);
    }

    @Override
    @SliceData.SliceType
    public int getSliceType() {
        return SliceData.SliceType.SWITCH;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public boolean isPublicSlice() {
        return false;
    }

    @Override
    public abstract int getSliceHighlightMenuRes();
}