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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;

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

import java.util.ArrayList;

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

    @Test
    public void testGetFirstFingerprintSensorPropertiesInternal() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop = new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                5,
                new ArrayList<>() /* componentInfo */,
                TYPE_UDFPS_OPTICAL,
                true /* resetLockoutRequiresHardwareAuthToken */);
        props.add(prop);
        doAnswer(invocation -> {
            final IFingerprintAuthenticatorsRegisteredCallback callback =
                    invocation.getArgument(0);
            callback.onAllAuthenticatorsRegistered(props);
            return null;
        }).when(mFingerprintManager).addAuthenticatorsRegisteredCallback(any());

        final FingerprintRepository repository = new FingerprintRepository(mFingerprintManager);
        assertThat(repository.getFirstFingerprintSensorPropertiesInternal()).isEqualTo(prop);
    }

    @Test
    public void testGetEnrollStageCount() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 999);

        final int expectedValue = 24;
        doReturn(expectedValue).when(mFingerprintManager).getEnrollStageCount();

        assertThat(repository.getEnrollStageCount()).isEqualTo(expectedValue);
    }

    @Test
    public void testGetEnrollStageThreshold() {
        final FingerprintRepository repository = newFingerprintRepository(mFingerprintManager,
                TYPE_UNKNOWN, 999);

        final float expectedValue0 = 0.42f;
        final float expectedValue1 = 0.24f;
        final float expectedValue2 = 0.33f;
        final float expectedValue3 = 0.90f;
        doReturn(expectedValue0).when(mFingerprintManager).getEnrollStageThreshold(0);
        doReturn(expectedValue1).when(mFingerprintManager).getEnrollStageThreshold(1);
        doReturn(expectedValue2).when(mFingerprintManager).getEnrollStageThreshold(2);
        doReturn(expectedValue3).when(mFingerprintManager).getEnrollStageThreshold(3);

        assertThat(repository.getEnrollStageThreshold(2)).isEqualTo(expectedValue2);
        assertThat(repository.getEnrollStageThreshold(1)).isEqualTo(expectedValue1);
        assertThat(repository.getEnrollStageThreshold(3)).isEqualTo(expectedValue3);
        assertThat(repository.getEnrollStageThreshold(0)).isEqualTo(expectedValue0);
    }
}
