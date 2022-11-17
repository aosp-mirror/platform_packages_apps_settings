/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.data.repository;

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_HOME_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_POWER_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UNKNOWN;

import static com.android.settings.biometrics2.util.FingerprintManagerUtil.setupFingerprintEnrolledFingerprints;
import static com.android.settings.biometrics2.util.FingerprintManagerUtil.setupFingerprintFirstSensor;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

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
    private FingerprintRepository mFingerprintRepository;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFingerprintRepository = new FingerprintRepository(mFingerprintManager);
    }

    @Test
    public void testCanAssumeSensorType() {
        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UNKNOWN, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isFalse();

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_REAR, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isFalse();

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UDFPS_ULTRASONIC, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isTrue();

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UDFPS_OPTICAL, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isTrue();

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_POWER_BUTTON, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isFalse();

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_HOME_BUTTON, 1);
        assertThat(mFingerprintRepository.canAssumeUdfps()).isFalse();
    }

    @Test
    public void testGetMaxFingerprints() {
        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UNKNOWN, 44);
        assertThat(mFingerprintRepository.getMaxFingerprints()).isEqualTo(44);

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UNKNOWN, 999);
        assertThat(mFingerprintRepository.getMaxFingerprints()).isEqualTo(999);
    }

    @Test
    public void testGetNumOfEnrolledFingerprintsSize() {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, 10, 3);
        setupFingerprintEnrolledFingerprints(mFingerprintManager, 22, 99);

        assertThat(mFingerprintRepository.getNumOfEnrolledFingerprintsSize(10)).isEqualTo(3);
        assertThat(mFingerprintRepository.getNumOfEnrolledFingerprintsSize(22)).isEqualTo(99);
    }

    @Test
    public void testGetMaxFingerprintsInSuw() {
        setupSuwMaxFingerprintsEnrollable(mContext, mResources, 333);
        assertThat(mFingerprintRepository.getMaxFingerprintsInSuw(mResources))
                .isEqualTo(333);

        setupSuwMaxFingerprintsEnrollable(mContext, mResources, 20);
        assertThat(mFingerprintRepository.getMaxFingerprintsInSuw(mResources)).isEqualTo(20);
    }

    public static void setupSuwMaxFingerprintsEnrollable(
            @NonNull Context context,
            @NonNull Resources mockedResources,
            int numOfFp) {
        final int resId = ResourcesUtils.getResourcesId(context, "integer",
                "suw_max_fingerprints_enrollable");
        when(mockedResources.getInteger(resId)).thenReturn(numOfFp);
    }
}
