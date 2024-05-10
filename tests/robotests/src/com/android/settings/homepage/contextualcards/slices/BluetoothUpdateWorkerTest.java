/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;

import com.android.settings.slices.ShadowSliceBackgroundWorker;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowSliceBackgroundWorker.class})
public class BluetoothUpdateWorkerTest {

    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");

    private BluetoothUpdateWorker mBluetoothUpdateWorker;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mBluetoothUpdateWorker = spy(new BluetoothUpdateWorker(mContext, URI));
    }

    @Test
    public void onAclConnectionStateChanged_shouldNotifyChange() {
        mBluetoothUpdateWorker.onAclConnectionStateChanged(null, 0);

        verify(mBluetoothUpdateWorker).notifySliceChange();
    }

    @Test
    public void onActiveDeviceChanged_shouldNotifyChange() {
        mBluetoothUpdateWorker.onActiveDeviceChanged(null, 0);

        verify(mBluetoothUpdateWorker).notifySliceChange();
    }

    @Test
    public void onBluetoothStateChanged_shouldNotifyChange() {
        mBluetoothUpdateWorker.onBluetoothStateChanged(0);

        verify(mBluetoothUpdateWorker).notifySliceChange();
    }

    @Test
    public void onConnectionStateChanged_shouldNotifyChange() {
        mBluetoothUpdateWorker.onConnectionStateChanged(null, 0);

        verify(mBluetoothUpdateWorker).notifySliceChange();
    }

    @Test
    public void onProfileConnectionStateChanged_shouldNotifyChange() {
        mBluetoothUpdateWorker.onProfileConnectionStateChanged(null, 0, 0);

        verify(mBluetoothUpdateWorker).notifySliceChange();
    }
}