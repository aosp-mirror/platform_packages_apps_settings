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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SubscriptionUtilTest {
    @Mock
    private Context mContext;
    @Mock
    private SubscriptionManager mSubMgr;
    @Mock
    private TelephonyManager mTelMgr;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mSubMgr).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelMgr).when(mContext).getSystemService(TelephonyManager.class);
        when(mTelMgr.getUiccSlotsInfo()).thenReturn(null);
    }

    @Test
    public void getAvailableSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getAvailableSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getAvailableSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Test
    public void getActiveSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getActiveSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getActiveSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Test
    public void isInactiveInsertedPSim_nullSubInfo_doesNotCrash() {
        assertThat(SubscriptionUtil.isInactiveInsertedPSim(null)).isFalse();
    }
}
