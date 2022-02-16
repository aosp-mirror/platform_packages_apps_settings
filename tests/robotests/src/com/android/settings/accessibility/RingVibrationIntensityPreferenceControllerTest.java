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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Vibrator;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link RingVibrationIntensityPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class RingVibrationIntensityPreferenceControllerTest {
    @Mock
    private Vibrator mVibrator;

    private RingVibrationIntensityPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final Context mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mVibrator).when(mContext).getSystemService(Vibrator.class);
        mController = new RingVibrationIntensityPreferenceController(mContext);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getDefaultIntensity_success() {
        doReturn(/* toBeReturned= */ 5).when(mVibrator).getDefaultRingVibrationIntensity();

        assertThat(mController.getDefaultIntensity()).isEqualTo(/* expected= */ 5);
    }
}
