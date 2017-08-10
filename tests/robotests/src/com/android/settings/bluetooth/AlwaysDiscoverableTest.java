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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

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
public class AlwaysDiscoverableTest {
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;

    @Mock
    private Context mContext;

    private AlwaysDiscoverable mAlwaysDiscoverable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAlwaysDiscoverable = new AlwaysDiscoverable(mContext, mLocalAdapter);
    }

    @Test
    public void isStartedWithoutStart() {
        assertThat(mAlwaysDiscoverable.mStarted).isFalse();
    }

    @Test
    public void isStartedWithStart() {
        mAlwaysDiscoverable.start();
        assertThat(mAlwaysDiscoverable.mStarted).isTrue();
    }

    @Test
    public void isStartedWithStartStop() {
        mAlwaysDiscoverable.start();
        mAlwaysDiscoverable.stop();
        assertThat(mAlwaysDiscoverable.mStarted).isFalse();
    }

    @Test
    public void stopWithoutStart() {
        mAlwaysDiscoverable.stop();
        // expect no crash
        verify(mLocalAdapter, never()).setScanMode(anyInt());
    }

    @Test
    public void startSetsModeAndRegistersReceiver() {
        when(mLocalAdapter.getScanMode()).thenReturn(BluetoothAdapter.SCAN_MODE_NONE);
        mAlwaysDiscoverable.start();
        verify(mLocalAdapter).setScanMode(eq(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE));
        verify(mContext).registerReceiver(eq(mAlwaysDiscoverable), any());
    }

    @Test
    public void stopUnregistersReceiver() {
        mAlwaysDiscoverable.start();
        mAlwaysDiscoverable.stop();
        verify(mContext).unregisterReceiver(mAlwaysDiscoverable);
    }

    @Test
    public void resetsToDiscoverableModeWhenScanModeChanges() {
        mAlwaysDiscoverable.start();
        verify(mLocalAdapter, times(1)).setScanMode(
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);

        sendScanModeChangedIntent(BluetoothAdapter.SCAN_MODE_CONNECTABLE,
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);

        verify(mLocalAdapter, times(2)).setScanMode(
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
    }

    private void sendScanModeChangedIntent(int newMode, int previousMode) {
        when(mLocalAdapter.getScanMode()).thenReturn(newMode);
        Intent intent = new Intent(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_SCAN_MODE, newMode);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, previousMode);
        mAlwaysDiscoverable.onReceive(mContext, intent);
    }
}
