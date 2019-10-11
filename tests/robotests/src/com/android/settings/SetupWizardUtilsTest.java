/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.Bundle;
import android.sysprop.SetupWizardProperties;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SetupWizardUtilsTest {

    @Test
    public void testCopySetupExtras() {
        Intent fromIntent = new Intent();
        final String theme = "TEST_THEME";
        fromIntent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
        fromIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        Intent toIntent = new Intent();
        SetupWizardUtils.copySetupExtras(fromIntent, toIntent);

        assertThat(theme).isEqualTo(toIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME));
        assertThat(toIntent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, false))
                .isTrue();
        assertThat(toIntent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true))
                .isFalse();
    }

    @Test
    public void testCopyLifecycleExtra() {
        Intent fromIntent = new Intent();
        final String theme = "TEST_THEME";
        fromIntent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
        fromIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        Bundle dstBundle = new Bundle();
        dstBundle = SetupWizardUtils.copyLifecycleExtra(fromIntent.getExtras(), dstBundle);

        assertThat(dstBundle).isNotNull();
        assertThat(dstBundle.getString(WizardManagerHelper.EXTRA_THEME)).isNull();
        assertThat(dstBundle.getBoolean(WizardManagerHelper.EXTRA_IS_SETUP_FLOW))
                .isTrue();
        assertThat(dstBundle.getBoolean(WizardManagerHelper.EXTRA_IS_FIRST_RUN))
                .isFalse();
    }

    @Test
    public void testGetTheme_withIntentExtra_shouldReturnExtraTheme() {
        SetupWizardProperties.theme(ThemeHelper.THEME_GLIF);
        Intent intent = createSetupWizardIntent();
        intent.putExtra(WizardManagerHelper.EXTRA_THEME, ThemeHelper.THEME_GLIF_V2);

        assertThat(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV2Theme);
    }

    @Test
    public void testGetTheme_withEmptyIntent_shouldReturnSystemProperty() {
        SetupWizardProperties.theme(ThemeHelper.THEME_GLIF_V2_LIGHT);
        Intent intent = createSetupWizardIntent();

        assertThat(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV2Theme_Light);
    }

    @Test
    public void testGetTheme_glifV3Light_shouldReturnThemeResource() {
        SetupWizardProperties.theme(ThemeHelper.THEME_GLIF_V3_LIGHT);
        Intent intent = createSetupWizardIntent();

        assertThat(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV3Theme_Light);
        assertThat(SetupWizardUtils.getTransparentTheme(intent))
                .isEqualTo(R.style.GlifV3Theme_Light_Transparent);
    }

    @Test
    public void testGetTheme_nonSuw_shouldReturnDayNightTheme() {
        SetupWizardProperties.theme(ThemeHelper.THEME_GLIF_V3_LIGHT);
        Intent intent = new Intent();

        assertThat(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV3Theme);
        assertThat(SetupWizardUtils.getTransparentTheme(intent))
                .isEqualTo(R.style.GlifV3Theme_Transparent);
    }

    private Intent createSetupWizardIntent() {
        return new Intent()
                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true);
    }
}
