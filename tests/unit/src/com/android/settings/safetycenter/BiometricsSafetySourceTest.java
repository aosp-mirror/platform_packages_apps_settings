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
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.biometrics.face.FaceEnrollIntroductionInternal;
import com.android.settings.biometrics.fingerprint.FingerprintSettings;
import com.android.settings.testutils.ActiveUnlockTestUtils;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.utils.StringUtil;

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

    private static final ComponentName COMPONENT_NAME = new ComponentName("package", "class");
    private static final UserHandle USER_HANDLE = new UserHandle(UserHandle.myUserId());
    private static final SafetyEvent EVENT_SOURCE_STATE_CHANGED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build();

    private Context mApplicationContext;

    @Mock private PackageManager mPackageManager;
    @Mock private DevicePolicyManager mDevicePolicyManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private FaceManager mFaceManager;
    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

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
    public void setSafetyData_whenSafetyCenterIsDisabled_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void setSafetySourceData_whenSafetyCenterIsEnabled_withoutBiometrics_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(BiometricsSafetySource.SAFETY_SOURCE_ID), eq(null), any());
    }

    @Test
    public void setSafetySourceData_setsDataForBiometricSource() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(BiometricsSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    public void setSafetySourceData_setsDataWithCorrectSafetyEvent() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), any(), eq(EVENT_SOURCE_STATE_CHANGED));
    }

    @Test
    public void setSafetySourceData_withFingerprintNotEnrolled_whenDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSetWithSingularSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary_none");
    }

    @Test
    public void setSafetySourceData_withFingerprintNotEnrolled_whenNotDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary_none",
                FingerprintSettings.class.getName());
    }

    @Test
    public void setSafetySourceData_withFingerprintsEnrolled_whenDisabledByAdmin_setsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSetWithPluralSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary",
                enrolledFingerprintsCount);
    }

    @Test
    public void setSafetySourceData_withFingerprintsEnrolled_whenNotDisabledByAdmin_setsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithPluralSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary",
                enrolledFingerprintsCount,
                FingerprintSettings.class.getName());
    }

    @Test
    public void setSafetySourceData_withFaceNotEnrolled_whenDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSetWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary_none");
    }

    @Test
    public void setSafetySourceData_withFaceNotEnrolled_whenNotDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary_none",
                FaceEnrollIntroductionInternal.class.getName());
    }

    @Test
    public void setSafetySourceData_withFaceEnrolled_whenDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSetWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary");
    }

    @Test
    public void setSafetySourceData_withFaceEnrolled_whenNotDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary",
                Settings.FaceSettingsInternalActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_withFaceAndFingerprint_whenBothNotDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_withFaceAndFingerprint_whenFaceDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFingerprintDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenBothDisabledByAdmin_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(
                        DevicePolicyManager.KEYGUARD_DISABLE_FACE
                                | DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_none_enrolled");
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFaceEnrolled_withMpFingers_setsData() {
        final int enrolledFingerprintsCount = 2;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_both_fp_multiple",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFaceEnrolled_withOneFinger_setsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_both_fp_single",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFaceEnrolled_withNoFingers_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(Collections.emptyList());

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_face_preference_summary",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_activeUnlockEnabled_withFingerprintOnly_setsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        ActiveUnlockTestUtils.enable(mApplicationContext);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithPluralSummary(
                "security_settings_fingerprint_preference_title",
                "security_settings_fingerprint_preference_summary",
                enrolledFingerprintsCount,
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_activeUnlockEnabled_withFaceOnly_setsData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        ActiveUnlockTestUtils.enable(mApplicationContext);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_face_preference_title",
                "security_settings_face_preference_summary",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_activeUnlockEnabled_withFaceAndFingerprint_setsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));
        ActiveUnlockTestUtils.enable(mApplicationContext);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithSingularSummary(
                "security_settings_biometric_preference_title",
                "security_settings_biometric_preference_summary_both_fp_single",
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenNoFaceEnrolled_withFingers_setsData() {
        final int enrolledFingerprintsCount = 1;
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSetWithPluralSummary(
                "security_settings_biometric_preference_title",
                "security_settings_fingerprint_preference_summary",
                enrolledFingerprintsCount,
                Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFaceEnrolled_setsInfoSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenFingerprintEnrolled_setsInfoSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_faceAndFingerprint_whenNotEnrolled_setsUnspSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_fingerprint_whenEnrolled_setsInfoSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_fingerprint_whenNotEnrolled_setsUnspSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_face_whenEnrolled_setsInfoSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_face_whenNotEnrolled_setsUnspSeverity() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        BiometricsSafetySource.setSafetySourceData(mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceStatus safetySourceStatus = captor.getValue().getStatus();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    private void assertSafetySourceDisabledDataSetWithSingularSummary(
            String expectedTitleResName, String expectedSummaryResName) {
        assertSafetySourceDisabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                ResourcesUtils.getResourcesString(mApplicationContext, expectedSummaryResName));
    }

    private void assertSafetySourceEnabledDataSetWithSingularSummary(
            String expectedTitleResName,
            String expectedSummaryResName,
            String expectedSettingsClassName) {
        assertSafetySourceEnabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                ResourcesUtils.getResourcesString(mApplicationContext, expectedSummaryResName),
                expectedSettingsClassName);
    }

    private void assertSafetySourceDisabledDataSetWithPluralSummary(
            String expectedTitleResName,
            String expectedSummaryResName,
            int expectedSummaryQuantity) {
        final int stringResId =
                ResourcesUtils.getResourcesId(
                        ApplicationProvider.getApplicationContext(),
                        "string",
                        expectedSummaryResName);
        assertSafetySourceDisabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                StringUtil.getIcuPluralsString(
                        mApplicationContext, expectedSummaryQuantity, stringResId));
    }

    private void assertSafetySourceEnabledDataSetWithPluralSummary(
            String expectedTitleResName,
            String expectedSummaryResName,
            int expectedSummaryQuantity,
            String expectedSettingsClassName) {
        final int stringResId =
                ResourcesUtils.getResourcesId(
                        ApplicationProvider.getApplicationContext(),
                        "string",
                        expectedSummaryResName);
        assertSafetySourceEnabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext, expectedTitleResName),
                StringUtil.getIcuPluralsString(
                        mApplicationContext, expectedSummaryQuantity, stringResId),
                expectedSettingsClassName);
    }

    private void assertSafetySourceDisabledDataSet(String expectedTitle, String expectedSummary) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isFalse();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);

        final Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
        assertThat(clickIntent.getAction()).isEqualTo(ACTION_SHOW_ADMIN_SUPPORT_DETAILS);
    }

    private void assertSafetySourceEnabledDataSet(
            String expectedTitle, String expectedSummary, String expectedSettingsClassName) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(BiometricsSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isTrue();
        final Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
        assertThat(clickIntent.getComponent().getPackageName()).isEqualTo("com.android.settings");
        assertThat(clickIntent.getComponent().getClassName()).isEqualTo(expectedSettingsClassName);
    }

    private List<Fingerprint> createFingerprintList(int size) {
        final List<Fingerprint> fingerprintList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fingerprintList.add(new Fingerprint("fingerprint" + i, 0, 0));
        }
        return fingerprintList;
    }
}
