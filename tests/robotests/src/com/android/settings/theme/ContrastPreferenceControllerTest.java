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
 * limitations under the License.
 */

package com.android.settings.theme;

import static android.app.UiModeManager.ContrastUtils;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD;
import static android.provider.Settings.Secure.CONTRAST_LEVEL;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.stream.Stream;

@RunWith(RobolectricTestRunner.class)
public class ContrastPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    private ContrastPreferenceController mController;

    @Mock
    private UiModeManager mMockUiModeManager;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ContrastPreferenceController(mContext, mMockUiModeManager);
    }

    @Test
    public void controllerIsAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Ignore("b/313614100")
    @Test
    public void testHandlePreferenceTreeClick() {
        Preference preference = new Preference(mContext);
        preference.setKey(ContrastPreferenceController.KEY);
        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();

        Preference otherPreference = new Preference(mContext);
        otherPreference.setKey("wrong key");
        assertThat(mController.handlePreferenceTreeClick(otherPreference)).isFalse();
    }

    @Test
    public void controllerSummary() {
        float initialContrast = mContext.getSystemService(UiModeManager.class).getContrast();
        try {
            allContrastValues().forEach(contrastLevel -> {
                float contrast = ContrastUtils.fromContrastLevel(contrastLevel);
                clearInvocations(mMockUiModeManager);
                when(mMockUiModeManager.getContrast()).thenReturn(contrast);
                String summary = mController.getSummary().toString();
                verify(mMockUiModeManager).getContrast();
                assertThat(summary).isEqualTo(mController.getSummary(contrastLevel));
            });
        } finally {
            putContrastInSettings(initialContrast);
        }
    }

    private static Stream<Integer> allContrastValues() {
        return Stream.of(CONTRAST_LEVEL_STANDARD, CONTRAST_LEVEL_MEDIUM, CONTRAST_LEVEL_HIGH);
    }

    private void putContrastInSettings(float contrast) {
        Settings.Secure.putFloat(mContext.getContentResolver(), CONTRAST_LEVEL, contrast);
    }
}
