package com.android.settings.datausage;

import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import com.android.settings.TestConfig;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.android.settingslib.net.DataUsageController.*;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataUsageInfoControllerTest {
    private DataUsageInfoController mInfoController;
    private  DataUsageInfo info;

    @Before
    public void setUp()  {
        mInfoController = new DataUsageInfoController();
        info = new DataUsageInfo();
    }

    @Test
    public void testLowUsageLowWarning_LimitUsed() {
        info.warningLevel = 5;
        info.limitLevel = 10;
        info.usageLevel = 5;
        assertEquals(mInfoController.getSummaryLimit(info), info.limitLevel);
    }

    @Test
    public void testLowUsageEqualWarning_LimitUsed() {
        info.warningLevel = 10;
        info.limitLevel = 10;
        info.usageLevel = 5;
        assertEquals(mInfoController.getSummaryLimit(info), info.limitLevel);
    }

    @Test
    public void testNoLimitNoUsage_WarningUsed() {
        info.warningLevel = 10;
        info.limitLevel = 0;
        info.usageLevel = 0;
        assertEquals(mInfoController.getSummaryLimit(info), info.warningLevel);
    }

    @Test
    public void testNoLimitLowUsage_WarningUsed() {
        info.warningLevel = 10;
        info.limitLevel = 0;
        info.usageLevel = 5;
        assertEquals(mInfoController.getSummaryLimit(info), info.warningLevel);
    }

    @Test
    public void testLowWarningNoLimit_UsageUsed() {
        info.warningLevel = 5;
        info.limitLevel = 0;
        info.usageLevel = 10;
        assertEquals(mInfoController.getSummaryLimit(info), info.usageLevel);
    }

    @Test
    public void testLowWarningLowLimit_UsageUsed() {
        info.warningLevel = 5;
        info.limitLevel = 5;
        info.usageLevel = 10;
        assertEquals(mInfoController.getSummaryLimit(info), info.usageLevel);
    }

    private NetworkPolicy getDefaultNetworkPolicy() {
        NetworkTemplate template = new NetworkTemplate(NetworkTemplate.MATCH_WIFI_WILDCARD,
                null, null);
        int cycleDay  = -1;
        String cycleTimezone = "UTC";
        long warningBytes = -1;
        long limitBytes = -1;
        return new NetworkPolicy(template,cycleDay, cycleTimezone, warningBytes, limitBytes, true);
    }

    @Test
    public void testNullArguments_NoError() {
        try {
            mInfoController.updateDataLimit(null, null);
            mInfoController.updateDataLimit(info, null);
            mInfoController.updateDataLimit(null, getDefaultNetworkPolicy());
        } catch (Exception e) {
            fail("Update Data Limit should drop calls with null arguments");
        }
    }

    @Test
    public void testNegativeWarning_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = -5;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(0, info.warningLevel);
    }

    @Test
    public void testWarningZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = 0;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(0, info.warningLevel);
    }

    @Test
    public void testWarningPositive_UpdatedToWarning() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.warningBytes = 5;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(policy.warningBytes, info.warningLevel);
    }

    @Test
    public void testLimitNegative_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = -5;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(0, info.limitLevel);
    }

    @Test
    public void testLimitZero_UpdatedToZero() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = 0;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(0, info.limitLevel);
    }

    @Test
    public void testLimitPositive_UpdatedToLimit() {
        NetworkPolicy policy = getDefaultNetworkPolicy();
        policy.limitBytes = 5;
        mInfoController.updateDataLimit(info, policy);
        Assert.assertEquals(policy.limitBytes, info.limitLevel);
    }
}
