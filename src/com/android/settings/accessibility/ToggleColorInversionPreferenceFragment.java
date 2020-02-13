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
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings page for color inversion. */
@SearchIndexable
public class ToggleColorInversionPreferenceFragment extends ToggleFeaturePreferenceFragment {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private static final String CATEGORY_FOOTER_KEY = "color_inversion_footer_category";
    private static final int DIALOG_ID_EDIT_SHORTCUT = 1;
    private final Handler mHandler = new Handler();
    private SettingsContentObserver mSettingsContentObserver;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? ON : OFF);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_color_inversion_settings;
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        switchBar.setSwitchBarText(R.string.accessibility_display_inversion_switch_title,
                R.string.accessibility_display_inversion_switch_title);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        updateSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener((toggleSwitch, checked) -> {
            onPreferenceToggled(mPreferenceKey, checked);
            return false;
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mComponentName = COLOR_INVERSION_COMPONENT_NAME;
        mPackageName = getString(R.string.accessibility_display_inversion_preference_title);
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

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        final PreferenceCategory footerCategory = preferenceScreen.findPreference(
                CATEGORY_FOOTER_KEY);
        footerCategory.setOrder(Integer.MAX_VALUE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        super.onSettingsClicked(preference);
        showDialog(DIALOG_ID_EDIT_SHORTCUT);
    }

    private void updateSwitchBarToggleSwitch() {
        final boolean checked = Settings.Secure.getInt(getContentResolver(), ENABLED, OFF) == ON;
        if (mSwitchBar.isChecked() == checked) {
            return;
        }
        mSwitchBar.setCheckedInternal(checked);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_color_inversion_settings);
}
