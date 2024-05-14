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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingDeviceVolumePreferenceTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private CachedBluetoothDevice mCachedDevice;
    private Context mContext;
    private AudioSharingDeviceVolumePreference mPreference;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new AudioSharingDeviceVolumePreference(mContext, mCachedDevice);
    }

    @Test
    public void getCachedDevice_returnsDevice() {
        assertThat(mPreference.getCachedDevice()).isEqualTo(mCachedDevice);
    }

    @Test
    public void initialize_setupMaxMin() {
        mPreference.initialize();
        assertThat(mPreference.getMax()).isEqualTo(AudioSharingDeviceVolumePreference.MAX_VOLUME);
        assertThat(mPreference.getMin()).isEqualTo(AudioSharingDeviceVolumePreference.MIN_VOLUME);
    }
}
