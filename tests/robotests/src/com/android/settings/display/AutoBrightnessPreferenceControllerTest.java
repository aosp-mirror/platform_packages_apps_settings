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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class AutoBrightnessPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "auto_brightness";

    private Context mContext;
    private AutoBrightnessPreferenceController mController;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new AutoBrightnessPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void onPreferenceChange_TurnOnAuto_ReturnAuto() {
        mController.onPreferenceChange(null, true);

        final int mode = Settings.System.getInt(mContentResolver, SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_MANUAL);
        assertThat(mode).isEqualTo(SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    @Test
    public void onPreferenceChange_TurnOffAuto_ReturnManual() {
        mController.onPreferenceChange(null, false);

        final int mode = Settings.System.getInt(mContentResolver, SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        assertThat(mode).isEqualTo(SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    @Test
    public void setChecked_updatesCorrectly() {
        mController.setChecked(true);

        assertThat(mController.isChecked()).isTrue();

        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_no() {
        Settings.System.putInt(mContentResolver, SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_MANUAL);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_yes() {
        Settings.System.putInt(mContentResolver, SCREEN_BRIGHTNESS_MODE,
                SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void getSummary_settingOn_shouldReturnOnSummary() {
        mController.setChecked(true);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.auto_brightness_summary_on));
    }

    @Test
    public void getSummary_settingOff_shouldReturnOffSummary() {
        mController.setChecked(false);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.auto_brightness_summary_off));
    }

    @Test
    public void getAvailabilityStatus_configTrueSet_shouldReturnAvailableUnsearchable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_configFalseSet_shouldReturnUnsupportedOnDevice() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
