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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class QRCodeScannerPreferenceControllerTest {
    private static final String TEST_KEY = "test_key";
    private static final String SETTING_KEY = Settings.Secure.LOCK_SCREEN_SHOW_QR_CODE_SCANNER;
    private static final String DEFAULT_COMPONENT =
            Settings.Secure.SHOW_QR_CODE_SCANNER_SETTING;

    private Context mContext;
    private ContentResolver mContentResolver;
    private QRCodeScannerPreferenceController mController;

    @Mock
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new QRCodeScannerPreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void isChecked_SettingIs1_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_SettingIs0_returnFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_SettingIsNotSet_returnFalse() {
        Settings.Secure.putString(mContentResolver, SETTING_KEY, null);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_true_SettingIsNot0() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isNotEqualTo(0);
    }

    @Test
    public void setChecked_false_SettingIs0() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isEqualTo(0);
    }

    @Test
    public void getAvailabilityStatus_defaultComponentNotSet() {
        Settings.Secure.putString(mContext.getContentResolver(), DEFAULT_COMPONENT, null);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_defaultComponentSet() {
        Settings.Secure.putString(mContext.getContentResolver(), DEFAULT_COMPONENT, "abc");
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
