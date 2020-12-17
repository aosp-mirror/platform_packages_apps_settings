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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
/** TODO(b/170970675): Update and add tests after ColorDisplayService work is integrated */
public class ReduceBrightColorsIntensityPreferenceControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final ReduceBrightColorsIntensityPreferenceController mPreferenceController =
            new ReduceBrightColorsIntensityPreferenceController(mContext,
            "rbc_intensity");

    @Test
    public void isAvailable_configuredRbcAvailable_enabledRbc_shouldReturnTrue() {
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }
    @Test
    public void isAvailable_configuredRbcAvailable_disabledRbc_shouldReturnFalse() {
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }
    @Test
    public void isAvailable_configuredRbcUnavailable_enabledRbc_shouldReturnFalse() {
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }
}
