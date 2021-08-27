/*
 * Copyright 2020 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowBluetoothAdapter.class)
public class PreviouslyConnectedDeviceDashboardFragmentTest {

    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private PreviouslyConnectedDeviceDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mFragment = new PreviouslyConnectedDeviceDashboardFragment();
        mFragment.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Test
    public void onStart_bluetoothIsDisable_enableBluetooth() {
        mShadowBluetoothAdapter.setEnabled(false);

        assertThat(mFragment.mBluetoothAdapter.isEnabled()).isFalse();
        mFragment.enableBluetoothIfNecessary();

        assertThat(mFragment.mBluetoothAdapter.isEnabled()).isTrue();
    }
}
