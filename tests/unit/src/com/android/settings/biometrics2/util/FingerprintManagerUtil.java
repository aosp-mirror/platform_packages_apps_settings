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

package com.android.settings.biometrics2.util;

import static org.mockito.Mockito.when;

import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class FingerprintManagerUtil {

    public static void setupFingerprintFirstSensor(
            @NonNull FingerprintManager mockedFingerprintManager,
            @FingerprintSensorProperties.SensorType int sensorType,
            int maxEnrollmentsPerUser) {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                maxEnrollmentsPerUser,
                new ArrayList<>() /* componentInfo */,
                sensorType,
                true /* resetLockoutRequiresHardwareAuthToken */));
        when(mockedFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);
    }

    public static void setupFingerprintEnrolledFingerprints(
            @NonNull FingerprintManager mockedFingerprintManager,
            int userId,
            int enrolledFingerprints) {
        final ArrayList<Fingerprint> ret = new ArrayList<>();
        for (int i = 0; i < enrolledFingerprints; ++i) {
            ret.add(new Fingerprint("name", 0, 0, 0L));
        }
        when(mockedFingerprintManager.getEnrolledFingerprints(userId)).thenReturn(ret);
    }
}
