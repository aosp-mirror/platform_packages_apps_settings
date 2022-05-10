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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import android.net.NetworkPolicy;
import android.net.NetworkTemplate;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.net.DataUsageController.DataUsageInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DataUsageInfoControllerTest {

    private static final int NEGATIVE = -1;
    private static final int ZERO = 0;
    private static final int POSITIVE_SMALL = 1;
    private static final int POSITIVE_LARGE = 5;

    private DataUsageInfoController mInfoController;
    private DataUsageInfo info;

    @Before
    public void setUp()  {
        mInfoController = new DataUsageInfoController();
        info = new DataUsageInfo();
    }

    @Test
    public void getSummaryLimit_LowUsageLowWarning_LimitUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = POSITIVE_LARGE;
        info.usageLevel = POSITIVE_SMALL;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.limitLevel);
    }

    @Test
    public void getSummaryLimit_LowUsageEqualWarning_LimitUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = POSITIVE_LARGE;
        info.usageLevel = POSITIVE_SMALL;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.limitLevel);
    }

    @Test
    public void getSummaryLimit_NoLimitNoUsage_WarningUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = ZERO;
        info.usageLevel = ZERO;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.warningLevel);
    }

    @Test
    public void getSummaryLimit_NoLimitLowUsage_WarningUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = ZERO;
        info.usageLevel = POSITIVE_SMALL;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.warningLevel);
    }

    @Test
    public void getSummaryLimit_LowWarningNoLimit_UsageUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = ZERO;
        info.usageLevel = POSITIVE_LARGE;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.usageLevel);
    }

    @Test
    public void getSummaryLimit_LowWarningLowLimit_UsageUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = POSITIVE_SMALL;
        info.usageLevel = POSITIVE_LARGE;

        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.usageLevel);
    }

    private NetworkPolicy getDefaultNetworkPolicy() {
        NetworkTemplate template = new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build();
        int cycleDay  = -1;
        String cycleTimezone = "UTC";
        long warningBytes = -1;
        long limitBytes = -1;
        return new NetworkPolicy(template, cycleDay, cycleTimezone, warningBytes, limitBytes, true);
    }

    @Test
    public void updateDataLimit_NullArguments_NoError() {
        mInfoController.updateDataLimit(null, null);
        mInfoController.updateDataLimit(info, null);
        mInfoController.updateDataLimit(null, getDefaultNetworkPolicy());
    }

    @Test
    public void updateDataLimit_NegativeWarning_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = NEGATIVE;

        mInfoController.updateDataLimit(info, policy);

        assertThat(info.warningLevel).isEqualTo(ZERO);
    }

    @Test
    public void updateDataLimit_WarningZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = ZERO;

        mInfoController.updateDataLimit(info, policy);

        assertThat(info.warningLevel).isEqualTo(ZERO);
    }

    @Test
    public void updateDataLimit_WarningPositive_UpdatedToWarning() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = POSITIVE_SMALL;

        mInfoController.updateDataLimit(info, policy);

        assertThat(info.warningLevel).isEqualTo(policy.warningBytes);
    }

    @Test
    public void updateDataLimit_LimitNegative_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = NEGATIVE;

        mInfoController.updateDataLimit(info, policy);

        assertThat(info.limitLevel).isEqualTo(ZERO);
    }

    @Test
    public void updateDataLimit_LimitZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = ZERO;

        mInfoController.updateDataLimit(info, policy);

        assertThat(info.limitLevel).isEqualTo(ZERO);
    }

    @Test
    public void updateDataLimit_LimitPositive_UpdatedToLimit() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = POSITIVE_SMALL;

        mInfoController.updateDataLimit(info, policy);
        
        assertThat(info.limitLevel).isEqualTo(policy.limitBytes);
    }
}
