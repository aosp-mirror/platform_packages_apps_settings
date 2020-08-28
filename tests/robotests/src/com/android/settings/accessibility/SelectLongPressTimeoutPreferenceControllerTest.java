/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SelectLongPressTimeoutPreferenceControllerTest {
    private static final int VALID_VALUE = 1500;
    private static final int INVALID_VALUE = 0;
    private static final int DEFAULT_VALUE = 400;

    private Context mContext;
    private SelectLongPressTimeoutPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new SelectLongPressTimeoutPreferenceController(mContext, "press_timeout");
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_byDefault_shouldReturnShort() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, DEFAULT_VALUE);
        final String expected = "Short";

        assertThat(mController.getSummary()).isEqualTo(expected);
    }

    @Test
    public void getSummary_validValue_shouldReturnLong() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, VALID_VALUE);
        final String expected = "Long";

        assertThat(mController.getSummary()).isEqualTo(expected);
    }

    @Test
    public void getSummary_invalidValue_shouldReturnNull() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, INVALID_VALUE);

        assertThat(mController.getSummary()).isNull();
    }
}
