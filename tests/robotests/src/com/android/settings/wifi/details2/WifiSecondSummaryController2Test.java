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
public class WifiSecondSummaryController2Test {

    private WifiSecondSummaryController2 mController;
    @Mock
    private WifiEntry mWifiEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new WifiSecondSummaryController2(RuntimeEnvironment.application);
    }

    @Test
    public void getAvailabilityStatus_showWhenSummaryAvailable() {
        // Visible when summary is not empty.
        when(mWifiEntry.getSecondSummary()).thenReturn("test");
        mController.setWifiEntry(mWifiEntry);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Invisible when summary is empty.
        when(mWifiEntry.getSecondSummary()).thenReturn("");
        mController.setWifiEntry(mWifiEntry);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
