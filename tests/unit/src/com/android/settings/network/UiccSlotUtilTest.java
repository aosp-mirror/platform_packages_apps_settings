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
import android.telephony.TelephonyManager;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class UiccSlotUtilTest {
    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
    }

    @Test
    public void getSlotInfos_oneSimSlotDevice_returnTheCorrectSlotInfoList() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSimSlotDevice_ActivePsim());
        ImmutableList<UiccSlotInfo> testUiccSlotInfos =
                UiccSlotUtil.getSlotInfos(mTelephonyManager);

        assertThat(testUiccSlotInfos.size()).isEqualTo(1);
    }

    @Test
    public void getSlotInfos_twoSimSlotsDevice_returnTheCorrectSlotInfoList() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDevice_ActivePsimActiveEsim());
        ImmutableList<UiccSlotInfo> testUiccSlotInfos =
                UiccSlotUtil.getSlotInfos(mTelephonyManager);

        assertThat(testUiccSlotInfos.size()).isEqualTo(2);
    }

    @Test
    public void getEsimSlotId_twoSimSlotsDeviceAndEsimIsSlot0_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDevice_ActiveEsimActivePsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext);

        assertThat(testSlot).isEqualTo(0);
    }

    @Test
    public void getEsimSlotId_twoSimSlotsDeviceAndEsimIsSlot1_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                twoSimSlotsDevice_ActivePsimActiveEsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext);

        assertThat(testSlot).isEqualTo(1);
    }

    @Test
    public void getEsimSlotId_noEimSlotDevice_returnTheCorrectEsimSlot() {
        when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(
                oneSimSlotDevice_ActivePsim());
        int testSlot = UiccSlotUtil.getEsimSlotId(mContext);

        assertThat(testSlot).isEqualTo(-1);
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

    private UiccSlotInfo[] oneSimSlotDevice_ActivePsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(false, true, 0, true)};
    }

    private UiccSlotInfo[] oneSimSlotDevice_ActiveEsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_ActivePsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, true),
                createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_ActiveEsimActivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(true, false, 0, true),
                createUiccSlotInfo(false, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_twoActiveEsims() {
        // device supports MEP, so device can enable two esims.
        // If device has psim slot, the UiccSlotInfo of psim always be in UiccSlotInfo[].
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, -1, true),
                createUiccSlotInfoForEsimMep(0, true, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_ActivePsimInactiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, true),
                createUiccSlotInfo(true, false, -1, false)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_InactivePsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, true, 0, false),
                createUiccSlotInfo(true, false, 1, true)};
    }

    private UiccSlotInfo[] twoSimSlotsDevice_NoInsertPsimActiveEsim() {
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
}
