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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

// TODO: Improve this test class so that it starts up the real activity and fragment.
@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
    SettingsShadowResources.class,
    SettingsShadowResources.SettingsShadowTheme.class,
})
public class PowerUsageSummaryTest {

    private static final int UID = 123;
    private static final int UID_2 = 234;
    private static final int POWER_MAH = 100;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_MS = 120 * 60 * 1000;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_US =
            TIME_SINCE_LAST_FULL_CHARGE_MS * 1000;
    private static final long USAGE_TIME_MS = 65 * 60 * 1000;
    private static final double TOTAL_POWER = 200;
    private static final String NEW_ML_EST_SUFFIX = "(New ML est)";
    private static final String OLD_EST_SUFFIX = "(Old est)";
    private static Intent sAdditionalBatteryInfoIntent;

    @BeforeClass
    public static void beforeClass() {
        sAdditionalBatteryInfoIntent = new Intent("com.example.app.ADDITIONAL_BATTERY_INFO");
    }

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
    private SettingsActivity mSettingsActivity;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;

    private List<BatterySipper> mUsageList;
    private Context mRealContext;
    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryMeterView mBatteryMeterView;
    private PowerGaugePreference mScreenUsagePref;
    private PowerGaugePreference mLastFullChargePref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRealContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mScreenUsagePref = new PowerGaugePreference(mRealContext);
        mLastFullChargePref = new PowerGaugePreference(mRealContext);
        mFragment = spy(new TestFragment(mRealContext));
        mFragment.initFeatureProvider();
        mBatteryMeterView = new BatteryMeterView(mRealContext);
        mBatteryMeterView.mDrawable = new BatteryMeterView.BatteryMeterDrawable(mRealContext, 0);
        doNothing().when(mFragment).restartBatteryStatsLoader();
        doReturn(mock(LoaderManager.class)).when(mFragment).getLoaderManager();

        when(mFragment.getActivity()).thenReturn(mSettingsActivity);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(sAdditionalBatteryInfoIntent);
        when(mBatteryHelper.getTotalPower()).thenReturn(TOTAL_POWER);
        when(mBatteryHelper.getStats().computeBatteryRealtime(anyLong(), anyInt()))
            .thenReturn(TIME_SINCE_LAST_FULL_CHARGE_US);

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
    public void testUpdateLastFullChargePreference_showCorrectSummary() {
        when(mFragment.getContext()).thenReturn(mRealContext);

        mFragment.updateLastFullChargePreference(TIME_SINCE_LAST_FULL_CHARGE_MS);

        assertThat(mLastFullChargePref.getSubtitle()).isEqualTo("2 hr. ago");
    }

    @Test
    public void testNonIndexableKeys_MatchPreferenceKeys() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks =
            PowerUsageSummary.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context);

        final List<String> keys =
            XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.power_usage_summary);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = RuntimeEnvironment.application;
        final PowerUsageSummary fragment = new PowerUsageSummary();
        final List<String> preferenceScreenKeys =
            XmlTestUtils.getKeysFromPreferenceXml(context, fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : fragment.createPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    @Test
    public void testUpdateAnomalySparseArray() {
        mFragment.mAnomalySparseArray = new SparseArray<>();
        final List<Anomaly> anomalies = new ArrayList<>();
        final Anomaly anomaly1 = new Anomaly.Builder().setUid(UID).build();
        final Anomaly anomaly2 = new Anomaly.Builder().setUid(UID).build();
        final Anomaly anomaly3 = new Anomaly.Builder().setUid(UID_2).build();
        anomalies.add(anomaly1);
        anomalies.add(anomaly2);
        anomalies.add(anomaly3);

        mFragment.updateAnomalySparseArray(anomalies);

        assertThat(mFragment.mAnomalySparseArray.get(UID)).containsExactly(anomaly1, anomaly2);
        assertThat(mFragment.mAnomalySparseArray.get(UID_2)).containsExactly(anomaly3);
    }

    @Test
    public void testRestartBatteryTipLoader() {
        //TODO: add policy logic here when BatteryTipPolicy is implemented
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mFragment.restartBatteryTipLoader();

        verify(mLoaderManager)
            .restartLoader(eq(PowerUsageSummary.BATTERY_TIP_LOADER), eq(Bundle.EMPTY), any());
    }

    @Test
    public void testShowBothEstimates_summariesAreBothModified() {
        when(mFeatureFactory.powerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(any()))
            .thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return mRealContext.getString(
                    R.string.power_usage_old_debug, invocation.getArguments()[0]);
            }
        }).when(mFeatureFactory.powerUsageFeatureProvider).getOldEstimateDebugString(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return mRealContext.getString(
                    R.string.power_usage_enhanced_debug, invocation.getArguments()[0]);
            }
        }).when(mFeatureFactory.powerUsageFeatureProvider).getEnhancedEstimateDebugString(any());

        doReturn(new TextView(mRealContext)).when(mBatteryLayoutPref).findViewById(R.id.summary2);
        doReturn(new TextView(mRealContext)).when(mBatteryLayoutPref).findViewById(R.id.summary1);
        mFragment.onLongClick(new View(mRealContext));
        TextView summary1 = mFragment.mBatteryLayoutPref.findViewById(R.id.summary1);
        TextView summary2 = mFragment.mBatteryLayoutPref.findViewById(R.id.summary2);
        Robolectric.flushBackgroundThreadScheduler();
        assertThat(summary2.getText().toString()).contains(NEW_ML_EST_SUFFIX);
        assertThat(summary1.getText().toString()).contains(OLD_EST_SUFFIX);
    }

    @Test
    public void testDebugMode() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).isEstimateDebugEnabled();
        doReturn(new TextView(mRealContext)).when(mBatteryLayoutPref).findViewById(R.id.summary2);

        mFragment.restartBatteryInfoLoader();
        ArgumentCaptor<View.OnLongClickListener> listener = ArgumentCaptor.forClass(
                View.OnLongClickListener.class);
        verify(mSummary1).setOnLongClickListener(listener.capture());

        // Calling the listener should disable it.
        listener.getValue().onLongClick(mSummary1);
        verify(mSummary1).setOnLongClickListener(null);

        // Restarting the loader should reset the listener.
        mFragment.restartBatteryInfoLoader();
        verify(mSummary1, times(2)).setOnLongClickListener(any(View.OnLongClickListener.class));
    }

    @Test
    public void testRestartBatteryStatsLoader_notClearHeader_quickUpdateNotInvoked() {
        mFragment.mBatteryHeaderPreferenceController = mBatteryHeaderPreferenceController;

        mFragment.restartBatteryStatsLoader(false /* clearHeader */);

        verify(mBatteryHeaderPreferenceController, never()).quickUpdateHeaderPreference();
    }

    public static class TestFragment extends PowerUsageSummary {
        private Context mContext;

        public TestFragment(Context context) {
            mContext = context;
        }

        @Override
        public Context getContext() {
            return mContext;
        }


        @Override
        protected void refreshUi() {
            // Leave it empty for toggle apps menu test
        }

        @Override
        void showBothEstimates() {
            List<BatteryInfo> fakeBatteryInfo = new ArrayList<>(2);
            BatteryInfo info1 = new BatteryInfo();
            info1.batteryLevel = 10;
            info1.remainingTimeUs = 10000;
            info1.discharging = true;

            BatteryInfo info2 = new BatteryInfo();
            info2.batteryLevel = 10;
            info2.remainingTimeUs = 10000;
            info2.discharging = true;

            fakeBatteryInfo.add(info1);
            fakeBatteryInfo.add(info2);
            updateViews(fakeBatteryInfo);
        }
    }
}
