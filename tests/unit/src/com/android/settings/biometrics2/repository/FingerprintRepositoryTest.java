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

package com.android.settings.biometrics2.repository;

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_HOME_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_POWER_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UNKNOWN;

import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupSuwMaxFingerprintsEnrollable;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintRepositoryTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private Resources mResources;
    @Mock private FingerprintManager mFingerprintManager;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testCanAssumeSensorType_forUnknownSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 1);
        assertThat(repository.canAssumeUdfps()).isFalse();
        assertThat(repository.canAssumeSfps()).isFalse();
    }

    @Test
    public void testCanAssumeSensorType_forRearSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_REAR, 1);
        assertThat(repository.canAssumeUdfps()).isFalse();
        assertThat(repository.canAssumeSfps()).isFalse();
    }

    @Test
    public void testCanAssumeSensorType_forUdfpsUltrasonicSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UDFPS_ULTRASONIC, 1);
        assertThat(repository.canAssumeUdfps()).isTrue();
        assertThat(repository.canAssumeSfps()).isFalse();
    }

    @Test
    public void testCanAssumeSensorType_forUdfpsOpticalSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UDFPS_OPTICAL, 1);
        assertThat(repository.canAssumeUdfps()).isTrue();
        assertThat(repository.canAssumeSfps()).isFalse();
    }

    @Test
    public void testCanAssumeSensorType_forPowerButtonSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_POWER_BUTTON, 1);
        assertThat(repository.canAssumeUdfps()).isFalse();
        assertThat(repository.canAssumeSfps()).isTrue();
    }

    @Test
    public void testCanAssumeSensorType_forHomeButtonSensor() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_HOME_BUTTON, 1);
        assertThat(repository.canAssumeUdfps()).isFalse();
        assertThat(repository.canAssumeSfps()).isFalse();
    }

    @Test
    public void testGetMaxFingerprints() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 999);
        assertThat(repository.getMaxFingerprints()).isEqualTo(999);
    }

    @Test
    public void testGetNumOfEnrolledFingerprintsSize() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 999);
        setupFingerprintEnrolledFingerprints(mFingerprintManager, 10, 3);
        setupFingerprintEnrolledFingerprints(mFingerprintManager, 22, 99);

        assertThat(repository.getNumOfEnrolledFingerprintsSize(10)).isEqualTo(3);
        assertThat(repository.getNumOfEnrolledFingerprintsSize(22)).isEqualTo(99);
    }

    @Test
    public void testGetMaxFingerprintsInSuw() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 999);
        setupSuwMaxFingerprintsEnrollable(mContext, mResources, 333);
        assertThat(repository.getMaxFingerprintsInSuw(mResources))
                .isEqualTo(333);

        setupSuwMaxFingerprintsEnrollable(mContext, mResources, 20);
        assertThat(repository.getMaxFingerprintsInSuw(mResources)).isEqualTo(20);
    }

}
