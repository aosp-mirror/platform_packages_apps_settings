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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.wifitrackerlib.WifiEntry;

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
public class LongPressWifiEntryPreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    Fragment mFragment;
    @Mock
    WifiEntry mWifiEntry;

    LongPressWifiEntryPreference mPreference;

    @Before
    public void setUp() {
        // Fake mWifiEntry as an available Wi-Fi network, and it's not connected.
        when(mWifiEntry.canConnect()).thenReturn(true);
        when(mWifiEntry.canDisconnect()).thenReturn(false);
        when(mWifiEntry.isSaved()).thenReturn(false);

        mPreference = new LongPressWifiEntryPreference(mContext, mWifiEntry, mFragment);
    }

    @Test
    public void shouldEnabled_canConnect_returnTrue() {
        // Fake mWifiEntry as an available Wi-Fi network, and it's not connected.
        when(mWifiEntry.canConnect()).thenReturn(true);

        assertThat(mPreference.shouldEnabled()).isTrue();
    }

    @Test
    public void shouldEnabled_canNotConnect_returnFalse() {
        // Fake mWifiEntry as a restricted Wi-Fi network, and cannot connect.
        when(mWifiEntry.canConnect()).thenReturn(false);

        assertThat(mPreference.shouldEnabled()).isFalse();
    }

    @Test
    public void shouldEnabled_canNotConnectButCanDisconnect_returnTrue() {
        // Fake mWifiEntry as a connected Wi-Fi network without saved configuration.
        when(mWifiEntry.canConnect()).thenReturn(false);
        when(mWifiEntry.canDisconnect()).thenReturn(true);

        assertThat(mPreference.shouldEnabled()).isTrue();
    }

    @Test
    public void shouldEnabled_canNotConnectButIsSaved_returnTrue() {
        // Fake mWifiEntry as a saved Wi-Fi network
        when(mWifiEntry.canConnect()).thenReturn(false);
        when(mWifiEntry.isSaved()).thenReturn(true);

        assertThat(mPreference.shouldEnabled()).isTrue();
    }

    @Test
    public void shouldEnabled_canNotConnectButCanDisconnectAndIsSaved_returnTrue() {
        // Fake mWifiEntry as a connected Wi-Fi network
        when(mWifiEntry.canConnect()).thenReturn(false);
        when(mWifiEntry.canDisconnect()).thenReturn(true);
        when(mWifiEntry.isSaved()).thenReturn(true);

        assertThat(mPreference.shouldEnabled()).isTrue();
    }
}
