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

package com.android.settings.biometrics2.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;

import androidx.annotation.NonNull;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;

import java.util.ArrayList;

public class FingerprintRepositoryUtils {

    public static void setupSuwMaxFingerprintsEnrollable(
            @NonNull Context context,
            @NonNull Resources mockedResources,
            int numOfFp) {
        final int resId = context.getResources().getIdentifier("suw_max_fingerprints_enrollable",
                "integer", context.getPackageName());
        when(mockedResources.getInteger(resId)).thenReturn(numOfFp);
    }

    public static FingerprintRepository newFingerprintRepository(
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
        doAnswer(invocation -> {
            final IFingerprintAuthenticatorsRegisteredCallback callback =
                    invocation.getArgument(0);
            callback.onAllAuthenticatorsRegistered(props);
            return null;
        }).when(mockedFingerprintManager).addAuthenticatorsRegisteredCallback(any());
        return new FingerprintRepository(mockedFingerprintManager);
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
