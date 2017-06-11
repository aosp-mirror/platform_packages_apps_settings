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

package com.android.settings.gestures;

import android.content.Context;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DoubleTapScreenPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private DoubleTapScreenPreferenceController mController;

    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTapScreenPreferenceController(
                mContext, null, mAmbientDisplayConfiguration, 0, KEY_DOUBLE_TAP_SCREEN);
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapAvailable()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsFalse_shouldReturnFalse() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapAvailable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testSwitchEnabled_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isSwitchPrefEnabled()).isTrue();
    }

    @Test
    public void testSwitchEnabled_configIsNotSet_shouldReturnFalse() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isSwitchPrefEnabled()).isFalse();
    }
}
