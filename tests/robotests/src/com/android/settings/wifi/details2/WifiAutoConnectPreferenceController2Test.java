/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiAutoConnectPreferenceController2Test {

    private WifiAutoConnectPreferenceController2 mController;
    @Mock
    private WifiEntry mWifiEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new WifiAutoConnectPreferenceController2(RuntimeEnvironment.application);
        mController.setWifiEntry(mWifiEntry);
    }

    @Test
    public void getAvailabilityStatus_shouldFollowCanSetAutoJoinEnabled() {
        // Test able to set auto join.
        when(mWifiEntry.canSetAutoJoinEnabled()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Test not able to set auto join.
        when(mWifiEntry.canSetAutoJoinEnabled()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_shouldFollowIsAutoJoinEnabled() {
        // Test auto join enabled.
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();

        // Test auto join disabled.
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_shouldSetAutoJoinEnabled() {
        // Test checked.
        mController.setChecked(true);

        verify(mWifiEntry).setAutoJoinEnabled(true);

        // Test unchecked.
        mController.setChecked(false);

        verify(mWifiEntry).setAutoJoinEnabled(false);
    }
}
