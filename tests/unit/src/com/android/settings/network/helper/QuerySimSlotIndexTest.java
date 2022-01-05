/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.network.helper;

import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.utils.ThreadUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;

@RunWith(AndroidJUnit4.class)
public class QuerySimSlotIndexTest {
    private static final String TAG = "QSSI_Test";

    @Mock
    private TelephonyManager mTelephonyManager;

    Future<AtomicIntegerArray> mActiveSimSlotIndex;
    Future<AtomicIntegerArray> mAllSimSlotIndex;

    @Before
    public void setUp() {
        // query in background thread
        mAllSimSlotIndex = ThreadUtils.postOnBackgroundThread(
                new QuerySimSlotIndex(mTelephonyManager, true, true));
        // query in background thread
        mActiveSimSlotIndex = ThreadUtils.postOnBackgroundThread(
                new QuerySimSlotIndex(mTelephonyManager, false, true));
    }

    @Test
    public void allSimSlotIndexCall_nullInput_getNoneNullEmptyList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(null);
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(0);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_oneSimAndActivePsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSim_ActivePsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(0);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_oneSimAndActiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSim_ActiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_twoSimsAndActivePsimActiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActivePsimActiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_twoSimsAndtwoActiveEsims_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_twoActiveEsims());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_twoSimsAndActivePsimInactiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActivePsimInactiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void allSimSlotIndexCall_twoSimsAndActiveEsimInactivePsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActiveEsimInactivePsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_nullInput_getNoneNullEmptyList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(null);
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(0);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_oneSimAndActivePsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSim_ActivePsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(0);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_oneSimAndActiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(oneSim_ActiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_twoSimsAndActivePsimActiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActivePsimActiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_twoSimsAndtwoActiveEsims_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_twoActiveEsims());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0)).isEqualTo(0);
            assertThat(result.get(1)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_twoSimsAndActivePsimInactiveEsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActivePsimInactiveEsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(0);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    @Test
    public void activeSimSlotIndexCall_twoSimsAndActiveEsimInactivePsim_getList() {
        try {
            when(mTelephonyManager.getUiccSlotsInfo()).thenReturn(twoSims_ActiveEsimInactivePsim());
            List<Integer> result = SelectableSubscriptions.atomicToList(mActiveSimSlotIndex.get());

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(1);
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
    }

    private UiccSlotInfo[] oneSim_ActivePsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(false, 0, true)};
    }

    private UiccSlotInfo[] oneSim_ActiveEsim() {
        return new UiccSlotInfo[]{createUiccSlotInfo(true, 1, true)};
    }

    private UiccSlotInfo[] twoSims_ActivePsimActiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, 0, true),
                createUiccSlotInfo(true, 1, true)};
    }

    private UiccSlotInfo[] twoSims_twoActiveEsims() {
        return new UiccSlotInfo[]{
                createUiccSlotInfoForTwoEsim(true, true)};
    }

    private UiccSlotInfo[] twoSims_ActivePsimInactiveEsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, 0, true),
                createUiccSlotInfo(true, 1, false)};
    }

    private UiccSlotInfo[] twoSims_ActiveEsimInactivePsim() {
        return new UiccSlotInfo[]{
                createUiccSlotInfo(false, 0, false),
                createUiccSlotInfo(true, 1, true)};
    }

    //ToDo: add more cases.
    /*
    private List<UiccSlotInfo> threeSims_ActivePsimTwoinactiveEsim(){
    }
    private List<UiccSlotInfo> threeSims_twoActiveEsimsInactivePsim(){
    }
    private List<UiccSlotInfo> threeSims_ActiveEsimInactivePsimInactiveEsim(){
    }
    private List<UiccSlotInfo> threeSims_ActivePsimActiveEsimInactiveEsim(){
    }
    */

    private UiccSlotInfo createUiccSlotInfo(boolean isEuicc, int logicalSlotIdx,
            boolean isActive) {
        return new UiccSlotInfo(
                isEuicc, /* isEuicc */
                "123", /* cardId */
                CARD_STATE_INFO_PRESENT, /* cardStateInfo */
                true, /* isExtendApduSupported */
                true, /* isRemovable */
                Collections.singletonList(
                        new UiccPortInfo("" /* iccId */, 0 /* portIdx */,
                                logicalSlotIdx /* logicalSlotIdx */, isActive /* isActive */))
        );
    }

    private UiccSlotInfo createUiccSlotInfoForTwoEsim(boolean isActiveEsim1,
            boolean isActiveEsim2) {
        return new UiccSlotInfo(
                true, /* isEuicc */
                "123", /* cardId */
                CARD_STATE_INFO_PRESENT, /* cardStateInfo */
                true, /* isExtendApduSupported */
                true, /* isRemovable */
                Arrays.asList(
                        new UiccPortInfo("" /* iccId */, 0 /* portIdx */,
                                0 /* logicalSlotIdx */, isActiveEsim1 /* isActive */),
                        new UiccPortInfo("" /* iccId */, 1 /* portIdx */,
                                1 /* logicalSlotIdx */, isActiveEsim2 /* isActive */))
        );
    }
}
