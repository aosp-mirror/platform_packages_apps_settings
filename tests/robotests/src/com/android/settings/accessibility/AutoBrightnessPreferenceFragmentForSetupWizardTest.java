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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AutoBrightnessPreferenceFragmentForSetupWizard}. */
@RunWith(RobolectricTestRunner.class)
public class AutoBrightnessPreferenceFragmentForSetupWizardTest {

    // Same as AutoBrightnessPreferenceFragmentForSetupWizard#FOOTER_PREFERENCE_KEY
    private static final String FOOTER_PREFERENCE_KEY = "auto_brightness_footer";

    private FragmentScenario<AutoBrightnessPreferenceFragmentForSetupWizard> mFragmentScenario;

    private AutoBrightnessPreferenceFragmentForSetupWizard mFragment;
    private GlifLayout mGlifLayout;

    @Before
    public void setUp() {
        mFragmentScenario = FragmentScenario
                .launch(
                        AutoBrightnessPreferenceFragmentForSetupWizard.class,
                        /* fragmentArgs= */ (Bundle) null,
                        R.style.GlifTheme,
                        /* factory= */ (FragmentFactory) null)
                .moveToState(Lifecycle.State.RESUMED);
        mFragmentScenario.onFragment(fragment -> mFragment = fragment);

        View view = mFragment.getView();
        assertThat(view).isInstanceOf(GlifPreferenceLayout.class);
        mGlifLayout = (GlifLayout) view;
    }

    @After
    public void tearDown() {
        mFragmentScenario.close();
    }

    @Test
    public void onViewCreated_verifyGlifHerderText() {
        assertThat(mGlifLayout.getHeaderText())
                .isEqualTo(mFragment.getString(R.string.auto_brightness_title));
    }

    @Test
    public void onViewCreated_verifyGlifFooter() {
        FooterBarMixin footerMixin = mGlifLayout.getMixin(FooterBarMixin.class);
        assertThat(footerMixin).isNotNull();

        Button footerButton = footerMixin.getPrimaryButtonView();
        assertThat(footerButton).isNotNull();
        assertThat(footerButton.getText().toString()).isEqualTo(mFragment.getString(R.string.done));

        footerButton.performClick();
        assertThat(mFragment.getActivity().isFinishing()).isTrue();
    }

    @Test
    public void onViewCreated_verifyFooterPreference() {
        Preference pref = mFragment.findPreference(FOOTER_PREFERENCE_KEY);
        assertThat(pref).isInstanceOf(FooterPreference.class);

        FooterPreference footerPref = (FooterPreference) pref;
        String exactTitle = footerPref.getTitle().toString();
        assertThat(exactTitle).isEqualTo(mFragment.getString(R.string.auto_brightness_description));

        // Ensure that footer content description has "About XXX" prefix for consistency with other
        // accessibility suw pages
        String expectedContentDescription =
                mFragment.getString(R.string.auto_brightness_content_description_title)
                        + "\n\n" + exactTitle;
        assertThat(footerPref.getContentDescription().toString())
                .isEqualTo(expectedContentDescription);
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.SUW_ACCESSIBILITY_AUTO_BRIGHTNESS);
    }
}
