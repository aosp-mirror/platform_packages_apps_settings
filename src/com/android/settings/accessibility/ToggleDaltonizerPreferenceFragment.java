/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_TILE_COMPONENT_NAME;
import static com.android.settings.accessibility.AccessibilityStatsLogUtils.logAccessibilityServiceEnabled;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings for daltonizer. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerRadioButtonPreferenceController.OnChangeListener {

    private static final String TAG = "ToggleDaltonizerPreferenceFragment";
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String KEY_PREVIEW = "daltonizer_preview";
    @VisibleForTesting
    static final String KEY_DEUTERANOMALY = "daltonizer_mode_deuteranomaly";
    @VisibleForTesting
    static final String KEY_PROTANOMALY = "daltonizer_mode_protanomaly";
    @VisibleForTesting
    static final String KEY_TRITANOMEALY = "daltonizer_mode_tritanomaly";
    @VisibleForTesting
    static final String KEY_GRAYSCALE = "daltonizer_mode_grayscale";
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        if (sControllers.size() == 0) {
            final Resources resources = context.getResources();
            final String[] daltonizerKeys = resources.getStringArray(
                    R.array.daltonizer_mode_keys);

            for (String daltonizerKey : daltonizerKeys) {
                sControllers.add(new DaltonizerRadioButtonPreferenceController(
                        context, lifecycle, daltonizerKey));
            }
        }
        return sControllers;
    }


    @Override
    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        super.registerKeysToObserverCallback(contentObserver);

        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(ENABLED);
        contentObserver.registerKeysToObserverCallback(enableServiceFeatureKeys,
                key -> updateSwitchBarToggleSwitch());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mComponentName = DALTONIZER_COMPONENT_NAME;
        mPackageName = getText(com.android.settingslib.R
                .string.accessibility_display_daltonizer_preference_title);
        mHtmlDescription = getText(com.android.settingslib.R
                .string.accessibility_display_daltonizer_preference_subtitle);
        mTopIntroTitle = getText(R.string.accessibility_daltonizer_about_intro_text);
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        updateFooterPreference();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View rootView = getActivity().getWindow().peekDecorView();
        if (rootView != null) {
            rootView.setAccessibilityPaneTitle(getString(com.android.settingslib.R
                    .string.accessibility_display_daltonizer_preference_title));
        }
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    private void updateFooterPreference() {
        final String title = getPrefContext()
                .getString(R.string.accessibility_daltonizer_about_title);
        final String learnMoreText = getPrefContext()
                .getString(R.string.accessibility_daltonizer_footer_learn_more_content_description);
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.setupHelpLink(getHelpResource(), learnMoreText);
        mFooterPreferenceController.displayPreference(getPreferenceScreen());
    }

    /** Customizes the order by preference key. */
    protected List<String> getPreferenceOrderList() {
        final List<String> lists = new ArrayList<>();
        lists.add(KEY_TOP_INTRO_PREFERENCE);
        lists.add(KEY_PREVIEW);
        lists.add(KEY_USE_SERVICE_PREFERENCE);
        lists.add(KEY_DEUTERANOMALY);
        lists.add(KEY_PROTANOMALY);
        lists.add(KEY_TRITANOMEALY);
        lists.add(KEY_GRAYSCALE);
        lists.add(KEY_GENERAL_CATEGORY);
        lists.add(KEY_HTML_DESCRIPTION_PREFERENCE);
        return lists;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(this);
            ((DaltonizerRadioButtonPreferenceController) controller).displayPreference(
                    getPreferenceScreen());
        }
    }

    @Override
    public void onPause() {
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(null);
        }
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_correction;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_daltonizer_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        final boolean isEnabled = Settings.Secure.getInt(getContentResolver(), ENABLED, OFF) == ON;
        if (enabled == isEnabled) {
            return;
        }

        if (enabled) {
            showQuickSettingsTooltipIfNeeded(QuickSettingsTooltipType.GUIDE_TO_DIRECT_USE);
        }
        logAccessibilityServiceEnabled(mComponentName, enabled);
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? ON : OFF);
    }

    @Override
    protected void onRemoveSwitchPreferenceToggleSwitch() {
        super.onRemoveSwitchPreferenceToggleSwitch();
        mToggleServiceSwitchPreference.setOnPreferenceClickListener(null);
    }

    @Override
    protected void updateToggleServiceTitle(SettingsMainSwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.accessibility_daltonizer_primary_switch_title);
    }

    @Override
    protected CharSequence getShortcutTitle() {
        return getText(R.string.accessibility_daltonizer_shortcut_title);
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    @Override
    ComponentName getTileComponentName() {
        return DALTONIZER_TILE_COMPONENT_NAME;
    }

    @Override
    CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
        return getText(type == QuickSettingsTooltipType.GUIDE_TO_EDIT
                ? R.string.accessibility_color_correction_qs_tooltip_content
                : R.string.accessibility_color_correction_auto_added_qs_tooltip_content);
    }

    @Override
    protected void updateSwitchBarToggleSwitch() {
        final boolean checked = Settings.Secure.getInt(getContentResolver(), ENABLED, OFF) == ON;
        if (mToggleServiceSwitchPreference.isChecked() == checked) {
            return;
        }
        mToggleServiceSwitchPreference.setChecked(checked);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
}
