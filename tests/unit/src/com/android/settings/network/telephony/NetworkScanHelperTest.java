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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.telephony.AccessNetworkConstants;
import android.telephony.CellInfo;
import android.telephony.ModemInfo;
import android.telephony.NetworkScan;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneCapability;
import android.telephony.RadioAccessSpecifier;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyScanManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class NetworkScanHelperTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private List<CellInfo> mCellInfos;
    @Mock
    private NetworkScanHelper.NetworkScanCallback mNetworkScanCallback;

    private static final long THREAD_EXECUTION_TIMEOUT_MS = 3000L;

    private ExecutorService mNetworkScanExecutor;
    private NetworkScanHelper mNetworkScanHelper;

    private static final int SCAN_ID = 1234;
    private static final int SUB_ID = 1;

    private NetworkScan mNetworkScan;

    public class NetworkScanMock extends NetworkScan {
        NetworkScanMock(int scanId, int subId) {
            super(scanId, subId);
        }

        @Override
        public void stopScan() {
            return;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mNetworkScanExecutor = Executors.newFixedThreadPool(1);

        mNetworkScanHelper = new NetworkScanHelper(mTelephonyManager,
                mNetworkScanCallback, mNetworkScanExecutor);

        mNetworkScan = spy(new NetworkScanMock(SCAN_ID, SUB_ID));
    }

    @Test
    public void startNetworkScan_incrementalAndSuccess_completionWithResult() {
        when(mCellInfos.size()).thenReturn(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TelephonyScanManager.NetworkScanCallback callback =
                        (TelephonyScanManager.NetworkScanCallback)
                        (invocation.getArguments()[2]);
                callback.onResults(mCellInfos);
                callback.onComplete();
                return mNetworkScan;
            }
        }).when(mTelephonyManager).requestNetworkScan(
                any(NetworkScanRequest.class), any(Executor.class),
                any(TelephonyScanManager.NetworkScanCallback.class));

        ArgumentCaptor<List<CellInfo>> argument = ArgumentCaptor.forClass(List.class);

        startNetworkScan_incremental(true);

        verify(mNetworkScanCallback, times(1)).onResults(argument.capture());
        List<CellInfo> actualResult = argument.getValue();
        assertThat(actualResult.size()).isEqualTo(mCellInfos.size());
        verify(mNetworkScanCallback, times(1)).onComplete();
    }

    @Test
    public void startNetworkScan_incrementalAndImmediateFailure_failureWithErrorCode() {
        doReturn(null).when(mTelephonyManager).requestNetworkScan(
                any(NetworkScanRequest.class), any(Executor.class),
                any(TelephonyScanManager.NetworkScanCallback.class));

        startNetworkScan_incremental(true);

        verify(mNetworkScanCallback, times(1)).onError(anyInt());
    }

    @Test
    public void startNetworkScan_incrementalAndFailure_failureWithErrorCode() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TelephonyScanManager.NetworkScanCallback callback =
                        (TelephonyScanManager.NetworkScanCallback)
                        (invocation.getArguments()[2]);
                callback.onError(NetworkScan.ERROR_MODEM_ERROR);
                return mNetworkScan;
            }
        }).when(mTelephonyManager).requestNetworkScan(
                any(NetworkScanRequest.class), any(Executor.class),
                any(TelephonyScanManager.NetworkScanCallback.class));

        startNetworkScan_incremental(true);

        verify(mNetworkScanCallback, times(1)).onError(anyInt());
    }

    @Test
    public void startNetworkScan_incrementalAndAbort_doStop() {
        doReturn(mNetworkScan).when(mTelephonyManager).requestNetworkScan(
                any(NetworkScanRequest.class), any(Executor.class),
                any(TelephonyScanManager.NetworkScanCallback.class));

        startNetworkScan_incremental(false);

        verify(mNetworkScan, times(1)).stopScan();
    }

    @Test
    public void createNetworkScanForPreferredAccessNetworks_deviceNoNrSa_noNgran() {
        int[] deviceNrCapabilities = new int[]{PhoneCapability.DEVICE_NR_CAPABILITY_NSA};
        PhoneCapability phoneCapability = createPhoneCapability(deviceNrCapabilities);
        doReturn(TelephonyManager.NETWORK_CLASS_BITMASK_2G
                | TelephonyManager.NETWORK_CLASS_BITMASK_3G
                | TelephonyManager.NETWORK_CLASS_BITMASK_4G
                | TelephonyManager.NETWORK_CLASS_BITMASK_5G).when(
                mTelephonyManager).getPreferredNetworkTypeBitmask();
        doReturn(phoneCapability).when(mTelephonyManager).getPhoneCapability();
        List<RadioAccessSpecifier> radioAccessSpecifiers = new ArrayList<>();
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.GERAN, null,
                        null));
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.UTRAN, null,
                        null));
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.EUTRAN, null,
                        null));
        NetworkScanRequest expectedNetworkScanRequest = createNetworkScanRequest(
                radioAccessSpecifiers);

        assertEquals(expectedNetworkScanRequest,
                mNetworkScanHelper.createNetworkScanForPreferredAccessNetworks());
    }

    @Test
    public void createNetworkScanForPreferredAccessNetworks_deviceHasNrSa_hasNgran() {
        int[] deviceNrCapabilities = new int[]{PhoneCapability.DEVICE_NR_CAPABILITY_NSA,
                PhoneCapability.DEVICE_NR_CAPABILITY_SA};
        PhoneCapability phoneCapability = createPhoneCapability(deviceNrCapabilities);
        doReturn(TelephonyManager.NETWORK_CLASS_BITMASK_2G
                | TelephonyManager.NETWORK_CLASS_BITMASK_3G
                | TelephonyManager.NETWORK_CLASS_BITMASK_4G
                | TelephonyManager.NETWORK_CLASS_BITMASK_5G).when(
                mTelephonyManager).getPreferredNetworkTypeBitmask();
        doReturn(phoneCapability).when(mTelephonyManager).getPhoneCapability();
        List<RadioAccessSpecifier> radioAccessSpecifiers = new ArrayList<>();
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.GERAN, null,
                        null));
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.UTRAN, null,
                        null));
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.EUTRAN, null,
                        null));
        radioAccessSpecifiers.add(
                new RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.NGRAN, null,
                        null));
        NetworkScanRequest expectedNetworkScanRequest = createNetworkScanRequest(
                radioAccessSpecifiers);

        assertEquals(expectedNetworkScanRequest,
                mNetworkScanHelper.createNetworkScanForPreferredAccessNetworks());
    }

    private PhoneCapability createPhoneCapability(int[] deviceNrCapabilities) {
        int maxActiveVoiceCalls = 1;
        int maxActiveData = 2;
        ModemInfo modemInfo = new ModemInfo(1, 2, true, false);
        List<ModemInfo> logicalModemList = new ArrayList<>();
        logicalModemList.add(modemInfo);
        return new PhoneCapability(maxActiveVoiceCalls, maxActiveData,
                logicalModemList, false, deviceNrCapabilities);
    }

    private NetworkScanRequest createNetworkScanRequest(
            List<RadioAccessSpecifier> radioAccessSpecifiers) {
        return new NetworkScanRequest(
                NetworkScanRequest.SCAN_TYPE_ONE_SHOT,
                radioAccessSpecifiers.toArray(
                        new RadioAccessSpecifier[radioAccessSpecifiers.size()]),
                mNetworkScanHelper.SEARCH_PERIODICITY_SEC,
                mNetworkScanHelper.MAX_SEARCH_TIME_SEC,
                mNetworkScanHelper.INCREMENTAL_RESULTS,
                mNetworkScanHelper.INCREMENTAL_RESULTS_PERIODICITY_SEC,
                null /* List of PLMN ids (MCC-MNC) */);
    }

    private void startNetworkScan_incremental(boolean waitForCompletion) {
        mNetworkScanHelper.startNetworkScan(
                NetworkScanHelper.NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS);
        if (!waitForCompletion) {
            mNetworkScanHelper.stopNetworkQuery();
        }
    }

}
