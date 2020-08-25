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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveConnectivityTogglePreferenceControllerTest {

    private static final String PREF_KEY = "adaptive_connectivity";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;

    private AdaptiveConnectivityTogglePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new AdaptiveConnectivityTogglePreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 0);

        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1))
                .isEqualTo(1);
    }

    @Test
    public void setChecked_withFalse_shouldUpdateSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1);

        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1))
                .isEqualTo(0);
    }
}
