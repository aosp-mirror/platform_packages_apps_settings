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
import android.provider.Settings;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DoubleTapPowerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    private DoubleTapPowerPreferenceController mController;
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTapPowerPreferenceController(mContext, null, KEY_DOUBLE_TAP_POWER);
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnFalse() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testSwitchEnabled_configIsNotSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
        mController = new DoubleTapPowerPreferenceController(context, null, KEY_DOUBLE_TAP_POWER);

        assertThat(mController.isSwitchPrefEnabled()).isTrue();
    }

    @Test
    public void testSwitchEnabled_configIsSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 1);
        mController = new DoubleTapPowerPreferenceController(context, null, KEY_DOUBLE_TAP_POWER);

        assertThat(mController.isSwitchPrefEnabled()).isFalse();
    }
}
