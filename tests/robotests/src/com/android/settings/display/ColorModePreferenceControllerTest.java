/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.hardware.display.ColorDisplayManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ColorModePreferenceControllerTest {

    @Mock
    private ColorDisplayManager mColorDisplayManager;

    private Context mContext;
    private Preference mPreference;
    private ColorModePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new ColorModePreferenceController(mContext, "test"));
        mPreference = new Preference(mContext);
        doReturn(mColorDisplayManager).when(mController).getColorDisplayManager();
    }

    @Test
    public void updateState_colorModeAutomatic_shouldSetSummaryToAutomatic() {
        when(mColorDisplayManager.getColorMode())
                .thenReturn(ColorDisplayManager.COLOR_MODE_AUTOMATIC);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.color_mode_option_automatic));
    }

    @Test
    public void updateState_colorModeSaturated_shouldSetSummaryToSaturated() {
        when(mColorDisplayManager.getColorMode())
                .thenReturn(ColorDisplayManager.COLOR_MODE_SATURATED);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.color_mode_option_saturated));
    }

    @Test
    public void updateState_colorModeBoosted_shouldSetSummaryToBoosted() {
        when(mColorDisplayManager.getColorMode())
                .thenReturn(ColorDisplayManager.COLOR_MODE_BOOSTED);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.color_mode_option_boosted));
    }

    @Test
    public void updateState_colorModeNatural_shouldSetSummaryToNatural() {
        when(mColorDisplayManager.getColorMode())
                .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.color_mode_option_natural));
    }
}
