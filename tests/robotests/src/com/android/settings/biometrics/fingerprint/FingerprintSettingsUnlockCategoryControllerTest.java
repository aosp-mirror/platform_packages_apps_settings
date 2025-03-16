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

package com.android.settings.biometrics.fingerprint;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class FingerprintSettingsUnlockCategoryControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RestrictedSwitchPreference mPreference;

    private Context mContext;
    private FingerprintSettingsRequireScreenOnToAuthPreferenceController mController;

    private FingerprintSettingsScreenOffUnlockUdfpsPreferenceController mScreenOffUnlockController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(eq(Context.FINGERPRINT_SERVICE))).thenReturn(
                mFingerprintManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mController = spy(new FingerprintSettingsRequireScreenOnToAuthPreferenceController(mContext,
                "test_key"));
        mScreenOffUnlockController = spy(
            new FingerprintSettingsScreenOffUnlockUdfpsPreferenceController(mContext,
                "screen_off_unlock_test_key"));
        ReflectionHelpers.setField(mController, "mFingerprintManager", mFingerprintManager);
        ReflectionHelpers.setField(mScreenOffUnlockController, "mFingerprintManager",
                mFingerprintManager);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void isAvailable_isEnabled_whenSfpsHardwareDetected_AndHasEnrolledFingerprints() {
        assertThat(mController.isAvailable()).isEqualTo(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                true /* isPowerbuttonFps */,
                true /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isEqualTo(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(android.hardware.biometrics.Flags.FLAG_SCREEN_OFF_UNLOCK_UDFPS)
    public void isAvailable_isEnabled_whenUdfpsHardwareDetected_AndHasEnrolledFingerprints() {
        assertThat(mScreenOffUnlockController.isAvailable()).isEqualTo(false);
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                false /* isPowerbuttonFps false implies udfps */,
                true /* hasEnrolledTemplates */);
        assertThat(mScreenOffUnlockController.isAvailable()).isEqualTo(true);
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isUnavailable_isDisabled_whenSfpsHardwareDetected_AndNoEnrolledFingerprints() {
        assertThat(mController.isAvailable()).isEqualTo(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                true /* isPowerbuttonFps */,
                false /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isEqualTo(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(android.hardware.biometrics.Flags.FLAG_SCREEN_OFF_UNLOCK_UDFPS)
    public void isUnavailable_isDisabled_whenUdfpsHardwareDetected_AndNoEnrolledFingerprints() {
        assertThat(mScreenOffUnlockController.isAvailable()).isEqualTo(false);
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                false /* isPowerbuttonFps false implies udfps */,
                false /* hasEnrolledTemplates */);
        assertThat(mScreenOffUnlockController.isAvailable()).isEqualTo(false);
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isUnavailable_whenHardwareNotDetected_onSfpsDevice() {
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                false /* isHardwareDetected */,
                true /* isPowerbuttonFps */,
                true /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isEqualTo(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(android.hardware.biometrics.Flags.FLAG_SCREEN_OFF_UNLOCK_UDFPS)
    public void isUnavailable_whenHardwareNotDetected_onUdfpsDevice() {
        assertThat(mScreenOffUnlockController.isAvailable()).isFalse();
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                false /* isHardwareDetected */,
                false /* isPowerbuttonFps false implies udfps */,
                true /* hasEnrolledTemplates */);
        assertThat(mScreenOffUnlockController.isAvailable()).isEqualTo(false);
        assertThat(mScreenOffUnlockController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isUnavailable_onNonSfpsDevice() {
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                false /* isPowerbuttonFps */,
                true /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    private void configure_hardwareDetected_isSfps_hasEnrolledTemplates(
            boolean isHardwareDetected, boolean isPowerbuttonFps, boolean hasEnrolledTemplates) {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(isHardwareDetected);
        when(mFingerprintManager.isPowerbuttonFps()).thenReturn(isPowerbuttonFps);
        when(mFingerprintManager.hasEnrolledTemplates(anyInt())).thenReturn(hasEnrolledTemplates);
    }

}
