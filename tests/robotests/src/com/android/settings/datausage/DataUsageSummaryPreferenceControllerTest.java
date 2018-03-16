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

package com.android.settings.datausage;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.TimeUnit;

@RunWith(SettingsRobolectricTestRunner.class)
public class DataUsageSummaryPreferenceControllerTest {

    private static final long UPDATE_BACKOFF_MS = TimeUnit.MINUTES.toMillis(13);
    private static final long CYCLE_BACKOFF_MS = TimeUnit.DAYS.toMillis(6);
    private static final long CYCLE_LENGTH_MS = TimeUnit.DAYS.toMillis(30);
    private static final long USAGE1 =  373000000L;
    private static final long LIMIT1 = 1000000000L;
    private static final String CARRIER_NAME = "z-mobile";
    private static final String PERIOD = "Feb";

    @Mock
    private DataUsageController mDataUsageController;
    @Mock
    private DataUsageInfoController mDataInfoController;
    @Mock
    private DataUsageSummaryPreference mSummaryPreference;
    @Mock
    private NetworkPolicyEditor mPolicyEditor;
    @Mock
    private NetworkTemplate mNetworkTemplate;

    private Context mContext;
    private DataUsageSummaryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        doReturn("%1$s %2%s").when(mContext)
            .getString(com.android.internal.R.string.fileSizeSuffix);
        mController = new DataUsageSummaryPreferenceController(
                mContext,
                mDataUsageController,
                mDataInfoController,
                mNetworkTemplate,
                mPolicyEditor,
                R.string.cell_data_template,
                true,
                null);
    }

    @Test
    public void testSummaryUpdate_onePlan_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(1 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo("500 MB data warning / 1.00 GB data limit");
        verify(mSummaryPreference).setUsageInfo(info.cycleEnd, now - UPDATE_BACKOFF_MS,
                CARRIER_NAME, 1 /* numPlans */, intent);
        verify(mSummaryPreference).setChartEnabled(true);
    }

    @Test
    public void testSummaryUpdate_noPlan_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo("500 MB data warning / 1.00 GB data limit");
        verify(mSummaryPreference).setUsageInfo(info.cycleEnd, now - UPDATE_BACKOFF_MS,
                CARRIER_NAME, 0 /* numPlans */, intent);
        verify(mSummaryPreference).setChartEnabled(true);
    }

    @Test
    public void testSummaryUpdate_noCarrier_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(null /* carrierName */, -1L /* snapshotTime */,
                info.cycleEnd, null /* intent */);
        mController.updateState(mSummaryPreference);

        verify(mSummaryPreference).setLimitInfo("500 MB data warning / 1.00 GB data limit");
        verify(mSummaryPreference).setUsageInfo(
                info.cycleEnd,
                -1L /* snapshotTime */,
                null /* carrierName */,
                0 /* numPlans */,
                null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(true);
    }

    @Test
    public void testSummaryUpdate_noPlanData_basic() {
        final long now = System.currentTimeMillis();

        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, -1L /* dataPlanSize */, USAGE1);
        mController.setCarrierValues(null /* carrierName */, -1L /* snapshotTime */,
                info.cycleEnd, null /* intent */);
        mController.updateState(mSummaryPreference);

        verify(mSummaryPreference).setLimitInfo("500 MB data warning / 1.00 GB data limit");
        verify(mSummaryPreference).setUsageInfo(
                info.cycleEnd,
                -1L /* snapshotTime */,
                null /* carrierName */,
                0 /* numPlans */,
                null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(false);
    }

    @Test
    public void testSummaryUpdate_noLimitNoWarning() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 0L;
        info.limitLevel = 0L;

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo(null);
    }

    @Test
    public void testSummaryUpdate_warningOnly() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 1000000L;
        info.limitLevel = 0L;

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo("1.00 MB data warning");
    }

    @Test
    public void testSummaryUpdate_limitOnly() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 0L;
        info.limitLevel = 1000000L;

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo("1.00 MB data limit");
    }

    @Test
    public void testSummaryUpdate_limitAndWarning() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 1000000L;
        info.limitLevel = 1000000L;

        final Intent intent = new Intent();

        when(mDataUsageController.getDataUsageInfo(any())).thenReturn(info);
        mController.setPlanValues(0 /* dataPlanCount */, LIMIT1, USAGE1);
        mController.setCarrierValues(CARRIER_NAME, now - UPDATE_BACKOFF_MS, info.cycleEnd, intent);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo("1.00 MB data warning / 1.00 MB data limit");
    }

    private DataUsageController.DataUsageInfo createTestDataUsageInfo(long now) {
        DataUsageController.DataUsageInfo info = new DataUsageController.DataUsageInfo();
        info.carrier = CARRIER_NAME;
        info.period = PERIOD;
        info.startDate = now;
        info.limitLevel = LIMIT1;
        info.warningLevel = LIMIT1 >> 1;
        info.usageLevel = USAGE1;
        info.cycleStart = now - CYCLE_BACKOFF_MS;
        info.cycleEnd = info.cycleStart + CYCLE_LENGTH_MS;
        return info;
    }
}
