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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NrDisabledInDsdsFooterPreferenceControllerTest {
    private static final String PREF_KEY = "pref_key";
    private static final int SUB_ID = 111;

    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    private NrDisabledInDsdsFooterPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(anyInt());
        mController = new NrDisabledInDsdsFooterPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isAvailable_noInit_notAvailable() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Ignore
    public void isAvailable_dataOnAndDsdsAnd5GSupported_Available() {
        when(mTelephonyManager.getSupportedRadioAccessFamily())
                .thenReturn(TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        when(mSubscriptionManager.getActiveSubscriptionIdList()).thenReturn(new int[] {1, 2});
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        mController.init(SUB_ID);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_5gNotSupported_notAvailable() {
        when(mTelephonyManager.getSupportedRadioAccessFamily())
                .thenReturn(TelephonyManager.NETWORK_TYPE_BITMASK_LTE);
        when(mSubscriptionManager.getActiveSubscriptionIdList()).thenReturn(new int[] {1, 2});
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        mController.init(SUB_ID);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_mobileDataOff_notAvailable() {
        when(mTelephonyManager.getSupportedRadioAccessFamily())
                .thenReturn(TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        when(mSubscriptionManager.getActiveSubscriptionIdList()).thenReturn(new int[] {1, 2});
        when(mTelephonyManager.isDataEnabled()).thenReturn(false);
        mController.init(SUB_ID);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_singleSimMode_notAvailable() {
        when(mTelephonyManager.getSupportedRadioAccessFamily())
                .thenReturn(TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        when(mSubscriptionManager.getActiveSubscriptionIdList()).thenReturn(new int[] {1});
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        mController.init(SUB_ID);
        assertThat(mController.isAvailable()).isFalse();
    }
}
