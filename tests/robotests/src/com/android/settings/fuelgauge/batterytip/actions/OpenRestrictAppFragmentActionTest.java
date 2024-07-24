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
package com.android.settings.fuelgauge.batterytip.actions;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OpenRestrictAppFragmentActionTest {

    private static final String PACKAGE_NAME_1 = "com.android.app1";
    private static final String PACKAGE_NAME_2 = "com.android.app2";
    private static final int ANOMALY_WAKEUP = 0;
    private static final int ANOMALY_BT = 1;
    private static final int METRICS_KEY = 1;

    @Mock private InstrumentedPreferenceFragment mFragment;
    @Mock private BatteryDatabaseManager mBatteryDatabaseManager;
    private OpenRestrictAppFragmentAction mAction;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private List<AppInfo> mAppInfos;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mAppInfos = new ArrayList<>();
        mAppInfos.add(
                new AppInfo.Builder()
                        .setPackageName(PACKAGE_NAME_1)
                        .addAnomalyType(ANOMALY_BT)
                        .build());
        mAppInfos.add(
                new AppInfo.Builder()
                        .setPackageName(PACKAGE_NAME_2)
                        .addAnomalyType(ANOMALY_WAKEUP)
                        .build());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFragment.getContext()).thenReturn(mContext);

        mAction =
                new OpenRestrictAppFragmentAction(
                        mFragment, new RestrictAppTip(BatteryTip.StateType.HANDLED, mAppInfos));
        mAction.mBatteryDatabaseManager = mBatteryDatabaseManager;
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Ignore
    @Test
    public void testHandlePositiveAction() {
        mAction.handlePositiveAction(METRICS_KEY);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        MetricsProto.MetricsEvent.ACTION_TIP_OPEN_APP_RESTRICTION_PAGE,
                        METRICS_KEY);
        verify(mBatteryDatabaseManager)
                .updateAnomalies(mAppInfos, AnomalyDatabaseHelper.State.HANDLED);
    }
}
