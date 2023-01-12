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

package com.android.settings.biometrics.fingerprint;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.After;
import org.junit.Before;
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
public class FingerprintSettingsRequireScreenOnToAuthPreferenceControllerTest {

    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RestrictedSwitchPreference mPreference;

    private Context mContext;
    private FingerprintSettingsRequireScreenOnToAuthPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(eq(Context.FINGERPRINT_SERVICE))).thenReturn(
                mFingerprintManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mController = spy(new FingerprintSettingsRequireScreenOnToAuthPreferenceController(mContext,
                "test_key"));
        ReflectionHelpers.setField(mController, "mFingerprintManager", mFingerprintManager);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void onPreferenceChange_settingIsUpdated() {
        boolean state = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SFPS_REQUIRE_SCREEN_ON_TO_AUTH_ENABLED, 1) != 0;

        assertThat(mController.isChecked()).isFalse();
        assertThat(mController.onPreferenceChange(mPreference, !state)).isTrue();
        boolean newState = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SFPS_REQUIRE_SCREEN_ON_TO_AUTH_ENABLED, 1) != 0;
        assertThat(newState).isEqualTo(!state);
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
    public void isAvailable_isDisabled_whenSfpsHardwareDetected_AndNoEnrolledFingerprints() {
        assertThat(mController.isAvailable()).isEqualTo(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                true /* isHardwareDetected */,
                true /* isPowerbuttonFps */,
                false /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isEqualTo(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void isUnavailable_whenHardwareNotDetected() {
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        configure_hardwareDetected_isSfps_hasEnrolledTemplates(
                false /* isHardwareDetected */,
                true /* isPowerbuttonFps */,
                true /* hasEnrolledTemplates */);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
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
