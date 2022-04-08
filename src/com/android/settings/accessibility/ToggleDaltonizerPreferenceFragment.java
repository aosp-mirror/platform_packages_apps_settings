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
import static com.android.settings.accessibility.AccessibilityStatsLogUtils.logAccessibilityServiceEnabled;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings for daltonizer. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public final class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerRadioButtonPreferenceController.OnChangeListener {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String KEY_PREVIEW = "daltonizer_preview";
    private static final String KEY_CATEGORY_MODE = "daltonizer_mode_category";
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private SettingsContentObserver mSettingsContentObserver;

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        if (sControllers.size() == 0) {
            final Resources resources = context.getResources();
            final String[] daltonizerKeys = resources.getStringArray(
                    R.array.daltonizer_mode_keys);

            for (int i = 0; i < daltonizerKeys.length; i++) {
                sControllers.add(new DaltonizerRadioButtonPreferenceController(
                        context, lifecycle, daltonizerKeys[i]));
            }
        }
        return sControllers;
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mComponentName = DALTONIZER_COMPONENT_NAME;
        mPackageName = getText(R.string.accessibility_display_daltonizer_preference_title);
        mHtmlDescription = getText(R.string.accessibility_display_daltonizer_preference_subtitle);
        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(ENABLED);
        mSettingsContentObserver = new SettingsContentObserver(mHandler, enableServiceFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateSwitchBarToggleSwitch();
            }
        };
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferenceOrder();
    }

    /** Customizes the order by preference key. */
    private List<String> getPreferenceOrderList() {
        List<String> lists = new ArrayList<>();
        lists.add(KEY_PREVIEW);
        lists.add(KEY_USE_SERVICE_PREFERENCE);
        lists.add(KEY_CATEGORY_MODE);
        lists.add(KEY_GENERAL_CATEGORY);
        lists.add(KEY_INTRODUCTION_CATEGORY);
        return lists;
    }

    private void updatePreferenceOrder() {
        List<String> lists = getPreferenceOrderList();
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);

        final int size = lists.size();
        for (int i = 0; i < size; i++) {
            final Preference preference = preferenceScreen.findPreference(lists.get(i));
            if (preference != null) {
                preference.setOrder(i);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
        mSettingsContentObserver.register(getContentResolver());

        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(this);
            ((DaltonizerRadioButtonPreferenceController) controller).displayPreference(
                    getPreferenceScreen());
        }
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
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
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        logAccessibilityServiceEnabled(mComponentName, enabled);
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? ON : OFF);
    }

    @Override
    protected void onRemoveSwitchPreferenceToggleSwitch() {
        super.onRemoveSwitchPreferenceToggleSwitch();
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener(null);
    }

    @Override
    protected void updateToggleServiceTitle(SwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.accessibility_daltonizer_master_switch_title);
    }

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        super.onInstallSwitchPreferenceToggleSwitch();
        updateSwitchBarToggleSwitch();
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener((preference) -> {
            boolean checked = ((SwitchPreference) preference).isChecked();
            onPreferenceToggled(mPreferenceKey, checked);
            return false;
        });
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        super.onSettingsClicked(preference);
        showDialog(DialogEnums.EDIT_SHORTCUT);
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    private void updateSwitchBarToggleSwitch() {
        final boolean checked = Settings.Secure.getInt(getContentResolver(), ENABLED, OFF) == ON;
        if (mToggleServiceDividerSwitchPreference.isChecked() == checked) {
            return;
        }
        mToggleServiceDividerSwitchPreference.setChecked(checked);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
}
