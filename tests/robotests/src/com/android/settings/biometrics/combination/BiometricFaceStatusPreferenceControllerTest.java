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

package com.android.settings.biometrics.combination;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import com.android.settings.testutils.ActiveUnlockTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.RestrictedPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class BiometricFaceStatusPreferenceControllerTest {

    @Rule public final MockitoRule mMocks = MockitoJUnit.rule();

    @Mock private UserManager mUserManager;
    @Mock private PackageManager mPackageManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private FaceManager mFaceManager;

    private Context mContext;
    private RestrictedPreference mPreference;
    private BiometricFaceStatusPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        ShadowApplication.getInstance()
                .setSystemService(Context.FINGERPRINT_SERVICE, mFingerprintManager);
        ShadowApplication.getInstance().setSystemService(Context.FACE_SERVICE, mFaceManager);
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUserManager);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {1234});
        mPreference = new RestrictedPreference(mContext);
        mController = new BiometricFaceStatusPreferenceController(mContext, "preferenceKey");
    }

    @After
    public void tearDown() {
        ActiveUnlockTestUtils.disable(mContext);
    }

    @Test
    public void onlyFaceEnabled_preferenceNotVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onlyFaceAndActiveUnlockEnabled_preferenceVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        ActiveUnlockTestUtils.enable(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void faceAndFingerprintEnabled_preferenceVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }
}
