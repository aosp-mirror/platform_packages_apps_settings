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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.SILENCE_GESTURE;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SilenceGesturePreferenceControllerTest {

    private static final String KEY_SILENCE = "gesture_silence";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Resources mResources;

    private SilenceGesturePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new SilenceGesturePreferenceController(mContext, KEY_SILENCE);
    }

    @Test
    public void getAvailabilityStatus_gestureNotSupported_UNSUPPORTED_ON_DEVICE() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_silenceSensorAvailable))
                .thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_gestureSupported_AVAILABLE() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_silenceSensorAvailable)).thenReturn(true);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(AVAILABLE);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isChecked_testTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), SILENCE_GESTURE, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_testFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), SILENCE_GESTURE, 0);
        assertThat(mController.isChecked()).isFalse();
    }
}
