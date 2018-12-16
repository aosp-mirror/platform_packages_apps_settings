package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import android.net.NetworkPolicy;
import android.net.NetworkTemplate;

import com.android.settingslib.net.DataUsageController.DataUsageInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
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
    public void testLowUsageLowWarning_LimitUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = POSITIVE_LARGE;
        info.usageLevel = POSITIVE_SMALL;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.limitLevel);
    }

    @Test
    public void testLowUsageEqualWarning_LimitUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = POSITIVE_LARGE;
        info.usageLevel = POSITIVE_SMALL;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.limitLevel);
    }

    @Test
    public void testNoLimitNoUsage_WarningUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = ZERO;
        info.usageLevel = ZERO;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.warningLevel);
    }

    @Test
    public void testNoLimitLowUsage_WarningUsed() {
        info.warningLevel = POSITIVE_LARGE;
        info.limitLevel = ZERO;
        info.usageLevel = POSITIVE_SMALL;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.warningLevel);
    }

    @Test
    public void testLowWarningNoLimit_UsageUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = ZERO;
        info.usageLevel = POSITIVE_LARGE;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.usageLevel);
    }

    @Test
    public void testLowWarningLowLimit_UsageUsed() {
        info.warningLevel = POSITIVE_SMALL;
        info.limitLevel = POSITIVE_SMALL;
        info.usageLevel = POSITIVE_LARGE;
        assertThat(mInfoController.getSummaryLimit(info)).isEqualTo(info.usageLevel);
    }

    private NetworkPolicy getDefaultNetworkPolicy() {
        NetworkTemplate template =
            new NetworkTemplate(NetworkTemplate.MATCH_WIFI_WILDCARD, null, null);
        int cycleDay  = -1;
        String cycleTimezone = "UTC";
        long warningBytes = -1;
        long limitBytes = -1;
        return new NetworkPolicy(template, cycleDay, cycleTimezone, warningBytes, limitBytes, true);
    }

    @Test
    public void testNullArguments_NoError() {
        mInfoController.updateDataLimit(null, null);
        mInfoController.updateDataLimit(info, null);
        mInfoController.updateDataLimit(null, getDefaultNetworkPolicy());
    }

    @Test
    public void testNegativeWarning_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = NEGATIVE;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.warningLevel).isEqualTo(ZERO);
    }

    @Test
    public void testWarningZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = ZERO;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.warningLevel).isEqualTo(ZERO);
    }

    @Test
    public void testWarningPositive_UpdatedToWarning() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = POSITIVE_SMALL;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.warningLevel).isEqualTo(policy.warningBytes);
    }

    @Test
    public void testLimitNegative_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = NEGATIVE;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.limitLevel).isEqualTo(ZERO);
    }

    @Test
    public void testLimitZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = ZERO;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.limitLevel).isEqualTo(ZERO);
    }

    @Test
    public void testLimitPositive_UpdatedToLimit() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = POSITIVE_SMALL;
        mInfoController.updateDataLimit(info, policy);
        assertThat(info.limitLevel).isEqualTo(policy.limitBytes);
    }
}