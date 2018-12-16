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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FingerprintEnrollSuggestionActivityTest {

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(eq(Context.DEVICE_POLICY_SERVICE)))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(), anyInt())).thenReturn(0);
        when(mContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintAdded() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        assertThat(FingerprintEnrollSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsNotCompleteWhenNoFingerprintAdded() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        assertThat(FingerprintEnrollSuggestionActivity.isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenHardwareNotDetected() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        assertThat(FingerprintEnrollSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintNotSupported() {
        stubFingerprintSupported(false);

        assertThat(FingerprintEnrollSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintDisabled() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(), anyInt()))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        assertThat(FingerprintEnrollSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    private void stubFingerprintSupported(boolean enabled) {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(enabled);
    }
}
