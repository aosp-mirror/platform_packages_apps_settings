/*
 * Copyright (C) 2023 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.res.Resources;
import android.os.Bundle;

import com.android.settings.Settings;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class FaceSettingsActivityTest {

    private static final String APPLIED_SETUP_WIZARD_THEME = "SettingsPreferenceTheme.SetupWizard";

    private Settings.FaceSettingsActivity mActivity;
    private Resources.Theme mTheme;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mActivity = spy(Settings.FaceSettingsActivity.class);
    }

    @Test
    public void verifyFaceSettingsActivity_shouldAppliedSetupWizardTheme() {
        createActivity();

        assertThat(isThemeApplied(APPLIED_SETUP_WIZARD_THEME)).isTrue();
    }

    private boolean isThemeApplied(String themeName) {
        final String [] appliedThemes =  mTheme.getTheme();
        for (String theme : appliedThemes) {
            if (theme.contains(themeName)) {
                return true;
            }
        }
        return false;
    }

    private void createActivity() {
        ActivityController.of(mActivity).create(new Bundle());
        mTheme = mActivity.getTheme();
    }
}
