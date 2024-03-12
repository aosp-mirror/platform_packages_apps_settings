/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link FontWeightAdjustmentPreferenceController}.
 */
@RunWith(AndroidJUnit4.class)
public class FontWeightAdjustmentPreferenceControllerTest {
    private static final int ON = FontWeightAdjustmentPreferenceController.BOLD_TEXT_ADJUSTMENT;
    private static final int OFF = 0;

    private Context mContext;
    private SwitchPreference mPreference;
    private FontWeightAdjustmentPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new SwitchPreference(mContext);
        mController = new FontWeightAdjustmentPreferenceController(
                mContext, "font_weight_adjustment");
    }

    @After
    public void teardown() {
        Settings.Secure.resetToDefaults(mContext.getContentResolver(), /* tag= */ null);
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_enabledBoldText_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, ON);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledBoldText_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, OFF);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableBoldText() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, OFF)).isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableBoldText() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, OFF)).isEqualTo(OFF);
    }

    @Test
    public void resetState_shouldDisableBoldText() {
        mController.setChecked(true);

        mController.resetState();

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, OFF)).isEqualTo(OFF);
    }
}
