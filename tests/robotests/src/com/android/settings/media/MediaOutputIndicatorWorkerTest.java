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

package com.android.settings.media;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class MediaOutputIndicatorWorkerTest {
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");

    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    private Context mContext;
    private MediaOutputIndicatorWorker mMediaDeviceUpdateWorker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        mMediaDeviceUpdateWorker = new MediaOutputIndicatorWorker(mContext, URI);
    }

    @Test
    public void onSlicePinned_registerCallback() {
        mMediaDeviceUpdateWorker.onSlicePinned();
        verify(mBluetoothEventManager).registerCallback(mMediaDeviceUpdateWorker);
    }

    @Test
    public void onSliceUnpinned_unRegisterCallback() {
        mMediaDeviceUpdateWorker.onSlicePinned();
        mMediaDeviceUpdateWorker.onSliceUnpinned();
        verify(mBluetoothEventManager).unregisterCallback(mMediaDeviceUpdateWorker);
    }
}
