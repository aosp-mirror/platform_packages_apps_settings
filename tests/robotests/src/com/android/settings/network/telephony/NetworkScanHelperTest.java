/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.telephony.CellInfo;
import android.telephony.NetworkScan;
import android.telephony.NetworkScanRequest;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyScanManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
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

    private class NetworkScanMock extends NetworkScan {
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

    private void startNetworkScan_incremental(boolean waitForCompletion) {
        mNetworkScanHelper.startNetworkScan(
                NetworkScanHelper.NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS);
        if (!waitForCompletion) {
            mNetworkScanHelper.stopNetworkQuery();
        }
    }

}
