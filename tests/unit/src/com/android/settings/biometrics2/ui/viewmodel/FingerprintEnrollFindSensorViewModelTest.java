/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.viewmodel;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollFindSensorViewModelTest {

    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    private Application mApplication;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testClickStartButton() {
        final FingerprintEnrollFindSensorViewModel viewModel =
                new FingerprintEnrollFindSensorViewModel(mApplication, false);

        viewModel.onStartButtonClick();
        assertThat(viewModel.getActionLiveData().getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START);
    }

    @Test
    public void testClickSkipButton() {
        final FingerprintEnrollFindSensorViewModel viewModel =
                new FingerprintEnrollFindSensorViewModel(mApplication, false);

        viewModel.onSkipButtonClick();
        assertThat(viewModel.getActionLiveData().getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP);
    }

    @Test
    public void testClickSkipButtonInSuw() {
        final FingerprintEnrollFindSensorViewModel viewModel =
                new FingerprintEnrollFindSensorViewModel(mApplication, true);

        viewModel.onSkipButtonClick();
        assertThat(viewModel.getActionLiveData().getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG);
    }

    @Test
    public void testClickSkipDialogButton() {
        final FingerprintEnrollFindSensorViewModel viewModel =
                new FingerprintEnrollFindSensorViewModel(mApplication, true);

        viewModel.onSkipDialogButtonClick();
        assertThat(viewModel.getActionLiveData().getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP);
    }

    @Test
    public void testClearActionLiveData() {
        final FingerprintEnrollFindSensorViewModel viewModel =
                new FingerprintEnrollFindSensorViewModel(mApplication, false);

        viewModel.onStartButtonClick();
        assertThat(viewModel.getActionLiveData().getValue()).isNotNull();

        viewModel.clearActionLiveData();
        assertThat(viewModel.getActionLiveData().getValue()).isNull();
    }
}
