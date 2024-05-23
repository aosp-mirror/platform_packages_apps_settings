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

package com.android.settings.biometrics.combination;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FACE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.utils.StringUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CombinedBiometricStatusUtilsTest {

    private static final ComponentName COMPONENT_NAME =
            new ComponentName("package", "class");
    private static final int USER_ID = UserHandle.myUserId();
    private static final UserHandle USER_HANDLE = new UserHandle(USER_ID);

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;

    private Context mApplicationContext;
    private CombinedBiometricStatusUtils mCombinedBiometricStatusUtils;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

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
        mCombinedBiometricStatusUtils = new CombinedBiometricStatusUtils(
                mApplicationContext, USER_ID);
    }

    @Test
    public void isAvailable_withoutFingerprint_withoutFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_withoutFingerprint_whenFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mCombinedBiometricStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenFingerprint_withoutFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenFingerprint_whenFace_returnsTrue() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mCombinedBiometricStatusUtils.isAvailable()).isTrue();
    }

    @Test
    public void hasEnrolled_withoutFingerprintHardware_withoutFaceHardware_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.hasEnrolled()).isFalse();
    }

    @Test
    public void hasEnrolled_withoutFingerprintEnroll_withoutFaceEnroll_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.hasEnrolled()).isFalse();
    }

    @Test
    public void hasEnrolled_withoutFingerprintEnroll_withFaceEnroll_returnsTrue() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        assertThat(mCombinedBiometricStatusUtils.hasEnrolled()).isTrue();
    }

    @Test
    public void hasEnrolled_withFingerprintEnroll_withoutFaceEnroll_returnsTrue() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.hasEnrolled()).isTrue();
    }

    @Test
    public void hasEnrolled_withFingerprintEnroll_withFaceEnroll_returnsTrue() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        assertThat(mCombinedBiometricStatusUtils.hasEnrolled()).isTrue();
    }

    @Test
    public void getDisabledAdmin_whenFingerprintDisabled_whenFaceDisabled_returnsEnforcedAdmin() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(KEYGUARD_DISABLE_FACE | KEYGUARD_DISABLE_FINGERPRINT);

        final RestrictedLockUtils.EnforcedAdmin admin =
                mCombinedBiometricStatusUtils.getDisablingAdmin();

        assertThat(admin).isEqualTo(new RestrictedLockUtils.EnforcedAdmin(
                COMPONENT_NAME, UserManager.DISALLOW_BIOMETRIC, USER_HANDLE));
    }

    @Test
    public void getDisabledAdmin_whenFingerprintDisabled_whenFaceEnabled_returnsNull() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(KEYGUARD_DISABLE_FINGERPRINT);

        assertThat(mCombinedBiometricStatusUtils.getDisablingAdmin()).isNull();
    }

    @Test
    public void getDisabledAdmin_whenFingerprintEnabled_whenFaceDisabled_returnsNull() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(KEYGUARD_DISABLE_FACE);

        assertThat(mCombinedBiometricStatusUtils.getDisablingAdmin()).isNull();
    }

    @Test
    public void getDisabledAdmin_whenFingerprintEnabled_whenFaceEnabled_returnsNull() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        assertThat(mCombinedBiometricStatusUtils.getDisablingAdmin()).isNull();
    }

    @Test
    public void getSummary_whenFaceEnrolled_whenMultipleFingerprints_returnsBothFpMultiple() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(2));

        assertThat(mCombinedBiometricStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_biometric_preference_summary_both_fp_multiple"));
    }

    @Test
    public void getSummary_whenFaceEnrolled_whenSingleFingerprint_returnsBothFpSingle() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(1));

        assertThat(mCombinedBiometricStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_biometric_preference_summary_both_fp_single"));
    }

    @Test
    public void getSummary_whenFaceEnrolled_whenNoFingerprints_returnsFace() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(0));

        assertThat(mCombinedBiometricStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_face_preference_summary"));
    }

    @Test
    public void getSummary_whenNoFaceEnrolled_whenMultipleFingerprints_returnsFingerprints() {
        final int enrolledFingerprintsCount = 2;
        final int stringResId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "string",
                "security_settings_fingerprint_preference_summary");
        final String summary = StringUtil.getIcuPluralsString(mApplicationContext,
                enrolledFingerprintsCount, stringResId);

        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        assertThat(mCombinedBiometricStatusUtils.getSummary()).isEqualTo(summary);
    }

    @Test
    public void getSummary_whenNoFaceEnrolled_whenSingleFingerprints_returnsFingerprints() {
        final int enrolledFingerprintsCount = 1;
        final int stringResId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "string",
                "security_settings_fingerprint_preference_summary");
        final String summary = StringUtil.getIcuPluralsString(mApplicationContext,
                enrolledFingerprintsCount, stringResId);

        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(enrolledFingerprintsCount));

        assertThat(mCombinedBiometricStatusUtils.getSummary()).isEqualTo(summary);
    }

    @Test
    public void getSummary_whenNoFaceEnrolled_whenNoFingerprints_returnsNoneEnrolled() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(createFingerprintList(0));

        assertThat(mCombinedBiometricStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_biometric_preference_summary_none_enrolled"));
    }

    @Test
    public void getSettingsClassName_returnsCombinedBiometricSettings() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.getSettingsClassName())
                .isEqualTo(Settings.CombinedBiometricSettingsActivity.class.getName());
    }

    @Test
    public void getProfileSettingsClassName_returnsCombinedBiometricProfileSettings() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mCombinedBiometricStatusUtils.getProfileSettingsClassName())
                .isEqualTo(Settings.CombinedBiometricProfileSettingsActivity.class.getName());
    }

    @Test
    public void getPrivateProfileSettingsClassName_returnsPrivateSpaceBiometricSettings() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        assertThat(mCombinedBiometricStatusUtils.getPrivateProfileSettingsClassName())
                .isEqualTo(Settings.PrivateSpaceBiometricSettingsActivity.class.getName());
    }

    private List<Fingerprint> createFingerprintList(int size) {
        final List<Fingerprint> fingerprintList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fingerprintList.add(new Fingerprint("fingerprint" + i, 0, 0));
        }
        return fingerprintList;
    }
}
