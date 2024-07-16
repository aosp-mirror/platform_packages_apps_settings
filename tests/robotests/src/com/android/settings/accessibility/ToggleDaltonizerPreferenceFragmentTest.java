/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment.KEY_USE_SERVICE_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.Flags;
import android.widget.PopupWindow;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

/** Tests for {@link ToggleDaltonizerPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleDaltonizerPreferenceFragmentTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ActivityController<SettingsActivity> mActivityController;

    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                ToggleDaltonizerPreferenceFragment.class.getName());
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, new Bundle());

        mActivityController = ActivityController.of(new SettingsActivity(), intent);
    }

    @Test
    public void onResume_colorCorrectEnabled_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);

        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();

        SettingsMainSwitchPreference switchPreference = getMainFeatureToggle(fragment);
        assertThat(switchPreference.isChecked()).isTrue();
    }

    @Test
    public void onResume_colorCorrectDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF);

        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();

        SettingsMainSwitchPreference switchPreference = getMainFeatureToggle(fragment);
        assertThat(switchPreference.isChecked()).isFalse();
    }

    @Test
    public void onResume_colorCorrectEnabled_switchPreferenceChecked_notShowTooltips() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);

        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();
        SettingsMainSwitchPreference switchPreference = getMainFeatureToggle(fragment);
        assertThat(switchPreference.isChecked()).isTrue();

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void onPreferenceToggled_colorCorrectDisabled_shouldReturnTrueAndShowTooltipView() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF);
        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();
        SettingsMainSwitchPreference switchPreference = getMainFeatureToggle(fragment);

        fragment.onPreferenceToggled(switchPreference.getKey(), true);

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF) == ON;
        assertThat(isEnabled).isTrue();
        assertThat(getLatestPopupWindow()).isNotNull();
        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void onPreferenceToggled_colorCorrectEnabled_shouldReturnFalseAndNotShowTooltipView() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);
        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();
        SettingsMainSwitchPreference switchPreference = getMainFeatureToggle(fragment);

        fragment.onPreferenceToggled(switchPreference.getKey(), false);

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF) == ON;
        assertThat(isEnabled).isFalse();
        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();

        assertThat(fragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();

        assertThat(fragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_daltonizer_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        ToggleDaltonizerPreferenceFragment fragment = getFragmentInResumedState();

        assertThat(fragment.getHelpResource()).isEqualTo(R.string.help_url_color_correction);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ToggleDaltonizerPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_daltonizer_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    private ToggleDaltonizerPreferenceFragment getFragmentInResumedState() {

        mActivityController.create().start().resume();
        Fragment fragment = mActivityController.get().getSupportFragmentManager().findFragmentById(
                R.id.main_content);

        assertThat(fragment).isNotNull();
        assertThat(fragment).isInstanceOf(ToggleDaltonizerPreferenceFragment.class);

        return (ToggleDaltonizerPreferenceFragment) fragment;
    }

    private SettingsMainSwitchPreference getMainFeatureToggle(
            ToggleDaltonizerPreferenceFragment fragment) {
        return fragment.findPreference(KEY_USE_SERVICE_PREFERENCE);
    }
}
