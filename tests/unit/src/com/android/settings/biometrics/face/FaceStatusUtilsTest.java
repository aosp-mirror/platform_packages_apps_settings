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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class FaceStatusUtilsTest {

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
    private FaceStatusUtils mFaceStatusUtils;

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
        mFaceStatusUtils = new FaceStatusUtils(mApplicationContext, mFaceManager, USER_ID);
    }

    @Test
    public void isAvailable_withoutFingerprint_withoutFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mFaceStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_withoutFingerprint_withFace_returnsTrue() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mFaceStatusUtils.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withFingerprint_withoutFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        assertThat(mFaceStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_withFingerprint_withFace_returnsFalse() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        assertThat(mFaceStatusUtils.isAvailable()).isFalse();
    }

    @Test
    public void hasEnrolled_withEnrolledTemplates_returnsTrue() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        assertThat(mFaceStatusUtils.hasEnrolled()).isTrue();
    }

    @Test
    public void hasEnrolled_withoutEnrolledTemplates_returnsFalse() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mFaceStatusUtils.hasEnrolled()).isFalse();
    }

    @Test
    public void getDisabledAdmin_whenFaceDisabled_returnsEnforcedAdmin() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FACE);

        final RestrictedLockUtils.EnforcedAdmin admin = mFaceStatusUtils.getDisablingAdmin();

        assertThat(admin).isEqualTo(new RestrictedLockUtils.EnforcedAdmin(
                COMPONENT_NAME, UserManager.DISALLOW_BIOMETRIC, USER_HANDLE));
    }

    @Test
    public void getDisabledAdmin_withFaceEnabled_returnsNull() {
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        assertThat(mFaceStatusUtils.getDisablingAdmin()).isNull();
    }

    @Test
    public void getSummary_whenNotEnrolled_returnsSummaryNone() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mFaceStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_face_preference_summary_none"));
    }

    @Test
    public void getSummary_whenEnrolled_returnsSummary() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        assertThat(mFaceStatusUtils.getSummary())
                .isEqualTo(ResourcesUtils.getResourcesString(
                        mApplicationContext,
                        "security_settings_face_preference_summary"));
    }

    @Test
    public void getSettingsClassName_whenNotEnrolled_returnsFaceEnrollInduction() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(false);

        assertThat(mFaceStatusUtils.getSettingsClassName())
                .isEqualTo(FaceEnrollIntroductionInternal.class.getName());
    }

    @Test
    public void getSettingsClassName_whenEnrolled_returnsFaceSettings() {
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);

        assertThat(mFaceStatusUtils.getSettingsClassName())
                .isEqualTo(Settings.FaceSettingsInternalActivity.class.getName());
    }
}
