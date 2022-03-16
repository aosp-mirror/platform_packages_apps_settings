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
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link HighTextContrastPreferenceController}.
 */
@RunWith(AndroidJUnit4.class)
public class HighTextContrastPreferenceControllerTest {

    private static final String PREF_KEY = "text_contrast";
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int UNKNOWN = -1;

    private Context mContext;
    private SwitchPreference mPreference;
    private HighTextContrastPreferenceController mController;
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(PREF_KEY);
        mScreen.addPreference(mPreference);
        mController = new HighTextContrastPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_enabledTextContrast_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, ON);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledTextContrast_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, OFF);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableTextContrast() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, UNKNOWN)).isEqualTo(ON);

    }

    @Test
    public void setChecked_setFalse_shouldDisableTextContrast() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, UNKNOWN)).isEqualTo(OFF);
    }

    @Test
    public void resetState_shouldDisableTextContrast() {
        mController.displayPreference(mScreen);
        mController.setChecked(true);
        mPreference.setChecked(true);

        mController.resetState();

        assertThat(mPreference.isChecked()).isFalse();
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, UNKNOWN)).isEqualTo(OFF);
    }
}
