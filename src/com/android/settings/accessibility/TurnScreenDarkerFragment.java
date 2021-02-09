/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.DarkUIPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Accessibility settings for turning screen darker. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class TurnScreenDarkerFragment extends DashboardFragment {

    private static final String TAG = "TurnDarkerFragment";

    private static final String CATEGORY_EXPERIMENTAL = "experimental_category";

    // Preferences
    private static final String TOGGLE_INVERSION_PREFERENCE = "toggle_inversion_preference";
    private static final String DISPLAY_REDUCE_BRIGHT_COLORS_PREFERENCE_SCREEN =
            "reduce_bright_colors_preference";

    private Preference mToggleInversionPreference;
    private Preference mReduceBrightColorsPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TURN_SCREEN_DARKER;
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initializeAllPreferences();
        updateSystemPreferences();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DarkUIPreferenceController.class).setParentFragment(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_turn_screen_darker;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private void initializeAllPreferences() {
        // Display inversion.
        mToggleInversionPreference = findPreference(TOGGLE_INVERSION_PREFERENCE);

        // Reduce brightness.
        mReduceBrightColorsPreference =
                findPreference(DISPLAY_REDUCE_BRIGHT_COLORS_PREFERENCE_SCREEN);
    }

    /**
     * Updates preferences related to system configurations.
     */
    private void updateSystemPreferences() {
        final PreferenceCategory experimentalCategory = getPreferenceScreen().findPreference(
                CATEGORY_EXPERIMENTAL);
        if (ColorDisplayManager.isColorTransformAccelerated(getContext())) {
            mToggleInversionPreference.setSummary(AccessibilityUtil.getSummary(
                    getContext(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED));
            mReduceBrightColorsPreference.setSummary(AccessibilityUtil.getSummary(
                    getContext(), Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED));
            getPreferenceScreen().removePreference(experimentalCategory);
        } else {
            // Move following preferences to experimental category if device don't supports HWC
            // hardware-accelerated color transform.
            getPreferenceScreen().removePreference(mToggleInversionPreference);
            getPreferenceScreen().removePreference(mReduceBrightColorsPreference);
            experimentalCategory.addPreference(mToggleInversionPreference);
            experimentalCategory.addPreference(mReduceBrightColorsPreference);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_turn_screen_darker);
}
