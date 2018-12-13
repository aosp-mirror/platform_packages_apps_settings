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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDeviceDetailsFragmentTest {

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private BluetoothDeviceDetailsFragment mFragment;
    private Context mContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();

        mFragment = spy(BluetoothDeviceDetailsFragment.newInstance(TEST_ADDRESS));
        doReturn(mLocalManager).when(mFragment).getLocalBluetoothManager(any());
        doReturn(mCachedDevice).when(mFragment).getCachedDevice(any());

        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, TEST_ADDRESS);
        mFragment.setArguments(args);
        mFragment.onAttach(mContext);
    }

    @Test
    public void verifyOnAttachResult() {
        assertThat(mFragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(mFragment.mManager).isEqualTo(mLocalManager);
        assertThat(mFragment.mCachedDevice).isEqualTo(mCachedDevice);
    }
}
