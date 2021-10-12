/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.ADAPTIVE_SLEEP;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.RestrictionUtils;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSensorPrivacyManager.class)
public class AdaptiveSleepPreferenceControllerTest {
    private Context mContext;
    private AdaptiveSleepPreferenceController mController;
    private ContentResolver mContentResolver;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(getApplicationContext());
        mContentResolver = mContext.getContentResolver();

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.getAttentionServicePackageName()).thenReturn("some.package");
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);
        when(mRestrictionUtils.checkIfRestrictionEnforced(any(),
                eq(UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT))).thenReturn(null);

        mController = spy(new AdaptiveSleepPreferenceController(mContext, mRestrictionUtils));
        mController.initializePreference();
        when(mController.isCameraLocked()).thenReturn(false);
        when(mController.isPowerSaveMode()).thenReturn(false);
    }

    @Test
    public void controlSetting_preferenceChecked_FeatureTurnOn() {
        mController.mPreference.setChecked(false);

        mController.mPreference.performClick();

        int mode = Settings.Secure.getInt(mContentResolver, ADAPTIVE_SLEEP, 0);
        assertThat(mode).isEqualTo(1);
    }

    @Test
    public void controlSetting_preferenceNotChecked_FeatureTurnOff() {
        mController.mPreference.setChecked(true);

        mController.mPreference.performClick();

        int mode = Settings.Secure.getInt(mContentResolver, ADAPTIVE_SLEEP, 1);
        assertThat(mode).isEqualTo(0);
    }

    @Test
    public void isControllerAvailable_serviceNotSupported_returnUnsupportedCode() {
        when(mPackageManager.resolveService(isA(Intent.class), anyInt())).thenReturn(null);

        assertThat(AdaptiveSleepPreferenceController.isControllerAvailable(mContext)).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void hasSufficientPermission_permissionGranted_returnTrue() {
        assertThat(AdaptiveSleepPreferenceController.hasSufficientPermission(
                mPackageManager)).isTrue();
    }

    @Test
    public void hasSufficientPermission_permissionNotGranted_returnFalse() {
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_DENIED);

        assertThat(AdaptiveSleepPreferenceController.hasSufficientPermission(
                mPackageManager)).isFalse();
    }

    @Test
    public void addToScreen_normalCase_enablePreference() {
        mController.mPreference.setEnabled(false);
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isEnabled()).isTrue();
        verify(mScreen).addPreference(mController.mPreference);
    }

    @Test
    public void addToScreen_permissionNotGranted_disablePreference() {
        mController.mPreference.setEnabled(true);
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_DENIED);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isEnabled()).isFalse();
    }

    @Test
    public void addToScreen_enforcedAdmin_disablePreference() {
        mController.mPreference.setEnabled(true);

        when(mRestrictionUtils.checkIfRestrictionEnforced(any(),
                eq(UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT))).thenReturn(mEnforcedAdmin);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isEnabled()).isFalse();
    }

    @Test
    public void addToScreen_cameraIsLocked_disablePreference() {
        when(mController.isCameraLocked()).thenReturn(true);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isEnabled()).isFalse();
    }

    @Test
    public void addToScreen_powerSaveEnabled_disablePreference() {
        when(mController.isPowerSaveMode()).thenReturn(true);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isEnabled()).isFalse();
    }

}
