/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.ColorDisplayManager;

import androidx.preference.Preference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ColorModePreferenceControllerTest {

    private Preference mPreference;
    private ColorModePreferenceController mController;

    @Before
    public void setup() {
        final Context context = spy(ApplicationProvider.getApplicationContext());
        mController = spy(new ColorModePreferenceController(context, "test"));
        mPreference = new Preference(context);
        final Resources res = spy(context.getResources());
        when(res.getIntArray(com.android.internal.R.array.config_availableColorModes)).thenReturn(
                new int[]{
                        ColorDisplayManager.COLOR_MODE_NATURAL,
                        ColorDisplayManager.COLOR_MODE_BOOSTED,
                        ColorDisplayManager.COLOR_MODE_SATURATED,
                        ColorDisplayManager.COLOR_MODE_AUTOMATIC
                });
        doReturn(res).when(context).getResources();
    }

    @Test
    @UiThreadTest
    public void updateState_colorModeAutomatic_shouldSetSummaryToAutomatic() {
        doReturn(ColorDisplayManager.COLOR_MODE_AUTOMATIC).when(mController).getColorMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Adaptive");
    }

    @Test
    @UiThreadTest
    public void updateState_colorModeSaturated_shouldSetSummaryToSaturated() {
        doReturn(ColorDisplayManager.COLOR_MODE_SATURATED).when(mController).getColorMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Saturated");
    }

    @Test
    public void updateState_colorModeBoosted_shouldSetSummaryToBoosted() {
        doReturn(ColorDisplayManager.COLOR_MODE_BOOSTED).when(mController).getColorMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Boosted");
    }

    @Test
    public void updateState_colorModeNatural_shouldSetSummaryToNatural() {
        doReturn(ColorDisplayManager.COLOR_MODE_NATURAL).when(mController).getColorMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Natural");
    }
}
