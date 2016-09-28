/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import static android.content.pm.PackageManager.FEATURE_FINGERPRINT;
import static com.android.settings.ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS;
import static com.android.settings.ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY;
import static com.android.settings.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE;
import static com.android.settings.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
import static com.android.settings.ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link SetNewPasswordController}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class SetNewPasswordControllerTest {
    private static final int CURRENT_UID = 101;
    private static final long FINGERPRINT_CHALLENGE = -9876512313131L;

    @Mock PackageManager mPackageManager;
    @Mock FingerprintManager mFingerprintManager;
    @Mock DevicePolicyManager mDevicePolicyManager;

    @Mock private SetNewPasswordController.Ui mUi;
    private SetNewPasswordController mSetNewPasswordController;

    @Before
    public void setUp()  {
        MockitoAnnotations.initMocks(this);

        mSetNewPasswordController = new SetNewPasswordController(
                CURRENT_UID, mPackageManager, mFingerprintManager, mDevicePolicyManager, mUi);

        when(mFingerprintManager.preEnroll()).thenReturn(FINGERPRINT_CHALLENGE);
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FINGERPRINT))).thenReturn(true);
    }

    @Test
    public void launchChooseLockWithFingerprint() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        // GIVEN DPC does not disallow fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched with fingerprint extras.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        // THEN the extras have all values for fingerprint setup.
        compareFingerprintExtras(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFingerprint_noFingerprintFeature() {
        // GIVEN the device does NOT support fingerprint feature.
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FINGERPRINT))).thenReturn(false);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without fingerprint extras.
        verify(mUi).launchChooseLock(null);
    }

    @Test
    public void launchChooseLockWithoutFingerprint_noFingerprintSensor() {
        // GIVEN the device does NOT support fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        // GIVEN DPC does not disallow fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without fingerprint extras.
        verify(mUi).launchChooseLock(null);
    }

    @Test
    public void launchChooseLockWithoutFingerprint_hasFingerprintEnrolled() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        // GIVEN DPC does not disallow fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without fingerprint extras.
        verify(mUi).launchChooseLock(null);
    }

    @Test
    public void launchChooseLockWithoutFingerprint_deviceAdminDisallowFingerprintForKeyguard() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there is an enrolled fingerprint.
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        // GIVEN DPC disallows fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without fingerprint extras.
        verify(mUi).launchChooseLock(null);
    }

    private void compareFingerprintExtras(Bundle actualBundle) {
        assertEquals(
                "Password quality must be something in order to config fingerprint.",
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
                actualBundle.getInt(MINIMUM_QUALITY_KEY));
        assertTrue(
                "All disabled preference should be removed.",
                actualBundle.getBoolean(HIDE_DISABLED_PREFS));
        assertTrue(
                "There must be a fingerprint challenge.",
                actualBundle.getBoolean(EXTRA_KEY_HAS_CHALLENGE));
        assertEquals(
                "The fingerprint challenge must come from the FingerprintManager",
                FINGERPRINT_CHALLENGE,
                actualBundle.getLong(EXTRA_KEY_CHALLENGE));
        assertTrue(
                "The request must be a fingerprint set up request.",
                actualBundle.getBoolean(EXTRA_KEY_FOR_FINGERPRINT));
        assertEquals(
                "User id must be equaled to the input one.",
                CURRENT_UID,
                actualBundle.getInt(Intent.EXTRA_USER_ID));
    }
}
