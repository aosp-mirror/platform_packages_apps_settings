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

package com.android.settings.biometrics.activeunlock;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.ActiveUnlockTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class ActiveUnlockStatusUtilsTest {

    @Rule public final MockitoRule mMocks = MockitoJUnit.rule();

    @Mock private PackageManager mPackageManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private FaceManager mFaceManager;

    private Context mApplicationContext;
    private ActiveUnlockStatusUtils mActiveUnlockStatusUtils;

    @Before
    public void setUp() {
        mApplicationContext = spy(ApplicationProvider.getApplicationContext());
        when(mApplicationContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mApplicationContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
        when(mApplicationContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        ActiveUnlockTestUtils.enable(mApplicationContext);
        mActiveUnlockStatusUtils = new ActiveUnlockStatusUtils(mApplicationContext);
    }

    @After
    public void tearDown() {
        ActiveUnlockTestUtils.disable(mApplicationContext);
    }

    @Test
    public void isAvailable_featureFlagDisabled_returnsConditionallyUnavailable() {
        ActiveUnlockTestUtils.disable(mApplicationContext);

        assertThat(mActiveUnlockStatusUtils.getAvailability()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isAvailable_withoutFingerprint_withoutFace_returnsUnsupported() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getAvailability()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isAvailable_withoutFingerprint_withFace_returnsAvailable() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getAvailability()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isAvailable_withFingerprint_withoutFace_returnsAvailable() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getAvailability()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isAvailable_withFingerprint_withFace_returnsAvailable() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getAvailability()).isEqualTo(AVAILABLE);
    }

    @Test
    public void configIsUnlockOnIntent_useUnlockIntentLayoutIsTrue() {
        ActiveUnlockTestUtils.enable(
                mApplicationContext, ActiveUnlockStatusUtils.UNLOCK_INTENT_LAYOUT);

        assertThat(mActiveUnlockStatusUtils.useUnlockIntentLayout()).isTrue();
        assertThat(mActiveUnlockStatusUtils.useBiometricFailureLayout()).isFalse();
    }

    @Test
    public void getTitle_faceEnabled_returnsFacePreferenceTitle() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.security_settings_face_preference_title));
    }

    @Test
    public void getTitle_fingerprintEnabled_returnsFingerprintPreferenceTitle() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.security_settings_fingerprint_preference_title));
    }

    @Test
    public void getIntro_faceEnabled_returnsFace() {
        ActiveUnlockTestUtils.enable(
                mApplicationContext, ActiveUnlockStatusUtils.UNLOCK_INTENT_LAYOUT);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getIntroForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_intro_with_face));
    }

    @Test
    public void getIntro_fingerprintEnabled_returnsFingerprint() {
        ActiveUnlockTestUtils.enable(
                mApplicationContext, ActiveUnlockStatusUtils.UNLOCK_INTENT_LAYOUT);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getIntroForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_intro_with_fingerprint));
    }

    @Test
    public void getIntro_faceAndFingerprintEnabled_returnsFaceAndFingerprint() {
        ActiveUnlockTestUtils.enable(
                mApplicationContext, ActiveUnlockStatusUtils.UNLOCK_INTENT_LAYOUT);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getIntroForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_intro_with_activeunlock));
    }

    @Test
    public void getUnlockDeviceSummary_fingerprintEnabled_returnsFingerprintOrWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getUnlockDeviceSummaryForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_fingerprint_or_watch_preference_summary));
    }

    @Test
    public void getUnlockDeviceSummary_faceEnabled_returnsFaceOrWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getUnlockDeviceSummaryForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_face_or_watch_preference_summary));
    }

    @Test
    public void getUseBiometricTitle_faceAndFingerprintEnabled_returnsFaceFingerprintOrWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getUseBiometricTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_face_fingerprint_or_watch_for));
    }

    @Test
    public void getUseBiometricTitle_fingerprintEnabled_returnsFingerprintOrWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getUseBiometricTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_fingerprint_or_watch_for));
    }

    @Test
    public void getUseBiometricTitle_faceEnabled_returnsFaceOrWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mActiveUnlockStatusUtils.getUseBiometricTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_face_or_watch_for));
    }

    @Test
    public void getUseBiometricTitle_withoutFaceOrFingerprint_returnsWatch() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mActiveUnlockStatusUtils.getUseBiometricTitleForActiveUnlock())
                .isEqualTo(mApplicationContext.getString(
                        R.string.biometric_settings_use_watch_for));
    }
}
