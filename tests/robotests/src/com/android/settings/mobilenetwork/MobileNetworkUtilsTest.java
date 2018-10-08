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

package com.android.settings.mobilenetwork;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(SettingsRobolectricTestRunner.class)
public class MobileNetworkUtilsTest {
    private static final int SUB_ID_1 = 1;
    private static final int SUB_ID_2 = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mTelephonyManager2;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID_1);
        doReturn(mTelephonyManager2).when(mTelephonyManager).createForSubscriptionId(SUB_ID_2);

        doReturn(SUB_ID_1).when(mSubscriptionInfo1).getSubscriptionId();
        doReturn(SUB_ID_2).when(mSubscriptionInfo2).getSubscriptionId();

        doReturn(Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2)).when(
                mSubscriptionManager).getActiveSubscriptionInfoList();
    }

    @Test
    public void setMobileDataEnabled_setEnabled_enabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, false);

        verify(mTelephonyManager).setDataEnabled(true);
        verify(mTelephonyManager2, never()).setDataEnabled(anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_setDisabled_disabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_2, true, false);

        verify(mTelephonyManager2).setDataEnabled(true);
        verify(mTelephonyManager, never()).setDataEnabled(anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_disableOtherSubscriptions() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, true);

        verify(mTelephonyManager).setDataEnabled(true);
        verify(mTelephonyManager2).setDataEnabled(false);
    }
}
