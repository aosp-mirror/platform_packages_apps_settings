/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DisableAutomaticUpdatesPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private DisableAutomaticUpdatesPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DisableAutomaticUpdatesPreferenceController(mContext, "test");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAVAILABLE() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_valueIsZeroInProvider_shouldReturnTrue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_valueIsOneInProvider_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                DisableAutomaticUpdatesPreferenceController.DISABLE_UPDATES_SETTING);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_true_providerValueIsZero() {
        mController.setChecked(true);

        int value = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING /* default */);

        assertThat(value).isEqualTo(
                DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING);
    }

    @Test
    public void setChecked_false_providerValueIsOne() {
        mController.setChecked(false);

        int value = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING /* default */);

        assertThat(value).isEqualTo(
                DisableAutomaticUpdatesPreferenceController.DISABLE_UPDATES_SETTING);
    }
}
