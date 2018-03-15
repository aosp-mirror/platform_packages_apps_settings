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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class ColorModePreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private ColorDisplayController mColorDisplayController;

    private Context mContext;
    private ColorModePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new ColorModePreferenceController(mContext));
        doReturn(mColorDisplayController).when(mController).getColorDisplayController();
    }

    @Test
    public void updateState_colorModeAutomatic_shouldSetSummaryToAutomatic() {
        when(mColorDisplayController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_AUTOMATIC);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.color_mode_option_automatic));
    }

    @Test
    public void updateState_colorModeSaturated_shouldSetSummaryToSaturated() {
        when(mColorDisplayController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_SATURATED);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.color_mode_option_saturated));
    }

    @Test
    public void updateState_colorModeBoosted_shouldSetSummaryToBoosted() {
        when(mColorDisplayController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_BOOSTED);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.color_mode_option_boosted));
    }

    @Test
    public void updateState_colorModeNatural_shouldSetSummaryToNatural() {
        when(mColorDisplayController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_NATURAL);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.color_mode_option_natural));
    }

}
