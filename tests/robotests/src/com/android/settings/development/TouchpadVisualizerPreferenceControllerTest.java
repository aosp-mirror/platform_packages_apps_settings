/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.hardware.input.Flags;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class TouchpadVisualizerPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;

    private TouchpadVisualizerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new TouchpadVisualizerPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    @EnableFlags({Flags.FLAG_TOUCHPAD_VISUALIZER})
    public void updateState_touchpadVisualizerEnabled_shouldCheckedPreference() {
        InputSettings.setTouchpadVisualizer(mContext, true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    @EnableFlags({Flags.FLAG_TOUCHPAD_VISUALIZER})
    public void updateState_touchpadVisualizerDisabled_shouldUncheckedPreference() {
        InputSettings.setTouchpadVisualizer(mContext, false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    @EnableFlags({Flags.FLAG_TOUCHPAD_VISUALIZER})
    public void onPreferenceChange_preferenceChecked_shouldEnableTouchpadVisualizer() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean touchpadVisualizer = InputSettings.useTouchpadVisualizer(mContext);

        assertThat(touchpadVisualizer).isTrue();
    }

    @Test
    @EnableFlags({Flags.FLAG_TOUCHPAD_VISUALIZER})
    public void onPreferenceChange_preferenceUnchecked_shouldDisableTouchpadVisualizer() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean touchpadVisualizer = InputSettings.useTouchpadVisualizer(mContext);

        assertThat(touchpadVisualizer).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_TOUCHPAD_VISUALIZER})
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        final boolean touchpadVisualizer = InputSettings.useTouchpadVisualizer(mContext);

        assertThat(touchpadVisualizer).isFalse();
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
