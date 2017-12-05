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

import static com.android.settings.testutils.ResIdSubject.assertResId;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;

import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.setupwizardlib.util.WizardManagerHelper;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowSystemProperties.class
        })
public class SetupWizardUtilsTest {

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void testCopySetupExtras() throws Throwable {
        Intent fromIntent = new Intent();
        final String theme = "TEST_THEME";
        fromIntent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
        fromIntent.putExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, true);
        Intent toIntent = new Intent();
        SetupWizardUtils.copySetupExtras(fromIntent, toIntent);

        assertThat(theme).isEqualTo(toIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME));
        assertThat(toIntent.getBooleanExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, false))
                .isTrue();
    }

    @Test
    public void testGetTheme_withIntentExtra_shouldReturnExtraTheme() {
        SettingsShadowSystemProperties.set(SetupWizardUtils.SYSTEM_PROP_SETUPWIZARD_THEME,
                WizardManagerHelper.THEME_GLIF);
        Intent intent = new Intent();
        intent.putExtra(WizardManagerHelper.EXTRA_THEME, WizardManagerHelper.THEME_GLIF_V2);

        assertResId(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV2Theme);
    }

    @Test
    public void testGetTheme_withEmptyIntent_shouldReturnSystemProperty() {
        SettingsShadowSystemProperties.set(SetupWizardUtils.SYSTEM_PROP_SETUPWIZARD_THEME,
                WizardManagerHelper.THEME_GLIF_V2_LIGHT);
        Intent intent = new Intent();

        assertResId(SetupWizardUtils.getTheme(intent)).isEqualTo(R.style.GlifV2Theme_Light);
    }

}
