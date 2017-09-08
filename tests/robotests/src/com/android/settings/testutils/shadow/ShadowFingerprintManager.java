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

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.support.annotation.NonNull;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Implements(FingerprintManager.class)
public class ShadowFingerprintManager {

    private static Map<String, String> getSystemServiceMap() {
        return ReflectionHelpers.getStaticField(ShadowContextImpl.class, "SYSTEM_SERVICE_MAP");
    }

    /**
     * Call this in @Before of a test to add FingerprintManager to Robolectric's system service
     * map. Otherwise getSystemService(FINGERPRINT_SERVICE) will return null.
     */
    public static void addToServiceMap() {
        getSystemServiceMap().put(Context.FINGERPRINT_SERVICE, FingerprintManager.class.getName());
    }

    @Resetter
    public static void reset() {
        getSystemServiceMap().remove(Context.FINGERPRINT_SERVICE);
    }

    public boolean hardwareDetected = true;

    @NonNull
    private List<Fingerprint> mFingerprints = Collections.emptyList();

    @Implementation
    public boolean isHardwareDetected() {
        return hardwareDetected;
    }

    @Implementation
    public boolean hasEnrolledFingerprints() {
        return !mFingerprints.isEmpty();
    }

    @Implementation
    public List<Fingerprint> getEnrolledFingerprints() {
        return mFingerprints;
    }

    @Implementation
    public List<Fingerprint> getEnrolledFingerprints(int userId) {
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
        return (ShadowFingerprintManager) ShadowExtractor.extract(
                RuntimeEnvironment.application.getSystemService(FingerprintManager.class));
    }
}
