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

package com.android.settings.development;

import static com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin.SECURE_OVERLAY_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OverlaySettingsPreferenceControllerTest {

    private Context mContext;
    private SwitchPreference mPreference;
    private OverlaySettingsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new OverlaySettingsPreferenceController(mContext);
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_isOverlaySettingsEnabled_shouldCheckPreference() {
        OverlaySettingsPreferenceController.setOverlaySettingsEnabled(mContext, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_isOverlaySettingsDisabled_shouldUncheckPreference() {
        OverlaySettingsPreferenceController.setOverlaySettingsEnabled(mContext, false);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableSettings() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isTrue();
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_shouldDisableSettings() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(
                OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isFalse();
    }

    @Test
    public void isOverlaySettingsEnabled_sharePreferenceSetTrue_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                SECURE_OVERLAY_SETTINGS, 1);

        assertThat(OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isTrue();
    }

    @Test
    public void isOverlaySettingsEnabled_sharePreferenceSetFalse_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                SECURE_OVERLAY_SETTINGS, 0);

        assertThat(
                OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isFalse();
    }

    @Test
    public void setOverlaySettingsEnabled_setTrue_shouldStoreTrue() {
        OverlaySettingsPreferenceController.setOverlaySettingsEnabled(mContext, true);

        assertThat(
                OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isTrue();
    }

    @Test
    public void setOverlaySettingsEnabled_setFalse_shouldStoreTrue() {
        OverlaySettingsPreferenceController.setOverlaySettingsEnabled(mContext, false);

        assertThat(
                OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mContext)).isFalse();
    }
}
