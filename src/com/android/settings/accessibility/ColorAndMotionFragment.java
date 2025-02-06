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
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Accessibility settings for color and motion. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ColorAndMotionFragment extends DashboardFragment {

    private static final String TAG = "ColorAndMotionFragment";

    private static final String CATEGORY_EXPERIMENTAL = "experimental_category";

    // Preferences
    private static final String DISPLAY_DALTONIZER_PREFERENCE_SCREEN = "daltonizer_preference";

    private Preference mDisplayDaltonizerPreferenceScreen;
    private TwoStatePreference mToggleDisableAnimationsPreference;
    private AccessibilitySettingsContentObserver mSettingsContentObserver;

    private final List<String> mShortcutFeatureKeys = new ArrayList<>();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initializeAllPreferences();
        updateSystemPreferences();

        mShortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED);
        mShortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
        mShortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        mShortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        mShortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_QS_TARGETS);

        mSettingsContentObserver = new AccessibilitySettingsContentObserver(new Handler());
        mSettingsContentObserver.registerKeysToObserverCallback(mShortcutFeatureKeys,
                key -> updatePreferencesState());
    }

    private void updatePreferencesState() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        controllers.forEach(controller -> controller.updateState(
                findPreference(controller.getPreferenceKey())));
    }

    @Override
    public void onStart() {
        super.onStart();

        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onStop() {
        super.onStop();

        mSettingsContentObserver.unregister(getContentResolver());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_color_and_motion;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private void initializeAllPreferences() {
        // Display color adjustments.
        mDisplayDaltonizerPreferenceScreen = findPreference(DISPLAY_DALTONIZER_PREFERENCE_SCREEN);

        // Disable animation.
        mToggleDisableAnimationsPreference = findPreference(RemoveAnimationsPreference.KEY);
    }

    /**
     * Updates preferences related to system configurations.
     */
    private void updateSystemPreferences() {
        final PreferenceCategory experimentalCategory = getPreferenceScreen().findPreference(
                CATEGORY_EXPERIMENTAL);
        if (ColorDisplayManager.isColorTransformAccelerated(getContext())) {
            getPreferenceScreen().removePreference(experimentalCategory);
        } else {
            // Move following preferences to experimental category if device don't supports HWC
            // hardware-accelerated color transform.
            getPreferenceScreen().removePreference(mDisplayDaltonizerPreferenceScreen);
            getPreferenceScreen().removePreference(mToggleDisableAnimationsPreference);
            experimentalCategory.addPreference(mDisplayDaltonizerPreferenceScreen);
            experimentalCategory.addPreference(mToggleDisableAnimationsPreference);
        }
    }

    @Nullable
    @Override
    public String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ColorAndMotionScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_color_and_motion);
}
