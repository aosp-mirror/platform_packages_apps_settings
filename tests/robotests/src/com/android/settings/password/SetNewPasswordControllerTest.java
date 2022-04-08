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

import static android.content.pm.PackageManager.FEATURE_FACE;
import static android.content.pm.PackageManager.FEATURE_FINGERPRINT;

import static com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        .HIDE_DISABLED_PREFS;
import static com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        .MINIMUM_QUALITY_KEY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SetNewPasswordControllerTest {

    private static final int CURRENT_USER_ID = 101;
    private static final long FINGERPRINT_CHALLENGE = -9876512313131L;
    private static final long FACE_CHALLENGE = 1352057789L;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private SetNewPasswordController.Ui mUi;

    private SetNewPasswordController mSetNewPasswordController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSetNewPasswordController = new SetNewPasswordController(
                CURRENT_USER_ID, mPackageManager, mFingerprintManager, mFaceManager,
                mDevicePolicyManager, mUi);

        when(mFingerprintManager.preEnroll()).thenReturn(FINGERPRINT_CHALLENGE);
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FINGERPRINT))).thenReturn(true);

        when(mFaceManager.generateChallenge()).thenReturn(FACE_CHALLENGE);
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FACE))).thenReturn(true);
    }

    @Test
    public void launchChooseLockWithFingerprint() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints(CURRENT_USER_ID)).thenReturn(false);
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
    public void launchChooseLockWithFace() {
        // GIVEN the device supports face.
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled face.
        when(mFaceManager.hasEnrolledTemplates(CURRENT_USER_ID)).thenReturn(false);
        // GIVEN DPC does not disallow face for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched with face extras.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        // THEN the extras have all values for face setup.
        compareFaceExtras(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFingerprint_noFingerprintFeature() {
        // GIVEN the device does NOT support fingerprint feature.
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FINGERPRINT))).thenReturn(false);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without fingerprint extras.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFace_no_FaceFeature() {
        // GIVEN the device does NOT support face feature.
        when(mPackageManager.hasSystemFeature(eq(FEATURE_FACE))).thenReturn(false);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without face extras.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFingerprint_noFingerprintSensor() {
        // GIVEN the device does NOT support fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints(CURRENT_USER_ID)).thenReturn(false);
        // GIVEN DPC does not disallow fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFace_noFaceSensor() {
        // GIVEN the device does NOT support face.
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        // GIVEN there are no enrolled face.
        when(mFaceManager.hasEnrolledTemplates(CURRENT_USER_ID)).thenReturn(false);
        // GIVEN DPC does not disallow face for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFingerprint_hasFingerprintEnrolled() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled fingerprints.
        when(mFingerprintManager.hasEnrolledFingerprints(CURRENT_USER_ID)).thenReturn(true);
        // GIVEN DPC does not disallow fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFace_hasFaceEnrolled() {
        // GIVEN the device supports face.
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there are no enrolled face.
        when(mFaceManager.hasEnrolledTemplates(CURRENT_USER_ID)).thenReturn(true);
        // GIVEN DPC does not disallow face for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(0);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFingerprint_deviceAdminDisallowFingerprintForKeyguard() {
        // GIVEN the device supports fingerprint.
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there is an enrolled fingerprint.
        when(mFingerprintManager.hasEnrolledFingerprints(CURRENT_USER_ID)).thenReturn(true);
        // GIVEN DPC disallows fingerprint for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
    }

    @Test
    public void launchChooseLockWithoutFace_deviceAdminDisallowFaceForKeyguard() {
        // GIVEN the device supports face.
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        // GIVEN there is an enrolled face.
        when(mFaceManager.hasEnrolledTemplates(CURRENT_USER_ID)).thenReturn(true);
        // GIVEN DPC disallows face for keyguard usage.
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(ComponentName.class)))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        // WHEN the controller dispatches a set new password intent.
        mSetNewPasswordController.dispatchSetNewPasswordIntent();

        // THEN the choose lock activity is launched without a bundle contains user id only.
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mUi).launchChooseLock(bundleArgumentCaptor.capture());
        assertBundleContainsUserIdOnly(bundleArgumentCaptor.getValue());
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
                CURRENT_USER_ID,
                actualBundle.getInt(Intent.EXTRA_USER_ID));
    }

    private void compareFaceExtras(Bundle actualBundle) {
        assertEquals(
                "Password quality must be something in order to config face.",
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
                actualBundle.getInt(MINIMUM_QUALITY_KEY));
        assertTrue(
                "All disabled preference should be removed.",
                actualBundle.getBoolean(HIDE_DISABLED_PREFS));
        assertTrue(
                "There must be a face challenge.",
                actualBundle.getBoolean(EXTRA_KEY_HAS_CHALLENGE));
        assertEquals(
                "The face challenge must come from the FaceManager",
                FACE_CHALLENGE,
                actualBundle.getLong(EXTRA_KEY_CHALLENGE));
        assertTrue(
                "The request must be a face set up request.",
                actualBundle.getBoolean(EXTRA_KEY_FOR_FACE));
        assertEquals(
                "User id must be equaled to the input one.",
                CURRENT_USER_ID,
                actualBundle.getInt(Intent.EXTRA_USER_ID));
    }

    private void assertBundleContainsUserIdOnly(Bundle actualBundle) {
        assertThat(actualBundle.size()).isEqualTo(1);
        assertThat(actualBundle.getInt(Intent.EXTRA_USER_ID)).isEqualTo(CURRENT_USER_ID);
    }
}
