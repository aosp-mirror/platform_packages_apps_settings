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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothA2dp;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AbstractBluetoothPreferenceControllerTest {

    @Mock
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    @Mock
    private BluetoothA2dp mBluetoothA2dp;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext;
    private AbstractBluetoothPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new AbstractBluetoothPreferenceControllerImpl(mContext, mLifecycle,
                mBluetoothA2dpConfigStore));
    }

    @Test
    public void onBluetoothServiceConnected_checkProfile() {
        assertThat(mController.mBluetoothA2dp).isNull();
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertThat(mController.mBluetoothA2dp).isEqualTo(mBluetoothA2dp);
    }

    @Test
    public void onBluetoothServiceConnected_updateState() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        verify(mController).updateState(any());
    }

    @Test
    public void onBluetoothCodecUpdated_updateState() {
        mController.onBluetoothCodecUpdated();
        verify(mController).updateState(any());
    }

    @Test
    public void onBluetoothServiceDisconnected_clearA2dpProfile() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertThat(mController.mBluetoothA2dp).isEqualTo(mBluetoothA2dp);
        mController.onBluetoothServiceDisconnected();
        assertThat(mController.mBluetoothA2dp).isNull();
    }

    @Test
    public void onDestroy_clearA2dpProfile() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertThat(mController.mBluetoothA2dp).isEqualTo(mBluetoothA2dp);
        mController.onDestroy();
        assertThat(mController.mBluetoothA2dp).isNull();
    }

    private static class AbstractBluetoothPreferenceControllerImpl extends
            AbstractBluetoothPreferenceController {

        private AbstractBluetoothPreferenceControllerImpl(Context context, Lifecycle lifecycle,
                BluetoothA2dpConfigStore store) {
            super(context, lifecycle, store);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }
    }
}
