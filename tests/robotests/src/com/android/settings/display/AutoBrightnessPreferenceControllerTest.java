/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AutoBrightnessPreferenceControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private AutoBrightnessPreferenceController mController;
    private final String PREFERENCE_KEY = "auto_brightness";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new AutoBrightnessPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void testOnPreferenceChange_TurnOnAuto_ReturnAuto() {
        mController.onPreferenceChange(null, true);

        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        assertThat(mode).isEqualTo(SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    @Test
    public void testOnPreferenceChange_TurnOffAuto_ReturnManual() {
        mController.onPreferenceChange(null, false);

        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        assertThat(mode).isEqualTo(SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    @Test
    public void testSetValue_updatesCorrectly() {
        boolean newValue = true;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);

        mController.setChecked(newValue);
        boolean updatedValue = Settings.System.getInt(resolver, SCREEN_BRIGHTNESS_MODE, -1)
                != SCREEN_BRIGHTNESS_MODE_MANUAL;

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testGetValue_correctValueReturned() {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        int newValue = mController.isChecked() ?
                SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : SCREEN_BRIGHTNESS_MODE_MANUAL;

        assertThat(newValue).isEqualTo(SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }
}
