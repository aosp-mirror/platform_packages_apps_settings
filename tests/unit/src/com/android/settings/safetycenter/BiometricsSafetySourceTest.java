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

package com.android.settings.safetycenter;

import static android.provider.Settings.ACTION_SHOW_ADMIN_SUPPORT_DETAILS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintSettings;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BiometricsSafetySourceTest {

    private static final ComponentName COMPONENT_NAME =
            new ComponentName("package", "class");
    private static final UserHandle USER_HANDLE = new UserHandle(UserHandle.myUserId());

    private Context mApplicationContext;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = spy(ApplicationProvider.getApplicationContext());
        when(mApplicationContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwnerOrDeviceOwnerSupervisionComponent(USER_HANDLE))
                .thenReturn(COMPONENT_NAME);
        when(mApplicationContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
        when(mApplicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mApplicationContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsDisabled_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_whenSafetyCenterIsEnabled_withoutBiometrics_sendsNoData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never()).sendSafetyCenterUpdate(any(), any());
    }

    @Test
    public void sendSafetyData_withFingerprintNotEnrolled_whenDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceDisabledDataSentWithSingularSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary_none");
    }

    @Test
    public void sendSafetyData_withFingerprintNotEnrolled_whenNotDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary_none",
                FingerprintEnrollIntroduction.class.getName());
    }

    @Test
    public void sendSafetyData_withFingerprintsEnrolled_whenDisabledByAdmin_sendsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceDisabledDataSentWithPluralSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary",
                enrolledFingerprintsCount);
    }

    @Test
    public void sendSafetyData_withFingerprintsEnrolled_whenNotDisabledByAdmin_sendsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithPluralSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary", enrolledFingerprintsCount,
                FingerprintSettings.class.getName());
    }

    @Test
    public void sendSafetyData_withFaceNotEnrolled_whenDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceDisabledDataSentWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary_none");
    }

    @Test
    public void sendSafetyData_withFaceNotEnrolled_whenNotDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary_none",
                FaceEnrollIntroduction.class.getName());
    }

    @Test
    public void sendSafetyData_withFaceEnrolled_whenDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceDisabledDataSentWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary");
    }

    @Test
    public void sendSafetyData_withFaceEnrolled_whenNotDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary",
                Settings.FaceSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenBothNotDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenFaceDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenFingerprintDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenBothDisabledByAdmin_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE
                        | DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceDisabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled");
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenFaceEnrolled_withMpFingers_sendsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
                createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_both_fp_multiple",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenFaceEnrolled_withOneFinger_sendsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
                createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_both_fp_single",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenFaceEnrolled_withNoFingers_sendsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
                Collections.emptyList());

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_face_preference_summary",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void sandSafetyData_withFaceAndFingerprint_whenNoFaceEnrolled_withFingers_sendsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
                createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.sendSafetyData(mApplicationContext);

        assertSafetySourceEnabledDataSentWithPluralSummary(
                "security_settings_biometric_preference_title",
                "security_settings_fingerprint_preference_summary", enrolledFingerprintsCount,
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    private void assertSafetySourceDisabledDataSentWithSingularSummary(String expectedTitleResName,
            String expectedSummaryResName) {
        assertSafetySourceDisabledDataSent(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                ResourcesUtils.getResourcesString(mApplicationContext, expectedSummaryResName)
        );
    }

    private void assertSafetySourceEnabledDataSentWithSingularSummary(String expectedTitleResName,
            String expectedSummaryResName,
            String expectedSettingsClassName) {
        assertSafetySourceEnabledDataSent(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                ResourcesUtils.getResourcesString(mApplicationContext, expectedSummaryResName),
                expectedSettingsClassName
        );
    }

    private void assertSafetySourceDisabledDataSentWithPluralSummary(String expectedTitleResName,
            String expectedSummaryResName, int expectedSummaryQuantity) {
        final int stringResId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "plurals",
                expectedSummaryResName);
        assertSafetySourceDisabledDataSent(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                mApplicationContext.getResources().getQuantityString(stringResId,
                        expectedSummaryQuantity /* quantity */,
                        expectedSummaryQuantity /* formatArgs */)
        );
    }

    private void assertSafetySourceEnabledDataSentWithPluralSummary(String expectedTitleResName,
            String expectedSummaryResName, int expectedSummaryQuantity,
            String expectedSettingsClassName) {
        final int stringResId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "plurals",
                expectedSummaryResName);
        assertSafetySourceEnabledDataSent(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                mApplicationContext.getResources().getQuantityString(stringResId,
                        expectedSummaryQuantity /* quantity */,
                        expectedSummaryQuantity /* formatArgs */),
                expectedSettingsClassName
        );
    }

    private void assertSafetySourceDisabledDataSent(String expectedTitle, String expectedSummary) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceData.getId()).isEqualTo(BiometricsSafetySource.SAFETY_SOURCE_ID);
        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isFalse();
        final Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
        assertThat(clickIntent.getAction()).isEqualTo(ACTION_SHOW_ADMIN_SUPPORT_DETAILS);
    }

    private void assertSafetySourceEnabledDataSent(String expectedTitle, String expectedSummary,
            String expectedSettingsClassName) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper).sendSafetyCenterUpdate(any(), captor.capture());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceData.getId()).isEqualTo(BiometricsSafetySource.SAFETY_SOURCE_ID);
        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isTrue();
        final Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
        assertThat(clickIntent.getComponent().getPackageName())
                .isEqualTo("com.android.settings");
        assertThat(clickIntent.getComponent().getClassName())
                .isEqualTo(expectedSettingsClassName);
    }


    private List<Fingerprint> createFingerprintList(int size) {
        final List<Fingerprint> fingerprintList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fingerprintList.add(new Fingerprint("fingerprint" + i, 0, 0));
        }
        return fingerprintList;
    }
}
