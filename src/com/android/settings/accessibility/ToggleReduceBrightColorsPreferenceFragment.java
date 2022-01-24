/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings for reducing brightness. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleReduceBrightColorsPreferenceFragment extends ToggleFeaturePreferenceFragment {

    private static final String REDUCE_BRIGHT_COLORS_ACTIVATED_KEY =
            Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED;
    private static final String KEY_INTENSITY = "rbc_intensity";
    private static final String KEY_PERSIST = "rbc_persist";

    private ReduceBrightColorsIntensityPreferenceController mRbcIntensityPreferenceController;
    private ReduceBrightColorsPersistencePreferenceController mRbcPersistencePreferenceController;
    private ColorDisplayManager mColorDisplayManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getPrefContext().getPackageName())
                .appendPath(String.valueOf(R.raw.extra_dim_banner))
                .build();
        mComponentName = AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME;
        mPackageName = getText(R.string.reduce_bright_colors_preference_title);
        mHtmlDescription = getText(R.string.reduce_bright_colors_preference_subtitle);
        mRbcIntensityPreferenceController =
                new ReduceBrightColorsIntensityPreferenceController(getContext(), KEY_INTENSITY);
        mRbcPersistencePreferenceController =
                new ReduceBrightColorsPersistencePreferenceController(getContext(), KEY_PERSIST);
        mRbcIntensityPreferenceController.displayPreference(getPreferenceScreen());
        mRbcPersistencePreferenceController.displayPreference(getPreferenceScreen());
        mColorDisplayManager = getContext().getSystemService(ColorDisplayManager.class);
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        // Parent sets the title when creating the view, so set it after calling super
        mToggleServiceSwitchPreference.setTitle(R.string.reduce_bright_colors_switch_title);
        updateGeneralCategoryOrder();
        updateFooterPreference();
        return view;
    }

    @Override
    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        super.registerKeysToObserverCallback(contentObserver);

        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(REDUCE_BRIGHT_COLORS_ACTIVATED_KEY);
        contentObserver.registerKeysToObserverCallback(enableServiceFeatureKeys,
                key -> updateSwitchBarToggleSwitch());
    }

    private void updateGeneralCategoryOrder() {
        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        final SeekBarPreference intensity = findPreference(KEY_INTENSITY);
        getPreferenceScreen().removePreference(intensity);
        intensity.setOrder(mShortcutPreference.getOrder() - 2);
        generalCategory.addPreference(intensity);
        final SwitchPreference persist = findPreference(KEY_PERSIST);
        getPreferenceScreen().removePreference(persist);
        persist.setOrder(mShortcutPreference.getOrder() - 1);
        generalCategory.addPreference(persist);
    }

    private void updateFooterPreference() {
        final String title = getPrefContext().getString(R.string.reduce_bright_colors_about_title);
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.displayPreference(getPreferenceScreen());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS;
    }

    @Override
    public int getHelpResource() {
        // TODO(170973645): Link to help support page
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.reduce_bright_colors_settings;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(mComponentName, enabled);
        mColorDisplayManager.setReduceBrightColorsActivated(enabled);
    }

    @Override
    protected void onRemoveSwitchPreferenceToggleSwitch() {
        super.onRemoveSwitchPreferenceToggleSwitch();
        mToggleServiceSwitchPreference.setOnPreferenceClickListener(
                /* onPreferenceClickListener= */ null);
    }

    @Override
    protected void updateToggleServiceTitle(SettingsMainSwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.reduce_bright_colors_preference_title);
    }

    @Override
    protected void updateShortcutTitle(ShortcutPreference shortcutPreference) {
        shortcutPreference.setTitle(R.string.reduce_bright_colors_shortcut_title);
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    @Override
    ComponentName getTileComponentName() {
        return null;
    }

    @Override
    CharSequence getTileName() {
        return null;
    }

    @Override
    protected void updateSwitchBarToggleSwitch() {
        final boolean checked = mColorDisplayManager.isReduceBrightColorsActivated();
        mRbcIntensityPreferenceController.updateState(getPreferenceScreen()
                .findPreference(KEY_INTENSITY));
        mRbcPersistencePreferenceController.updateState(getPreferenceScreen()
                .findPreference(KEY_PERSIST));
        if (mToggleServiceSwitchPreference.isChecked() != checked) {
            mToggleServiceSwitchPreference.setChecked(checked);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.reduce_bright_colors_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return ColorDisplayManager.isReduceBrightColorsAvailable(context);
                }
            };
}
