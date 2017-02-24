/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Process;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.BatteryInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.fuelgauge.PowerUsageBase.MENU_STATS_REFRESH;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_ADDITIONAL_BATTERY_INFO;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_TOGGLE_APPS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PowerUsageSummary}.
 */
// TODO: Improve this test class so that it starts up the real activity and fragment.
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageSummaryTest {
    private static final String[] PACKAGE_NAMES = {"com.app1", "com.app2"};
    private static final String TIME_LEFT = "2h30min";
    private static final int UID = 123;
    private static final int POWER_MAH = 100;
    private static final long REMAINING_TIME_US = 100000;
    private static final int DISCHARGE_AMOUNT = 100;
    private static final long USAGE_TIME_MS = 10000;
    private static final double TOTAL_POWER = 200;
    private static final double BATTERY_SCREEN_USAGE = 300;
    private static final double BATTERY_SYSTEM_USAGE = 600;
    private static final double PRECISION = 0.001;
    private static final double POWER_USAGE_PERCENTAGE = 50;
    private static final Intent ADDITIONAL_BATTERY_INFO_INTENT =
            new Intent("com.example.app.ADDITIONAL_BATTERY_INFO");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuItem mAdditionalBatteryInfoMenu;
    @Mock
    private MenuItem mToggleAppsMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mScreenBatterySipper;
    @Mock
    private BatterySipper mSystemBatterySipper;
    @Mock
    private BatterySipper mCellBatterySipper;
    @Mock
    private PowerGaugePreference mPreference;
    @Mock
    private LayoutPreference mBatteryLayoutPref;
    @Mock
    private BatteryMeterView mBatteryMeterView;
    @Mock
    private TextView mTimeText;
    @Mock
    private TextView mSummary1;
    @Mock
    private TextView mSummary2;
    @Mock
    private BatteryInfo mBatteryInfo;
    @Mock
    private Preference mScreenUsagePref;
    @Mock
    private Preference mScreenConsumptionPref;
    @Mock
    private Preference mCellularNetworkPref;
    @Mock
    private BatteryStatsHelper mBatteryHelper;
    @Mock
    private PowerManager mPowerManager;

    private List<BatterySipper> mUsageList;
    private Context mRealContext;
    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRealContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);

        mFragment = spy(new TestFragment(mContext));
        mFragment.initFeatureProvider();

        when(mAdditionalBatteryInfoMenu.getItemId())
                .thenReturn(MENU_ADDITIONAL_BATTERY_INFO);
        when(mToggleAppsMenu.getItemId()).thenReturn(MENU_TOGGLE_APPS);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(ADDITIONAL_BATTERY_INFO_INTENT);
        when(mBatteryHelper.getTotalPower()).thenReturn(TOTAL_POWER);

        when(mNormalBatterySipper.getPackages()).thenReturn(PACKAGE_NAMES);
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        mNormalBatterySipper.totalPowerMah = POWER_MAH;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        mCellBatterySipper.drainType = BatterySipper.DrainType.CELL;
        mCellBatterySipper.totalPowerMah = POWER_MAH;

        when(mBatteryLayoutPref.findViewById(R.id.summary1)).thenReturn(mSummary1);
        when(mBatteryLayoutPref.findViewById(R.id.summary2)).thenReturn(mSummary2);
        when(mBatteryLayoutPref.findViewById(R.id.time)).thenReturn(mTimeText);
        when(mBatteryLayoutPref.findViewById(R.id.battery_header_icon))
                .thenReturn(mBatteryMeterView);
        mFragment.setBatteryLayoutPreference(mBatteryLayoutPref);

        mScreenBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        mScreenBatterySipper.totalPowerMah = BATTERY_SCREEN_USAGE;
        mScreenBatterySipper.usageTimeMs = USAGE_TIME_MS;

        mSystemBatterySipper.drainType = BatterySipper.DrainType.APP;
        mSystemBatterySipper.totalPowerMah = BATTERY_SYSTEM_USAGE;
        when(mSystemBatterySipper.getUid()).thenReturn(Process.SYSTEM_UID);

        mUsageList = new ArrayList<>();
        mUsageList.add(mNormalBatterySipper);
        mUsageList.add(mScreenBatterySipper);
        mUsageList.add(mCellBatterySipper);

        mFragment.mStatsHelper = mBatteryHelper;
        when(mBatteryHelper.getUsageList()).thenReturn(mUsageList);
        mFragment.mScreenUsagePref = mScreenUsagePref;
        mFragment.mScreenConsumptionPref = mScreenConsumptionPref;
        mFragment.mCellularNetworkPref = mCellularNetworkPref;
    }

    @Test
    public void testOptionsMenu_additionalBatteryInfoEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isAdditionalBatteryInfoEnabled())
                .thenReturn(true);

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                Menu.NONE, R.string.additional_battery_info);

        mFragment.onOptionsItemSelected(mAdditionalBatteryInfoMenu);

        assertThat(mFragment.mStartActivityCalled).isTrue();
        assertThat(mFragment.mStartActivityIntent).isEqualTo(ADDITIONAL_BATTERY_INFO_INTENT);
    }

    @Test
    public void testOptionsMenu_additionalBatteryInfoDisabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isAdditionalBatteryInfoEnabled())
                .thenReturn(false);

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu, never()).add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                Menu.NONE, R.string.additional_battery_info);
    }

    @Test
    public void testOptionsMenu_ToggleAppsEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isPowerAccountingToggleEnabled())
                .thenReturn(true);
        mFragment.mShowAllApps = false;

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, MENU_TOGGLE_APPS, Menu.NONE, R.string.show_all_apps);
    }

    @Test
    public void testOptionsMenu_ClickToggleAppsMenu_DataChanged() {
        testToggleAllApps(true);
        testToggleAllApps(false);
    }

    @Test
    public void testExtractKeyFromSipper_TypeAPPUidObjectNull_ReturnPackageNames() {
        mNormalBatterySipper.uidObj = null;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(TextUtils.concat(mNormalBatterySipper.getPackages()).toString());
    }

    @Test
    public void testExtractKeyFromSipper_TypeOther_ReturnDrainType() {
        mNormalBatterySipper.uidObj = null;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.BLUETOOTH;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(mNormalBatterySipper.drainType.toString());
    }

    @Test
    public void testExtractKeyFromSipper_TypeAPPUidObjectNotNull_ReturnUid() {
        mNormalBatterySipper.uidObj = new BatteryStatsImpl.Uid(new BatteryStatsImpl(), UID);
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(Integer.toString(mNormalBatterySipper.getUid()));
    }

    @Test
    public void testRemoveHiddenBatterySippers_ContainsHiddenSippers_RemoveAndReturnValue() {
        final List<BatterySipper> sippers = new ArrayList<>();
        sippers.add(mNormalBatterySipper);
        sippers.add(mScreenBatterySipper);
        sippers.add(mSystemBatterySipper);
        when(mFeatureFactory.powerUsageFeatureProvider.isTypeSystem(mSystemBatterySipper))
                .thenReturn(true);

        final double totalUsage = mFragment.removeHiddenBatterySippers(sippers);
        assertThat(sippers).containsExactly(mNormalBatterySipper);
        assertThat(totalUsage).isWithin(PRECISION).of(BATTERY_SCREEN_USAGE + BATTERY_SYSTEM_USAGE);
    }

    @Test
    public void testShouldHideSipper_TypeIdle_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.IDLE;
        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeCell_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.CELL;
        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeScreen_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeSystem_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(Process.ROOT_UID);
        when(mFeatureFactory.powerUsageFeatureProvider.isTypeSystem(Matchers.<BatterySipper>any()))
                .thenReturn(true);
        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_UidNormal_ReturnFalse() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isFalse();
    }

    @Test
    public void testShouldHideSipper_TypeService_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        when(mFeatureFactory.powerUsageFeatureProvider.isTypeService(Matchers.<BatterySipper>any()))
                .thenReturn(true);

        assertThat(mFragment.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testSetUsageSummary_TimeLessThanOneMinute_DoNotSetSummary() {
        final long usageTimeMs = 59 * DateUtils.SECOND_IN_MILLIS;

        mFragment.setUsageSummary(mPreference, "", usageTimeMs);
        verify(mPreference, never()).setSummary(anyString());
    }

    @Test
    public void testSetUsageSummary_TimeMoreThanOneMinute_SetSummary() {
        final long usageTimeMs = 2 * DateUtils.MINUTE_IN_MILLIS;

        mFragment.setUsageSummary(mPreference, "", usageTimeMs);
        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void testUpdatePreference_NoEstimatedTime_DoNotShowSummary() {
        mBatteryInfo.remainingTimeUs = 0;
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mFragment.updateHeaderPreference(mBatteryInfo);

        verify(mSummary1).setVisibility(View.INVISIBLE);
        verify(mSummary2).setVisibility(View.INVISIBLE);
    }

    @Test
    public void testUpdatePreference_HasEstimatedTime_ShowSummary() {
        mBatteryInfo.remainingTimeUs = REMAINING_TIME_US;
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mFragment.updateHeaderPreference(mBatteryInfo);

        verify(mSummary1).setVisibility(View.VISIBLE);
        verify(mSummary2).setVisibility(View.VISIBLE);
    }

    @Test
    public void testUpdatePreference_Charging_ShowChargingTimeLeft() {
        mBatteryInfo.remainingTimeUs = REMAINING_TIME_US;
        mBatteryInfo.mDischarging = false;

        mFragment.updateHeaderPreference(mBatteryInfo);
        verify(mSummary1).setText(R.string.estimated_charging_time_left);
    }

    @Test
    public void testUpdatePreference_NotCharging_ShowTimeLeft() {
        mBatteryInfo.remainingTimeUs = REMAINING_TIME_US;
        mBatteryInfo.mDischarging = true;

        mFragment.updateHeaderPreference(mBatteryInfo);
        verify(mSummary1).setText(R.string.estimated_time_left);
    }

    @Test
    public void testUpdateHeaderPreference_AsyncUpdate_ShouldNotCrash() {
        when(mFragment.getContext()).thenReturn(null);
        mBatteryInfo.remainingTimeUs = REMAINING_TIME_US;

        //Should not crash
        mFragment.updateHeaderPreference(mBatteryInfo);
    }

    private void testToggleAllApps(final boolean isShowApps) {
        mFragment.mShowAllApps = isShowApps;

        mFragment.onOptionsItemSelected(mToggleAppsMenu);
        assertThat(mFragment.mShowAllApps).isEqualTo(!isShowApps);
    }

    @Test
    public void testFindBatterySipperByType_findTypeScreen() {
        BatterySipper sipper = mFragment.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.SCREEN);

        assertThat(sipper).isSameAs(mScreenBatterySipper);
    }

    @Test
    public void testFindBatterySipperByType_findTypeApp() {
        BatterySipper sipper = mFragment.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.APP);

        assertThat(sipper).isSameAs(mNormalBatterySipper);
    }

    @Test
    public void testUpdateCellularPreference_ShowCorrectSummary() {
        final double percent = POWER_MAH / TOTAL_POWER * DISCHARGE_AMOUNT;
        final String expectedSummary = mRealContext.getString(R.string.battery_overall_usage,
                Utils.formatPercentage((int) percent));
        doReturn(expectedSummary).when(mFragment)
                .getString(eq(R.string.battery_overall_usage), anyInt());
        mFragment.updateCellularPreference(DISCHARGE_AMOUNT);

        verify(mCellularNetworkPref).setSummary(expectedSummary);
    }

    @Test
    public void testUpdateScreenPreference_ShowCorrectSummary() {
        final String expectedUsedTime = mRealContext.getString(R.string.battery_used_for,
                Utils.formatElapsedTime(mRealContext, USAGE_TIME_MS, false));
        final double percent = BATTERY_SCREEN_USAGE / TOTAL_POWER * DISCHARGE_AMOUNT;
        final String expectedOverallUsage = mRealContext.getString(R.string.battery_overall_usage,
                Utils.formatPercentage((int) percent));
        doReturn(expectedUsedTime).when(mFragment).getString(
                eq(R.string.battery_used_for), anyInt());
        doReturn(expectedOverallUsage).when(mFragment).getString(
                eq(R.string.battery_overall_usage), anyInt());

        mFragment.updateScreenPreference(DISCHARGE_AMOUNT);

        verify(mScreenUsagePref).setSummary(expectedUsedTime);
        verify(mScreenConsumptionPref).setSummary(expectedOverallUsage);
    }

    @Test
    public void testUpdatePreference_UsageListEmpty_ShouldNotCrash() {
        when(mBatteryHelper.getUsageList()).thenReturn(new ArrayList<BatterySipper>());
        doReturn("").when(mFragment).getString(anyInt(), Matchers.anyObject());

        // Should not crash when update
        mFragment.updateScreenPreference(DISCHARGE_AMOUNT);
        mFragment.updateCellularPreference(DISCHARGE_AMOUNT);
    }

    @Test
    public void testCalculatePercentage() {
        final double percent = mFragment.calculatePercentage(POWER_MAH, DISCHARGE_AMOUNT);
        assertThat(percent).isWithin(PRECISION).of(POWER_USAGE_PERCENTAGE);
    }

    public static class TestFragment extends PowerUsageSummary {

        private Context mContext;
        private boolean mStartActivityCalled;
        private Intent mStartActivityIntent;

        public TestFragment(Context context) {
            mContext = context;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public void startActivity(Intent intent) {
            mStartActivityCalled = true;
            mStartActivityIntent = intent;
        }

        @Override
        protected void refreshStats() {
            // Leave it empty for toggle apps menu test
        }
    }
}
