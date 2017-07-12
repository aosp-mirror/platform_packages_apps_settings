/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.Mockito.verify;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DevicePickerFragmentTest {
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    @Mock
    private BluetoothProgressCategory mAvailableDevicesCategory;
    private DevicePickerFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new DevicePickerFragment();

        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
    }

    @Test
    public void testScanningStateChanged_started_setProgressStarted() {
        mFragment.mScanEnabled = true;

        mFragment.onScanningStateChanged(true);

        verify(mAvailableDevicesCategory).setProgress(true);
    }
}
