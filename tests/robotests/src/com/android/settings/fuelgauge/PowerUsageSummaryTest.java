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
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_ADDITIONAL_BATTERY_INFO;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_HIGH_POWER_APPS;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_TOGGLE_APPS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class PowerUsageSummaryTest {
    private static final String[] PACKAGE_NAMES = {"com.app1", "com.app2"};
    private static final String STUB_STRING = "stub_string";
    private static final int UID = 123;
    private static final int POWER_MAH = 100;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_MS = 120 * 60 * 1000;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_US =
            TIME_SINCE_LAST_FULL_CHARGE_MS * 1000;
    private static final int DISCHARGE_AMOUNT = 100;
    private static final long USAGE_TIME_MS = 65 * 60 * 1000;
    private static final double TOTAL_POWER = 200;
    private static final double BATTERY_SCREEN_USAGE = 300;
    private static final double BATTERY_SYSTEM_USAGE = 600;
    private static final double BATTERY_OVERCOUNTED_USAGE = 500;
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
    private MenuItem mHighPowerMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mScreenBatterySipper;
    @Mock
    private BatterySipper mCellBatterySipper;
    @Mock
    private LayoutPreference mBatteryLayoutPref;
    @Mock
    private TextView mBatteryPercentText;
    @Mock
    private TextView mSummary1;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryHelper;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private List<BatterySipper> mUsageList;
    private Context mRealContext;
    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryMeterView mBatteryMeterView;
    private PowerGaugePreference mPreference;
    private PowerGaugePreference mScreenUsagePref;
    private PowerGaugePreference mLastFullChargePref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRealContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);

        mPreference = new PowerGaugePreference(mRealContext);
        mScreenUsagePref = new PowerGaugePreference(mRealContext);
        mLastFullChargePref = new PowerGaugePreference(mRealContext);
        mFragment = spy(new TestFragment(mContext));
        mFragment.initFeatureProvider();
        mBatteryMeterView = new BatteryMeterView(mRealContext);
        mBatteryMeterView.mDrawable = new BatteryMeterView.BatteryMeterDrawable(mRealContext, 0);
        doNothing().when(mFragment).restartBatteryStatsLoader();

        when(mFragment.getActivity()).thenReturn(mSettingsActivity);
        when(mAdditionalBatteryInfoMenu.getItemId())
                .thenReturn(MENU_ADDITIONAL_BATTERY_INFO);
        when(mToggleAppsMenu.getItemId()).thenReturn(MENU_TOGGLE_APPS);
        when(mHighPowerMenu.getItemId()).thenReturn(MENU_HIGH_POWER_APPS);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(ADDITIONAL_BATTERY_INFO_INTENT);
        when(mBatteryHelper.getTotalPower()).thenReturn(TOTAL_POWER);
        when(mBatteryHelper.getStats().computeBatteryRealtime(anyLong(), anyInt())).thenReturn(
                TIME_SINCE_LAST_FULL_CHARGE_US);

        when(mNormalBatterySipper.getPackages()).thenReturn(PACKAGE_NAMES);
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        mNormalBatterySipper.totalPowerMah = POWER_MAH;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        mCellBatterySipper.drainType = BatterySipper.DrainType.CELL;
        mCellBatterySipper.totalPowerMah = POWER_MAH;

        when(mBatteryLayoutPref.findViewById(R.id.summary1)).thenReturn(mSummary1);
        when(mBatteryLayoutPref.findViewById(R.id.battery_percent)).thenReturn(mBatteryPercentText);
        when(mBatteryLayoutPref.findViewById(R.id.battery_header_icon))
                .thenReturn(mBatteryMeterView);
        mFragment.setBatteryLayoutPreference(mBatteryLayoutPref);

        mScreenBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        mScreenBatterySipper.usageTimeMs = USAGE_TIME_MS;

        mUsageList = new ArrayList<>();
        mUsageList.add(mNormalBatterySipper);
        mUsageList.add(mScreenBatterySipper);
        mUsageList.add(mCellBatterySipper);

        mFragment.mStatsHelper = mBatteryHelper;
        when(mBatteryHelper.getUsageList()).thenReturn(mUsageList);
        mFragment.mScreenUsagePref = mScreenUsagePref;
        mFragment.mLastFullChargePref = mLastFullChargePref;
        mFragment.mBatteryUtils = spy(new BatteryUtils(mRealContext));
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
    public void testOptionsMenu_menuHighPower_metricEventInvoked() {
        mFragment.onOptionsItemSelected(mHighPowerMenu);

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_OPTIMIZATION);
    }

    @Test
    public void testOptionsMenu_menuAdditionalBattery_metricEventInvoked() {
        mFragment.onOptionsItemSelected(mAdditionalBatteryInfoMenu);

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_USAGE_ALERTS);
    }

    @Test
    public void testOptionsMenu_menuAppToggle_metricEventInvoked() {
        mFragment.onOptionsItemSelected(mToggleAppsMenu);
        mFragment.mShowAllApps = false;

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_APPS_TOGGLE, true);
    }

    @Test
    public void testOptionsMenu_toggleAppsEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isPowerAccountingToggleEnabled())
                .thenReturn(true);
        mFragment.mShowAllApps = false;

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, MENU_TOGGLE_APPS, Menu.NONE, R.string.show_all_apps);
    }

    @Test
    public void testOptionsMenu_clickToggleAppsMenu_dataChanged() {
        testToggleAllApps(true);
        testToggleAllApps(false);
    }

    @Test
    public void testExtractKeyFromSipper_typeAPPUidObjectNull_returnPackageNames() {
        mNormalBatterySipper.uidObj = null;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(TextUtils.concat(mNormalBatterySipper.getPackages()).toString());
    }

    @Test
    public void testExtractKeyFromSipper_typeOther_returnDrainType() {
        mNormalBatterySipper.uidObj = null;
        mNormalBatterySipper.drainType = BatterySipper.DrainType.BLUETOOTH;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(mNormalBatterySipper.drainType.toString());
    }

    @Test
    public void testExtractKeyFromSipper_typeAPPUidObjectNotNull_returnUid() {
        mNormalBatterySipper.uidObj = new BatteryStatsImpl.Uid(new BatteryStatsImpl(), UID);
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mFragment.extractKeyFromSipper(mNormalBatterySipper);
        assertThat(key).isEqualTo(Integer.toString(mNormalBatterySipper.getUid()));
    }

    @Test
    public void testSetUsageSummary_timeLessThanOneMinute_DoNotSetSummary() {
        mNormalBatterySipper.usageTimeMs = 59 * DateUtils.SECOND_IN_MILLIS;

        mFragment.setUsageSummary(mPreference, mNormalBatterySipper);
        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void testSetUsageSummary_timeMoreThanOneMinute_normalApp_setScreenSummary() {
        mNormalBatterySipper.usageTimeMs = 2 * DateUtils.MINUTE_IN_MILLIS;
        doReturn(mRealContext.getText(R.string.battery_screen_usage)).when(mFragment).getText(
                R.string.battery_screen_usage);
        doReturn(mRealContext).when(mFragment).getContext();
        final String expectedSummary = "Screen usage 2m";

        mFragment.setUsageSummary(mPreference, mNormalBatterySipper);

        assertThat(mPreference.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void testSetUsageSummary_timeMoreThanOneMinute_hiddenApp_setUsedSummary() {
        mNormalBatterySipper.usageTimeMs = 2 * DateUtils.MINUTE_IN_MILLIS;
        doReturn(true).when(mFragment.mBatteryUtils).shouldHideSipper(mNormalBatterySipper);
        doReturn(mRealContext).when(mFragment).getContext();
        final String expectedSummary = "2m";

        mFragment.setUsageSummary(mPreference, mNormalBatterySipper);

        assertThat(mPreference.getSummary().toString()).isEqualTo(expectedSummary);
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
    public void testUpdateScreenPreference_showCorrectSummary() {
        doReturn(mScreenBatterySipper).when(mFragment).findBatterySipperByType(any(), any());
        doReturn(mRealContext).when(mFragment).getContext();
        final CharSequence expectedSummary = Utils.formatElapsedTime(mRealContext, USAGE_TIME_MS,
                false);

        mFragment.updateScreenPreference();

        assertThat(mScreenUsagePref.getSubtitle()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdateLastFullChargePreference_showCorrectSummary() {
        final CharSequence formattedString = mRealContext.getText(
                R.string.power_last_full_charge_summary);
        final CharSequence timeSequence = Utils.formatElapsedTime(mRealContext,
                TIME_SINCE_LAST_FULL_CHARGE_MS, false);
        final CharSequence expectedSummary = TextUtils.expandTemplate(
                formattedString, timeSequence);
        doReturn(formattedString).when(mFragment).getText(R.string.power_last_full_charge_summary);
        doReturn(mRealContext).when(mFragment).getContext();

        mFragment.updateLastFullChargePreference(TIME_SINCE_LAST_FULL_CHARGE_MS);

        assertThat(mLastFullChargePref.getSubtitle()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdatePreference_usageListEmpty_shouldNotCrash() {
        when(mBatteryHelper.getUsageList()).thenReturn(new ArrayList<BatterySipper>());
        doReturn(STUB_STRING).when(mFragment).getString(anyInt(), any());
        doReturn(mRealContext).when(mFragment).getContext();

        // Should not crash when update
        mFragment.updateScreenPreference();
    }

    @Test
    public void testCalculatePercentage() {
        final double percent = mFragment.calculatePercentage(POWER_MAH, DISCHARGE_AMOUNT);
        assertThat(percent).isWithin(PRECISION).of(POWER_USAGE_PERCENTAGE);
    }

    @Test
    public void testCalculateRunningTimeBasedOnStatsType() {
        assertThat(mFragment.calculateRunningTimeBasedOnStatsType()).isEqualTo(
                TIME_SINCE_LAST_FULL_CHARGE_MS);
    }

    @Test
    public void testNonIndexableKeys_MatchPreferenceKeys() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = PowerUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.power_usage_summary);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = RuntimeEnvironment.application;
        final PowerUsageSummary fragment = new PowerUsageSummary();
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(context,
                fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (PreferenceController controller : fragment.getPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    @Test
    public void testSaveInstanceState_showAllAppsRestored() {
        Bundle bundle = new Bundle();
        mFragment.mShowAllApps = true;
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        mFragment.onSaveInstanceState(bundle);
        mFragment.restoreSavedInstance(bundle);

        assertThat(mFragment.mShowAllApps).isTrue();
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
        protected void refreshUi() {
            // Leave it empty for toggle apps menu test
        }
    }
}
