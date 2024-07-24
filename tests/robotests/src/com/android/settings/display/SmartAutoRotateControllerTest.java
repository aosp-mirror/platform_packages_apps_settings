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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.testutils.ResolveInfoBuilder;
import com.android.settings.testutils.shadow.ShadowDeviceStateRotationLockSettingsManager;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSensorPrivacyManager.class, ShadowSystemSettings.class})
public class SmartAutoRotateControllerTest {

    private static final String PACKAGE_NAME = "package_name";

    private SmartAutoRotateController mController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Preference mPreference;
    private ContentResolver mContentResolver;
    private final DeviceStateRotationLockSettingsManager mDeviceStateAutoRotateSettingsManager =
            DeviceStateRotationLockSettingsManager.getInstance(RuntimeEnvironment.application);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = Mockito.spy(RuntimeEnvironment.application);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(context.getPackageManager()).thenReturn(mPackageManager);
        when(context.getContentResolver()).thenReturn(mContentResolver);
        doReturn(PACKAGE_NAME).when(mPackageManager).getRotationResolverPackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);
        mController = Mockito.spy(new SmartAutoRotateController(context, "test_key"));
        when(mController.isCameraLocked()).thenReturn(false);
        when(mController.isPowerSaveMode()).thenReturn(false);
        doReturn(mController.getPreferenceKey()).when(mPreference).getKey();

        final ResolveInfo resolveInfo = new ResolveInfoBuilder(PACKAGE_NAME).build();
        resolveInfo.serviceInfo = new ServiceInfo();
        when(mPackageManager.resolveService(any(), anyInt())).thenReturn(resolveInfo);
        enableAutoRotation();
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_resolveInfoIsNull_returnUnsupportedOnDevice() {
        when(mPackageManager.resolveService(any(), anyInt())).thenReturn(null);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noCameraPermission_returnDisableDependentSetting() {
        doReturn(PackageManager.PERMISSION_DENIED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_rotationLocked_returnDisableDependentSetting() {
        disableAutoRotation();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_cameraDisabled_returnDisableDependentSetting() {
        when(mController.isCameraLocked()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_powerSaveEnabled_returnDisableDependentSetting() {
        when(mController.isPowerSaveMode()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @Config(shadows = {
            ShadowDeviceStateRotationLockSettingsManager.class,
            ShadowRotationPolicy.class
    })
    public void getAvailabilityStatus_deviceStateRotationLocked_returnDisableDependentSetting() {
        enableDeviceStateRotation();
        lockDeviceStateRotation();

        int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @Config(shadows = {
            ShadowDeviceStateRotationLockSettingsManager.class,
            ShadowRotationPolicy.class
    })
    public void getAvailabilityStatus_deviceStateRotationUnlocked_returnAvailable() {
        enableDeviceStateRotation();
        unlockDeviceStateRotation();

        int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(AVAILABLE);
    }

    private void enableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1, UserHandle.USER_CURRENT);
    }

    private void disableAutoRotation() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT);
    }

    private void enableDeviceStateRotation() {
        ShadowRotationPolicy.setRotationSupported(true);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(true);
    }

    private void lockDeviceStateRotation() {
        ShadowDeviceStateRotationLockSettingsManager shadowManager =
                Shadow.extract(mDeviceStateAutoRotateSettingsManager);
        shadowManager.setRotationLockedForAllStates(true);
    }

    private void unlockDeviceStateRotation() {
        ShadowDeviceStateRotationLockSettingsManager shadowManager =
                Shadow.extract(mDeviceStateAutoRotateSettingsManager);
        shadowManager.setRotationLockedForAllStates(false);
    }
}
