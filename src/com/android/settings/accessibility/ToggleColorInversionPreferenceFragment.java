/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_TILE_COMPONENT_NAME;
import static com.android.settings.accessibility.AccessibilityStatsLogUtils.logAccessibilityServiceEnabled;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for color inversion.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleColorInversionPreferenceFragment extends ToggleFeaturePreferenceFragment {

    private static final String TAG = "ToggleColorInversionPreferenceFragment";
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;

    private static final String KEY_SHORTCUT_PREFERENCE = "color_inversion_shortcut_key";

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
        mComponentName = COLOR_INVERSION_COMPONENT_NAME;
        mPackageName = getText(R.string.accessibility_display_inversion_preference_title);
        mHtmlDescription = getText(R.string.accessibility_display_inversion_preference_subtitle);
        mTopIntroTitle = getText(R.string.accessibility_display_inversion_preference_intro_text);
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getPrefContext().getPackageName())
            .appendPath(String.valueOf(R.raw.a11y_color_inversion_banner))
            .build();
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        updateFooterPreference();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View rootView = getActivity().getWindow().peekDecorView();
        if (rootView != null) {
            rootView.setAccessibilityPaneTitle(getString(
                    R.string.accessibility_display_inversion_preference_title));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS;
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
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_color_inversion_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void onRemoveSwitchPreferenceToggleSwitch() {
        super.onRemoveSwitchPreferenceToggleSwitch();
        mToggleServiceSwitchPreference.setOnPreferenceClickListener(null);
    }

    @Override
    protected void updateToggleServiceTitle(SettingsMainSwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.accessibility_display_inversion_switch_title);
    }

    @Override
    protected CharSequence getShortcutTitle() {
        return getText(R.string.accessibility_display_inversion_shortcut_title);
    }

    private void updateFooterPreference() {
        final String title = getPrefContext().getString(
                R.string.accessibility_color_inversion_about_title);
        final String learnMoreText = getPrefContext().getString(
                R.string.accessibility_color_inversion_footer_learn_more_content_description);
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.setupHelpLink(getHelpResource(), learnMoreText);
        mFooterPreferenceController.displayPreference(getPreferenceScreen());
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
    public int getHelpResource() {
        return R.string.help_url_color_inversion;
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
            mComponentName);
    }

    @Override
    ComponentName getTileComponentName() {
        return COLOR_INVERSION_TILE_COMPONENT_NAME;
    }

    @Override
    CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
        return getText(type == QuickSettingsTooltipType.GUIDE_TO_EDIT
            ? R.string.accessibility_color_inversion_qs_tooltip_content
            : R.string.accessibility_color_inversion_auto_added_qs_tooltip_content);
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
            new BaseSearchIndexProvider(R.xml.accessibility_color_inversion_settings) {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> rawData = new ArrayList<>();
                    SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.key = KEY_SHORTCUT_PREFERENCE;
                    raw.title = context.getString(
                        R.string.accessibility_display_inversion_shortcut_title);
                    rawData.add(raw);
                    return rawData;
                }
            };
}
