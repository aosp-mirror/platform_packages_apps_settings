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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiSubscriptionDetailPreferenceController2Test {

    @Mock
    private WifiEntry mMockWifiEntry;

    private WifiSubscriptionDetailPreferenceController2 mPreferenceController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mMockWifiEntry = mock(WifiEntry.class);
        WifiSubscriptionDetailPreferenceController2 preferenceController =
                new WifiSubscriptionDetailPreferenceController2(mContext);
        preferenceController.setWifiEntry(mMockWifiEntry);
        mPreferenceController = spy(preferenceController);
    }

    @Test
    public void testUpdateState_canSetPrivacy_shouldBeSelectable() {
        when(mMockWifiEntry.canManageSubscription()).thenReturn(true);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateState_canNotSetPrivacy_shouldNotSelectable() {
        when(mMockWifiEntry.canManageSubscription()).thenReturn(false);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }
}
