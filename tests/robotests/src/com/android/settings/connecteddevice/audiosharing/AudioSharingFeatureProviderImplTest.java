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

import com.android.settings.connecteddevice.AvailableMediaDeviceGroupController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingFeatureProviderImplTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private DashboardFragment mFragment;
    private Context mContext;
    private AudioSharingFeatureProviderImpl mFeatureProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureProvider = new AudioSharingFeatureProviderImpl();
    }

    @Test
    public void createAudioSharingDevicePreferenceController_returnsNull() {
        assertThat(
                        mFeatureProvider.createAudioSharingDevicePreferenceController(
                                mContext, mFragment, /* lifecycle= */ null))
                .isNull();
    }

    @Test
    public void createAvailableMediaDeviceGroupController_returnsNull() {
        assertThat(
                        mFeatureProvider.createAvailableMediaDeviceGroupController(
                                mContext, /* fragment= */ null, /* lifecycle= */ null))
                .isInstanceOf(AvailableMediaDeviceGroupController.class);
    }

    @Test
    public void isAudioSharingFilterMatched_returnsFalse() {
        assertThat(mFeatureProvider.isAudioSharingFilterMatched(mCachedDevice, mLocalBtManager))
                .isFalse();
    }
}
