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

import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_ABSENT;
import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
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
        when(mSubMgr.getSelectableSubscriptionInfoList()).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getAvailableSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getAvailableSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Test
    public void getAvailableSubscriptions_oneSelectableOneDisabledPSim_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);

        when(info1.getSubscriptionId()).thenReturn(111);
        when(info1.getSimSlotIndex()).thenReturn(-1);
        when(info1.getCardString()).thenReturn("info1_cardid");

        when(info2.getSubscriptionId()).thenReturn(222);
        when(info2.getSimSlotIndex()).thenReturn(0);
        when(info2.getCardString()).thenReturn("info2_cardid");

        when(mSubMgr.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info1));
        when(mSubMgr.getAllSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));

        final UiccSlotInfo info2slot = mock(UiccSlotInfo.class);
        when(info2slot.getCardStateInfo()).thenReturn(CARD_STATE_INFO_PRESENT);
        when(info2slot.getLogicalSlotIdx()).thenReturn(0);
        when(info2slot.getCardId()).thenReturn("info2_cardid");

        final UiccSlotInfo[] slotInfos = {info2slot};
        when(mTelMgr.getUiccSlotsInfo()).thenReturn(slotInfos);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).hasSize(2);
        assertThat(subs.get(0).getSubscriptionId()).isEqualTo(111);
        assertThat(subs.get(1).getSubscriptionId()).isEqualTo(222);
    }


    @Test
    public void getAvailableSubscriptions_oneSelectableTwoDisabledPSimsOneAbsent_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info3 = mock(SubscriptionInfo.class);

        when(info1.getSubscriptionId()).thenReturn(111);
        when(info1.getSimSlotIndex()).thenReturn(-1);
        when(info1.getCardString()).thenReturn("info1_cardid");

        when(info2.getSubscriptionId()).thenReturn(222);
        when(info2.getSimSlotIndex()).thenReturn(-1);
        when(info2.getCardString()).thenReturn("info2_cardid");

        when(info3.getSubscriptionId()).thenReturn(333);
        when(info3.getSimSlotIndex()).thenReturn(0);
        when(info3.getCardString()).thenReturn("info3_cardid");

        when(mSubMgr.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info1));
        when(mSubMgr.getAllSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2, info3));

        final UiccSlotInfo info2slot = mock(UiccSlotInfo.class);
        final UiccSlotInfo info3slot = mock(UiccSlotInfo.class);

        when(info2slot.getLogicalSlotIdx()).thenReturn(-1);
        when(info2slot.getCardStateInfo()).thenReturn(CARD_STATE_INFO_ABSENT);
        when(info2slot.getCardId()).thenReturn("info2_cardid");

        when(info3slot.getLogicalSlotIdx()).thenReturn(0);
        when(info3slot.getCardStateInfo()).thenReturn(CARD_STATE_INFO_PRESENT);
        when(info3slot.getCardId()).thenReturn("info3_cardid");

        final UiccSlotInfo[] slotInfos = {info2slot, info3slot};
        when(mTelMgr.getUiccSlotsInfo()).thenReturn(slotInfos);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        assertThat(subs).hasSize(2);
        assertThat(subs.get(0).getSubscriptionId()).isEqualTo(111);
        assertThat(subs.get(1).getSubscriptionId()).isEqualTo(333);
    }

    @Test
    public void getActiveSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mSubMgr.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getActiveSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getActiveSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(
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
