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

package com.android.settings.biometrics;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.RemoteException;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUtils.class,
        ShadowUserManager.class,
        ShadowRestrictedLockUtilsInternal.class,
        ShadowSensorPrivacyManager.class,
        ShadowLockPatternUtils.class
})
public class MultiBiometricEnrollHelperTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;

    private Context mContext;

    @Captor
    private ArgumentCaptor<FingerprintManager.GenerateChallengeCallback> mFingerprintCaptor;

    private final int mUserId = 10;
    private final long mChallenge = 0L;
    private final int mSensorId = 0;
    private final long mGkPwHandle = 0L;

    private MultiBiometricEnrollHelper mMultiBiometricEnrollHelper;
    private Intent mFingerprintIntent;
    private Intent mFaceIntent;

    @Before
    public void setUp() throws RemoteException {
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mFingerprintIntent = new Intent(mContext, FingerprintEnrollIntroduction.class);
        mFaceIntent = new Intent(mContext, FaceEnrollIntroduction.class);
        mMultiBiometricEnrollHelper = new MultiBiometricEnrollHelper(
                mActivity, mUserId, true /* enrollFace */, true /* enrollFingerprint */,
                mGkPwHandle, mFingerprintManager, mFaceManager, mFingerprintIntent, mFaceIntent,
                (challenge) -> null);
    }

    @Test
    public void launchFaceAndFingerprintEnroll_testFingerprint() {
        mMultiBiometricEnrollHelper.startNextStep();

        verify(mFingerprintManager).generateChallenge(anyInt(), mFingerprintCaptor.capture());

        FingerprintManager.GenerateChallengeCallback generateChallengeCallback =
                mFingerprintCaptor.getValue();
        generateChallengeCallback.onChallengeGenerated(mSensorId, mUserId, mChallenge);

        assertThat(mFingerprintIntent.hasExtra(
                MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FINGERPRINT)).isTrue();
        assertThat(mFingerprintIntent.getExtra(BiometricEnrollBase.EXTRA_KEY_SENSOR_ID,
                -1 /* defaultValue */)).isEqualTo(mSensorId);
        assertThat(mFingerprintIntent.getExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE,
                -1 /* defaultValue */)).isEqualTo(mChallenge);
        assertThat(mFingerprintIntent.getExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                -1 /* defaultValue */)).isEqualTo(mGkPwHandle);
    }

    @Test
    public void launchFaceAndFingerprintEnroll_testFace() {
        mMultiBiometricEnrollHelper.startNextStep();

        verify(mFingerprintManager).generateChallenge(anyInt(), mFingerprintCaptor.capture());

        FingerprintManager.GenerateChallengeCallback fingerprintGenerateChallengeCallback =
                mFingerprintCaptor.getValue();
        fingerprintGenerateChallengeCallback.onChallengeGenerated(
                mSensorId, mUserId, mChallenge);

        assertThat(mFaceIntent.getExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                -1 /* defaultValue */)).isEqualTo(mGkPwHandle);
        assertThat(mFaceIntent.getIntExtra(Intent.EXTRA_USER_ID, -1 /* defaultValue */))
                .isEqualTo(mUserId);

        final ShadowPackageManager shadowPackageManager = shadowOf(mContext.getPackageManager());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_FACE, true);
        ShadowUtils.setFaceManager(mFaceManager);
        ActivityController.of(new FaceEnrollIntroduction(), mFaceIntent)
                .create(mFaceIntent.getExtras()).get();

        verify(mFaceManager).generateChallenge(eq(mUserId), any());
    }
}
