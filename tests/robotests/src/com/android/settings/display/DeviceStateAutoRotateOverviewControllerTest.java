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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.testutils.shadow.ShadowDeviceStateRotationLockSettingsManager;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRotationPolicy.class, ShadowDeviceStateRotationLockSettingsManager.class})
public class DeviceStateAutoRotateOverviewControllerTest {

    private final DeviceStateAutoRotateOverviewController mController =
            new DeviceStateAutoRotateOverviewController(
                    RuntimeEnvironment.application, "device_state_auto_rotate");

    @Test
    public void getAvailabilityStatus_rotationAndDeviceStateRotationEnabled_returnsAvailable() {
        ShadowRotationPolicy.setRotationSupported(true);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(true);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_rotationNotSupported_returnsUnsupportedOnDevice() {
        ShadowRotationPolicy.setRotationSupported(false);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(true);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_deviceStateRotationNotSupported_returnsUnsupportedOnDevice() {
        ShadowRotationPolicy.setRotationSupported(true);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(false);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
