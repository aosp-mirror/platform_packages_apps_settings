/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.provider.Settings.Secure.CAMERA_AUTOROTATE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResolveInfoBuilder;
import com.android.settings.testutils.shadow.ShadowDeviceStateRotationLockSettingsManager;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
        ShadowSensorPrivacyManager.class,
        ShadowDeviceStateRotationLockSettingsManager.class
})
public class SmartAutoRotatePreferenceControllerTest {

    private static final String PACKAGE_NAME = "package_name";
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;
    private Context mContext;
    private ContentResolver mContentResolver;
    private SmartAutoRotatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Mockito.spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        when(mResources.getBoolean(R.bool.config_auto_rotate_face_detection_available)).thenReturn(
                true);

        doReturn(PACKAGE_NAME).when(mPackageManager).getRotationResolverPackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);
        when(mContext.getString(R.string.auto_rotate_option_off))
                .thenReturn("Off");
        when(mContext.getString(R.string.auto_rotate_option_on))
                .thenReturn("On");
        when(mContext.getString(R.string.auto_rotate_option_face_based))
                .thenReturn("On - Face-based");

        disableCameraBasedRotation();
        final ResolveInfo resolveInfo = new ResolveInfoBuilder(PACKAGE_NAME).build();
        resolveInfo.serviceInfo = new ServiceInfo();
        when(mPackageManager.resolveService(any(), anyInt())).thenReturn(resolveInfo);

        mController = Mockito.spy(
                new SmartAutoRotatePreferenceController(mContext, "smart_auto_rotate"));
        when(mController.isCameraLocked()).thenReturn(false);
        when(mController.isPowerSaveMode()).thenReturn(false);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(false);
    }

    @Test
    public void isAvailableWhenPolicyAllows() {
        assertThat(mController.isAvailable()).isFalse();

        enableAutoRotationPreference();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getSummary_settingsIsOff_returnsOff() {
        disableAutoRotation();

        assertThat(mController.getSummary()).isEqualTo("Off");
    }

    @Test
    public void getSummary_settingsIsOn_returnsOn() {
        enableAutoRotation();

        assertThat(mController.getSummary()).isEqualTo("On");
    }

    @Test
    public void getSummary_autoRotateOffSmartAutoRotateOn_returnsOff() {
        enableCameraBasedRotation();
        disableAutoRotation();

        assertThat(mController.getSummary()).isEqualTo("Off");
    }

    @Test
    public void updatePreference_smartAutoRotateOn_returnsFaceBased() {
        enableCameraBasedRotation();
        enableAutoRotation();

        assertThat(mController.getSummary()).isEqualTo("On - Face-based");
    }

    @Test
    public void getSummary_noSmartAuto_returnsOff() {
        disableAutoRotation();
        Settings.Secure.putStringForUser(mContentResolver,
                CAMERA_AUTOROTATE, null, UserHandle.USER_CURRENT);

        assertThat(mController.getSummary()).isEqualTo("Off");

    }

    @Test
    public void getSummary_noSmartAuto_returnsOn() {
        enableAutoRotation();
        Settings.Secure.putStringForUser(mContentResolver,
                CAMERA_AUTOROTATE, null, UserHandle.USER_CURRENT);

        assertThat(mController.getSummary()).isEqualTo("On");
    }

    @Test
    public void getSummary_noCameraPermission_returnsOn() {
        enableAutoRotation();
        enableCameraBasedRotation();
        doReturn(PackageManager.PERMISSION_DENIED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        assertThat(mController.getSummary()).isEqualTo("On");
    }

    @Test
    public void getSummary_cameraDisabled_returnsOn() {
        enableAutoRotation();
        enableCameraBasedRotation();
        when(mController.isCameraLocked()).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo("On");
    }

    @Test
    public void getSummary_powerSaveEnabled_returnsOn() {
        enableAutoRotation();
        enableCameraBasedRotation();
        when(mController.isPowerSaveMode()).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo("On");
    }

    @Test
    public void testGetAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .UNSUPPORTED_ON_DEVICE);

        enableAutoRotationPreference();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .AVAILABLE);

        disableAutoRotationPreference();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(BasePreferenceController
                .UNSUPPORTED_ON_DEVICE);
    }


    @Test
    public void getAvailabilityStatus_deviceStateRotationEnabled_returnsUnsupported() {
        enableAutoRotationPreference();
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final AutoRotatePreferenceController controller =
                new AutoRotatePreferenceController(mContext, "auto_rotate");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final AutoRotatePreferenceController controller =
                new AutoRotatePreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    private void enableAutoRotationPreference() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mResources.getBoolean(anyInt())).thenReturn(true);
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0,
                UserHandle.USER_CURRENT);
    }

    private void disableAutoRotationPreference() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mResources.getBoolean(anyInt())).thenReturn(true);
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 1,
                UserHandle.USER_CURRENT);
    }

    private void enableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1, UserHandle.USER_CURRENT);
    }

    private void disableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT);
    }

    private void enableCameraBasedRotation() {
        Settings.Secure.putIntForUser(mContentResolver,
                CAMERA_AUTOROTATE, 1, UserHandle.USER_CURRENT);
    }

    private void disableCameraBasedRotation() {
        Settings.Secure.putIntForUser(mContentResolver,
                CAMERA_AUTOROTATE, 0, UserHandle.USER_CURRENT);
    }
}
