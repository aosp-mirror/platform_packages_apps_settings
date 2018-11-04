/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Implements(FingerprintManager.class)
public class ShadowFingerprintManager extends org.robolectric.shadows.ShadowFingerprintManager {


    public boolean hardwareDetected = true;

    @NonNull
    private List<Fingerprint> mFingerprints = Collections.emptyList();

    @Implementation
    protected boolean isHardwareDetected() {
        return hardwareDetected;
    }

    @Implementation
    protected boolean hasEnrolledFingerprints() {
        return !mFingerprints.isEmpty();
    }

    @Implementation
    protected List<Fingerprint> getEnrolledFingerprints() {
        return mFingerprints;
    }

    @Implementation
    protected List<Fingerprint> getEnrolledFingerprints(int userId) {
        return mFingerprints;
    }

    public void setEnrolledFingerprints(Fingerprint... fingerprints) {
        mFingerprints = Arrays.asList(fingerprints);
    }

    public void setDefaultFingerprints(int num) {
        setEnrolledFingerprints(
                IntStream.range(0, num)
                        .mapToObj(i -> new Fingerprint(
                                "Fingerprint " + i,
                                0, /* groupId */
                                i, /* fingerId */
                                0 /* deviceId */))
                        .toArray(Fingerprint[]::new));
    }

    public static ShadowFingerprintManager get() {
        return (ShadowFingerprintManager) Shadow.extract(
                RuntimeEnvironment.application.getSystemService(FingerprintManager.class));
    }
}
