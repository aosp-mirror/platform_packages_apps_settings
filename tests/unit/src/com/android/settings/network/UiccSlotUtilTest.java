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

package com.android.settings.network;

import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UiccSlotUtilTest {
    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private static final int ESIM_PHYSICAL_SLOT = 0;
    private static final int PSIM_PHYSICAL_SLOT = 1;

    private List<SubscriptionInfo> mSubscriptionInfoList = new ArrayList<>();
    private List<UiccCardInfo> mUiccCardInfo = new ArrayList<>();
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getUiccCardsInfo()).thenReturn(mUiccCardInfo);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(mSubscriptionInfoList);
    }

    @Test
    public void getSlotInfos_oneSimSlotDevice_returnTheCorrectSlotInfoList() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSimSlotDeviceActivePsim());
        ImmutableList<UiccSlotInfo> testUiccSlotInfos =
                UiccSlotUtil.getSlotInfos(mTelephonyManager);

        assertThat(testUiccSlotInfos.size()).isEqualTo(1);
    }

    @Test
    public void getSlotInfos_twoSimSlotsDevice_returnTheCorrectSlotInfoList() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActivePsimActiveEsim());
        ImmutableList<UiccSlotInfo> testUiccSlotInfos =
                UiccSlotUtil.getSlotInfos(mTelephonyManager);

        assertThat(testUiccSlotInfos.size()).isEqualTo(2);
    }

    @Test
    public void getEsimSlotId_twoSimSlotsDeviceAndEsimIsSlot0_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActiveEsimActivePsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext,0);

        assertThat(testSlot).isEqualTo(0);
    }

    @Test
    public void getEsimSlotId_simIsRemovableEsimAndRemovableEsimIsSlot1_returnRemovableEsimSlot1() {
        int subId = 0;
        int cardId = 0;
        mSubscriptionInfoList.add(createSubscriptionInfo(subId,-1, -1, true, cardId));
        mUiccCardInfo.add(createUiccCardInfo(true, 3, 0, false, -1, -1));
        mUiccCardInfo.add(createUiccCardInfo(true, cardId, 1, true, -1, -1));
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActiveEsimActiveRemovableEsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext, subId);

        assertThat(testSlot).isEqualTo(1);
    }

    @Test
    public void getEsimSlotId_simIsRemovableEsimAndTwoRemovableSlots_returnRemovableEsimSlot1() {
        int subId = 0;
        int cardId = 0;
        mSubscriptionInfoList.add(createSubscriptionInfo(subId,-1, -1, true, cardId));
        mUiccCardInfo.add(createUiccCardInfo(false, 4, 0, true, -1, -1));
        mUiccCardInfo.add(createUiccCardInfo(true, cardId, 1, true, -1, -1));
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActivePsimActiveRemovableEsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext, subId);

        assertThat(testSlot).isEqualTo(1);
    }

    @Test
    public void getEsimSlotId_twoSimSlotsDeviceAndEsimIsSlot1_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActivePsimActiveEsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext,0);

        assertThat(testSlot).isEqualTo(1);
    }

    @Test
    public void getEsimSlotId_noEimSlotDevice_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                oneSimSlotDeviceActivePsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext,0);

        assertThat(testSlot).isEqualTo(-1);
    }

    @Test
    public void prepareUiccSlotMappings_fromPsimActiveToEsimPort0Active_esimPort0Active() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingSsModePsimActive();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingSsModeEsimPort0Active();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromEsimPort0ActiveToPsimActive_psimActive() {
        Collection<UiccSlotMapping> uiccSlotMappings =
                createUiccSlotMappingSsModeEsimPort0Active();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingSsModePsimActive();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromPsimAndPort0ToPsimAndPort1_psimAndPort1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort1();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 1, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromPsimAndPort1ToPsimAndPort0_psimAndPort0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort0();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromPsimAndPort0ToDualPortsB_dualPortsB() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingDualPortsB();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 1, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromPsimAndPort1ToDualPortsA_dualPortsA() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingDualPortsA();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_noPsimAndFromPsimAndPort0ToDualPortsB_dualPortsB() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingDualPortsB();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 1, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_noPsimAndFromPsimAndPort1ToDualPortsA_dualPortsA() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingDualPortsA();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, false, ESIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_oneEsimAndFromDualPortsAToPsimAndPort1_psimAndPort1() {
        // There is only one enabled esimPort1 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort1();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_oneEsimAndFromDualPortsAToPsimAndPort0_psimAndPort0() {
        // There is only one enabled esimPort0 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort0();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_oneEsimAndFromDualPortsBToPsimAndPort1_psimAndPort1() {
        // There is only one enabled esimPort1 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort1();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_oneEsimAndFromDualPortsBToPsimAndPort0_psimAndPort0() {
        // There is only one enabled esimPort0 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort0();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromDualPortsAToPsimAndPort1_psimAndPort1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort1();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromDualPortsAToPsimAndPort0_psimAndPort0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort0();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromDualPortsBToPsimAndPort1_psimAndPort1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort1();
        int removedLogicalSlotIndex = 1;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void prepareUiccSlotMappings_fromDualPortsBToPsimAndPort0_psimAndPort0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<UiccSlotMapping> verifyUiccSlotMappings =
                createUiccSlotMappingPsimAndPort0();
        int removedLogicalSlotIndex = 0;

        Collection<UiccSlotMapping> testUiccSlotMappings = UiccSlotUtil.prepareUiccSlotMappings(
                uiccSlotMappings, true, PSIM_PHYSICAL_SLOT, 0, removedLogicalSlotIndex);

        compareTwoUiccSlotMappings(testUiccSlotMappings, verifyUiccSlotMappings);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromPsimActiveToEsimPort0Active_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingSsModePsimActive();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(0, 0);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, false);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromEsimPort0ActiveToPsimActive_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingSsModeEsimPort0Active();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(0, 0);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, false);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromPsimAndPort0ToPsimAndPort1_logicalSlot1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 0);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(1, 0);
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromPsimAndPort1ToPsimAndPort0_logicalSlot1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 1);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(1, 1);
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromPsimAndPort0ToDualPortsB_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 0);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(0, 0);
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromPsimAndPort1ToDualPortsA_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 1);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(0, 0);
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_noPsimAndFromPsimAndPort0ToDualPortsB_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort0();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(1, 0);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_noPsimAndFromPsimAndPort1ToDualPortsA_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingPsimAndPort1();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(1, 1);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_oneEsimAndFromDualPortsAToPsimAndPort1_logicalSlot0() {
        // There is only one enabled esimPort1 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(1, 1);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_oneEsimAndFromDualPortsAToPsimAndPort0_logicalSlot1() {
        // There is only one enabled esimPort0 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(0, 0);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_oneEsimAndFromDualPortsBToPsimAndPort1_logicalSlot1() {
        // There is only one enabled esimPort1 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(0, 1);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_oneEsimAndFromDualPortsBToPsimAndPort0_logicalSlot0() {
        // There is only one enabled esimPort0 before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListOneSim(1, 0);
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromDualPortsAToPsimAndPort1_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 1);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(0, 0);
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromDualPortsAToPsimAndPort0_logicalSlot1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 0, 1, 1);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(1, 1);
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromDualPortsBToPsimAndPort1_logicalSlot1() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 1, 1, 0);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(1, 0);
        int verifyExcludedLogicalSlotIndex = 1;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_fromDualPortsBToPsimAndPort0_logicalSlot0() {
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<SubscriptionInfo> activeSubscriptionInfoList =
                createActiveSubscriptionInfoListTwoSims(0, 1, 1, 0);
        SubscriptionInfo removedSubInfo = createSubscriptionInfo(0, 1);
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_noEsimAndFromDualPortsAToPsimAndPort1_logicalSlot0() {
        // There is no profiles enabled on either esim port before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsA();
        Collection<SubscriptionInfo> activeSubscriptionInfoList = new ArrayList<>();
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_noEsimAndFromDualPortsBToPsimAndPort0_logicalSlot0() {
        // There is no profiles enabled on either esim port before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsB();
        Collection<SubscriptionInfo> activeSubscriptionInfoList = new ArrayList<>();
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void getExcludedLogicalSlotIndex_noEsimNoOrdingFromDualPortsBToPsimAndPort1_logical0() {
        // There is no profiles enabled on either esim port before user enables the psim.
        Collection<UiccSlotMapping> uiccSlotMappings = createUiccSlotMappingDualPortsBNoOrding();
        Collection<SubscriptionInfo> activeSubscriptionInfoList = new ArrayList<>();
        SubscriptionInfo removedSubInfo = null;
        int verifyExcludedLogicalSlotIndex = 0;

        int testExcludedLogicalSlotIndex = UiccSlotUtil.getExcludedLogicalSlotIndex(
                uiccSlotMappings, activeSubscriptionInfoList, removedSubInfo, true);

        assertThat(testExcludedLogicalSlotIndex).isEqualTo(verifyExcludedLogicalSlotIndex);
    }

    @Test
    public void isRemovableSimEnabled_noPsim_returnsFalse() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                oneSimSlotDeviceActiveEsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isFalse();
    }

    @Test
    public void isRemovableSimEnabled_activeRemovableEsimAndInactivePsim_returnsFalse() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActiveRemovableEsimInactivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isFalse();
    }

    @Test
    public void isRemovableSimEnabled_activeRemovableEsimAndActivePsim_returnsTrue() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActivePsimActiveRemovableEsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isTrue();
    }

    @Test
    public void isRemovableSimEnabled_inactiveRemovableEsimAndActivePsim_returnsTrue() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceInactiveRemovableEsimActivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isTrue();
    }

    @Test
    public void isRemovableSimEnabled_twoActiveRemovableEsimsAndInactivePsim_returnsFalse() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceTwoActiveRemovableEsimsInactivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isFalse();
    }

    @Test
    public void isRemovableSimEnabled_oneActiveOneInactiveRemovableEsimActivePsim_returnsTrue() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceOneActiveOneInactiveRemovableEsimsActivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isTrue();
    }

    @Test
    public void isRemovableSimEnabled_activePsim_returnsTrue() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                oneSimSlotDeviceActivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isTrue();
    }

    @Test
    public void isRemovableSimEnabled_inactivePsim_returnsFalse() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                oneSimSlotDeviceinactivePsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isFalse();
    }

    @Test
    public void isRemovableSimEnabled_activeEsimAndActivePsim_returnsTrue() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceActivePsimActiveEsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isTrue();
    }

    @Test
    public void isRemovableSimEnabled_activeEsimAndInactivePsim_returnsFalse() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDeviceInactivePsimActiveEsim());

        boolean testSlot = UiccSlotUtil.isRemovableSimEnabled(mTelephonyManager);

        assertThat(testSlot).isFalse();
    }

    private void compareTwoUiccSlotMappings(Collection<UiccSlotMapping> testUiccSlotMappings,
            Collection<UiccSlotMapping> verifyUiccSlotMappings) {
        assertThat(testUiccSlotMappings.size()).isEqualTo(verifyUiccSlotMappings.size());
        Iterator<UiccSlotMapping> testIterator = testUiccSlotMappings.iterator();
        Iterator<UiccSlotMapping> verifyIterator = verifyUiccSlotMappings.iterator();
        while (testIterator.hasNext()) {
            UiccSlotMapping testUiccSlotMapping = testIterator.next();
            UiccSlotMapping verifyUiccSlotMapping = verifyIterator.next();
            assertThat(testUiccSlotMapping.getLogicalSlotIndex()).isEqualTo(
                    verifyUiccSlotMapping.getLogicalSlotIndex());
            assertThat(testUiccSlotMapping.getPortIndex()).isEqualTo(
                    verifyUiccSlotMapping.getPortIndex());
            assertThat(testUiccSlotMapping.getPhysicalSlotIndex()).isEqualTo(
                    verifyUiccSlotMapping.getPhysicalSlotIndex());
        }
    }

    private SubscriptionInfo createSubscriptionInfo(int logicalSlotIndex, int portIndex) {
        return createSubscriptionInfo(0, logicalSlotIndex, portIndex, true, 25);
    }

    private SubscriptionInfo createSubscriptionInfo(int subId, int logicalSlotIndex, int portIndex,
            boolean isEmbedded, int cardId) {
        return new SubscriptionInfo(
                subId, "",
                logicalSlotIndex, "", "", 0, 0, "", 0, null, "", "", "",
                isEmbedded /* isEmbedded */,
                null, "",
                cardId,
                false, null, false, 0, 0, 0, null, null, true, portIndex);
    }

    private UiccCardInfo createUiccCardInfo(boolean isEuicc, int cardId, int physicalSlotIndex,
            boolean isRemovable, int logicalSlotIndex, int portIndex) {
        return new UiccCardInfo(
                isEuicc /* isEuicc */,
                cardId /* cardId */,
                null /* eid */,
                physicalSlotIndex /* physicalSlotIndex */,
                isRemovable /* isRemovable */,
                false /* isMultipleEnabledProfileSupported */,
                Collections.singletonList(
                        new UiccPortInfo(
                                "123451234567890" /* iccId */,
                                portIndex /* portIdx */,
                                logicalSlotIndex /* logicalSlotIdx */,
                                true /* isActive */)
                ));
    }

    private List<SubscriptionInfo> createActiveSubscriptionInfoListOneSim(int logicalSlotIndex,
            int portIndex) {
        List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        subscriptionInfoList.add(createSubscriptionInfo(logicalSlotIndex, portIndex));

        return subscriptionInfoList;
    }

    private List<SubscriptionInfo> createActiveSubscriptionInfoListTwoSims(int logicalSlotIndex1,
            int portIndex1, int logicalSlotIndex2, int portIndex2) {
        List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        subscriptionInfoList.add(createSubscriptionInfo(logicalSlotIndex1, portIndex1));
        subscriptionInfoList.add(createSubscriptionInfo(logicalSlotIndex2, portIndex2));
        return subscriptionInfoList;
    }

    // Device |                                        |Slot   |
    // Working|                                        |Mapping|
    // State  |Type                                    |Mode   |Friendly name
    //--------------------------------------------------------------------------
    // Single |SIM pSIM [RIL 0]                        |1      |pSIM active
    // Single |SIM MEP Port #0 [RIL0]                  |2      |eSIM Port0 active
    // Single |SIM MEP Port #1 [RIL0]                  |2.1    |eSIM Port1 active
    // DSDS   |pSIM [RIL 0] + MEP Port #0 [RIL 1]      |3      |pSIM+Port0
    // DSDS   |pSIM [RIL 0] + MEP Port #1 [RIL 1]      |3.1    |pSIM+Port1
    // DSDS   |MEP Port #0 [RIL 0] + MEP Port #1 [RIL1]|3.2    |Dual-Ports-A
    // DSDS   |MEP Port #1 [RIL 0] + MEP Port #0 [RIL1]|4      |Dual-Ports-B
    private List<UiccSlotMapping> createUiccSlotMappingSsModePsimActive() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, PSIM_PHYSICAL_SLOT, 0));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingSsModeEsimPort0Active() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, ESIM_PHYSICAL_SLOT, 0));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingSsModeEsimPort1Active() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(1, ESIM_PHYSICAL_SLOT, 0));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingPsimAndPort0() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, PSIM_PHYSICAL_SLOT, 0));
        slotMap.add(new UiccSlotMapping(0, ESIM_PHYSICAL_SLOT, 1));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingPsimAndPort1() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, PSIM_PHYSICAL_SLOT, 0));
        slotMap.add(new UiccSlotMapping(1, ESIM_PHYSICAL_SLOT, 1));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingDualPortsA() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, ESIM_PHYSICAL_SLOT, 0));
        slotMap.add(new UiccSlotMapping(1, ESIM_PHYSICAL_SLOT, 1));

        return slotMap;
    }

    private List<UiccSlotMapping> createUiccSlotMappingDualPortsB() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(1, ESIM_PHYSICAL_SLOT, 0));
        slotMap.add(new UiccSlotMapping(0, ESIM_PHYSICAL_SLOT, 1));

        return slotMap;
    }
    private List<UiccSlotMapping> createUiccSlotMappingDualPortsBNoOrding() {
        List<UiccSlotMapping> slotMap = new ArrayList<>();
        slotMap.add(new UiccSlotMapping(0, ESIM_PHYSICAL_SLOT, 1));
        slotMap.add(new UiccSlotMapping(1, ESIM_PHYSICAL_SLOT, 0));

        return slotMap;
    }
    /**
     * The "oneSimSlotDevice" has below cases
     * 1) The device is one psim slot and no esim slot
     * 2) The device is no psim slot and one esim slot. like the watch.
     *
     * The "twoSimsSlotDevice" has below cases
     * 1) The device is one psim slot and one esim slot
     * 2) The device is two psim slots
     */

    private UiccSlotInfo[] oneSimSlotDeviceActivePsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(false, true, 0, true)};
    }

    private UiccSlotInfo[] oneSimSlotDeviceActiveEsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] oneSimSlotDeviceinactivePsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(false, true, -1, false)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActivePsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, true),
                createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActiveEsimActiveRemovableEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(true, false, 0, true),
                createUiccSlotInfo(true, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActivePsimActiveRemovableEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, true),
                createUiccSlotInfo(true, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActiveRemovableEsimInactivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(true, true, 0, true),
                createUiccSlotInfo(false, true, -1, false)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceInactiveRemovableEsimActivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(true, true, -1, false),
                createUiccSlotInfo(false, true, 0, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceTwoActiveRemovableEsimsInactivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfoForRemovableEsimMep(0, true, 1, true),
                createUiccSlotInfo(false, true, -1, false)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceOneActiveOneInactiveRemovableEsimsActivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfoForRemovableEsimMep(1, true, -1, false),
                createUiccSlotInfo(false, true, 0, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActiveEsimActivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(true, false, 0, true),
                createUiccSlotInfo(false, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceTwoActiveEsims() {
        // device supports MEP, so device can enable two esims.
        // If device has psim slot, the UiccSlotInfo of psim always be in UiccSlotInfo[].
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, -1, true),
                createUiccSlotInfoForEsimMep(0, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceActivePsimInactiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, true),
                createUiccSlotInfo(true, false, -1, false)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceInactivePsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, false),
                createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDeviceNoInsertPsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, -1, false),
                createUiccSlotInfo(true, false, 1, true)};
    }
    //ToDo: add more cases.

    private UiccSlotInfo createUiccSlotInfo(boolean isEuicc, boolean isRemovable,
            int logicalSlotIdx, boolean isActive) {
        return new UiccSlotInfo(
                isEuicc, /* isEuicc */
                "123", /* cardId */
                CARD_STATE_INFO_PRESENT, /* cardStateInfo */
                true, /* isExtendApduSupported */
                isRemovable, /* isRemovable */
                Collections.singletonList(
                        new UiccPortInfo("" /* iccId */, 0 /* portIdx */,
                                logicalSlotIdx /* logicalSlotIdx */, isActive /* isActive */))
        );
    }

    private UiccSlotInfo createUiccSlotInfoForEsimMep(int logicalSlotIdx1, boolean isActiveEsim1,
            int logicalSlotIdx2, boolean isActiveEsim2) {
        return new UiccSlotInfo(
                true, /* isEuicc */
                "123", /* cardId */
                CARD_STATE_INFO_PRESENT, /* cardStateInfo */
                true, /* isExtendApduSupported */
                false, /* isRemovable */
                Arrays.asList(
                        new UiccPortInfo("" /* iccId */, 0 /* portIdx */,
                                logicalSlotIdx1 /* logicalSlotIdx */, isActiveEsim1 /* isActive */),
                        new UiccPortInfo("" /* iccId */, 1 /* portIdx */,
                                logicalSlotIdx2 /* logicalSlotIdx */,
                                isActiveEsim2 /* isActive */)));
    }

    private UiccSlotInfo createUiccSlotInfoForRemovableEsimMep(int logicalSlotIdx1,
            boolean isActiveEsim1, int logicalSlotIdx2, boolean isActiveEsim2) {
        return new UiccSlotInfo(
                true, /* isEuicc */
                "123", /* cardId */
                CARD_STATE_INFO_PRESENT, /* cardStateInfo */
                true, /* isExtendApduSupported */
                true, /* isRemovable */
                Arrays.asList(
                        new UiccPortInfo("" /* iccId */, 0 /* portIdx */,
                                logicalSlotIdx1 /* logicalSlotIdx */, isActiveEsim1 /* isActive */),
                        new UiccPortInfo("" /* iccId */, 1 /* portIdx */,
                                logicalSlotIdx2 /* logicalSlotIdx */,
                                isActiveEsim2 /* isActive */)));
    }
}
