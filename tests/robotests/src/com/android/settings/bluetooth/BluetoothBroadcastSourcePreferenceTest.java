/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BluetoothBroadcastSourcePreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    BluetoothLeBroadcastReceiveState mBroadcastReceiveState;
    @Mock
    BluetoothLeBroadcastMetadata mBroadcastMetadata;

    BluetoothBroadcastSourcePreference mPreference;


    @Before
    public void setUp() {
        mPreference = new BluetoothBroadcastSourcePreference(mContext);
    }

    @Test
    public void isCreatedByReceiveState_updateUiFromReceviceState_returnsTrue() {
        mPreference.updateReceiveStateAndRefreshUi(mBroadcastReceiveState);

        assertThat(mPreference.isCreatedByReceiveState()).isTrue();
    }

    @Test
    public void isCreatedByReceiveState_updateUiFromMetadata_returnsFalse() {
        mPreference.updateMetadataAndRefreshUi(mBroadcastMetadata, true);

        assertThat(mPreference.isCreatedByReceiveState()).isFalse();
    }

}
