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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.idleMainLooper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
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
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class ActiveUnlockStatusPreferenceControllerTest {

    @Rule public final MockitoRule mMocks = MockitoJUnit.rule();

    @Mock private UserManager mUserManager;
    @Mock private PackageManager mPackageManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private FaceManager mFaceManager;
    @Mock private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private ActiveUnlockStatusPreferenceController mController;
    private RestrictedPreference mPreference;

    @Before
    public void setUp() {
        Robolectric.setupContentProvider(FakeContentProvider.class, FakeContentProvider.AUTHORITY);
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
        when(mPreferenceScreen.findPreference(any())).thenReturn(mPreference);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        ActiveUnlockTestUtils.enable(mContext);
        FakeContentProvider.init(mContext);
        mController = new ActiveUnlockStatusPreferenceController(mContext);
    }

    @After
    public void tearDown() {
        ActiveUnlockTestUtils.disable(mContext);
    }

    @Test
    public void updateState_featureFlagDisabled_isNotVisible() {
        ActiveUnlockTestUtils.disable(mContext);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_withoutFingerprint_withoutFace_isNotVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_withoutFingerprint_withFace_isVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_withFingerprint_withoutFace_isVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_withFingerprint_withFace_isVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void defaultState_summaryIsEmpty() {
        mController.displayPreference(mPreferenceScreen);

        idleMainLooper();

        assertThat(mPreference.getSummary().toString()).isEqualTo(" ");
    }

    @Test
    public void onStart_summaryIsUpdated() {
        String summary = "newSummary";
        updateSummary(summary);
        mController.displayPreference(mPreferenceScreen);

        mController.onStart();
        idleMainLooper();

        assertThat(mPreference.getSummary().toString()).isEqualTo(summary);
    }

    @Test
    public void biometricsNotSetUp_deviceNameIsNotSet_setupBiometricStringShown() {
        ActiveUnlockTestUtils.enable(mContext, ActiveUnlockStatusUtils.BIOMETRIC_FAILURE_LAYOUT);
        updateSummary("newSummary");
        mController.displayPreference(mPreferenceScreen);

        mController.onStart();
        idleMainLooper();

        assertThat(mPreference.getSummary()).isEqualTo(mContext.getString(
                R.string.security_settings_activeunlock_require_face_fingerprint_setup_title));
    }

    @Test
    public void biometricNotSetUp_deviceNameIsSet_summaryShown() {
        ActiveUnlockTestUtils.enable(mContext, ActiveUnlockStatusUtils.BIOMETRIC_FAILURE_LAYOUT);
        String summary = "newSummary";
        updateSummary(summary);
        updateDeviceName("deviceName");
        mController.displayPreference(mPreferenceScreen);

        mController.onStart();
        idleMainLooper();

        assertThat(mPreference.getSummary()).isEqualTo(summary);
    }

    @Test
    public void biometricSetUp_summaryShown() {
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);
        ActiveUnlockTestUtils.enable(mContext, ActiveUnlockStatusUtils.BIOMETRIC_FAILURE_LAYOUT);
        String summary = "newSummary";
        updateSummary(summary);
        mController.displayPreference(mPreferenceScreen);

        mController.onStart();
        idleMainLooper();

        assertThat(mPreference.getSummary()).isEqualTo(summary);
    }

    private void updateSummary(String summary) {
        FakeContentProvider.setTileSummary(summary);
        mContext.getContentResolver().notifyChange(FakeContentProvider.URI, null /* observer */);
        idleMainLooper();
    }

    private void updateDeviceName(String deviceName) {
        FakeContentProvider.setDeviceName(deviceName);
        mContext.getContentResolver().notifyChange(FakeContentProvider.URI, null /* observer */);
        idleMainLooper();
    }

}
