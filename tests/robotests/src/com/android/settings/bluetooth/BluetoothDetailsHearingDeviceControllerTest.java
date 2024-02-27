/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BluetoothDetailsHearingDeviceController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsHearingDeviceControllerTest extends
        BluetoothDetailsControllerTestBase {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private BluetoothDetailsHearingDeviceController mHearingDeviceController;

    @Mock
    private BluetoothDetailsHearingDeviceSettingsController mHearingDeviceSettingsController;

    @Override
    public void setUp() {
        super.setUp();

        mHearingDeviceController = new BluetoothDetailsHearingDeviceController(mContext,
                mFragment, mCachedDevice, mLifecycle);
        mHearingDeviceController.setSubControllers(mHearingDeviceSettingsController);
    }

    @Test
    public void isAvailable_hearingDeviceSettingsAvailable_returnTrue() {
        when(mHearingDeviceSettingsController.isAvailable()).thenReturn(true);

        assertThat(mHearingDeviceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noControllersAvailable_returnFalse() {
        when(mHearingDeviceSettingsController.isAvailable()).thenReturn(false);

        assertThat(mHearingDeviceController.isAvailable()).isFalse();
    }


    @Test
    public void initSubControllers_launchFromHearingDevicePage_hearingDeviceSettingsNotExist() {
        mHearingDeviceController.initSubControllers(true);

        assertThat(mHearingDeviceController.getSubControllers().stream().anyMatch(
                c -> c instanceof BluetoothDetailsHearingDeviceSettingsController)).isFalse();
    }

    @Test
    public void initSubControllers_notLaunchFromHearingDevicePage_hearingDeviceSettingsExist() {
        mHearingDeviceController.initSubControllers(false);

        assertThat(mHearingDeviceController.getSubControllers().stream().anyMatch(
                c -> c instanceof BluetoothDetailsHearingDeviceSettingsController)).isTrue();
    }
}
