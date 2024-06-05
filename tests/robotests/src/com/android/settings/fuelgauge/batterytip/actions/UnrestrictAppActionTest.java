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

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UnrestrictAppActionTest {

    private static final int UID_1 = 12345;
    private static final String PACKAGE_NAME_1 = "com.android.app1";
    private static final int METRICS_KEY = 1;

    @Mock private BatteryUtils mBatteryUtils;
    private UnrestrictAppAction mUnrestrictAppAction;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final AppInfo appInfo =
                new AppInfo.Builder().setUid(UID_1).setPackageName(PACKAGE_NAME_1).build();
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        mUnrestrictAppAction =
                new UnrestrictAppAction(
                        RuntimeEnvironment.application,
                        new UnrestrictAppTip(BatteryTip.StateType.NEW, appInfo));
        mUnrestrictAppAction.mBatteryUtils = mBatteryUtils;
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(RuntimeEnvironment.application);
    }

    @Test
    public void testHandlePositiveAction() {
        mUnrestrictAppAction.handlePositiveAction(METRICS_KEY);

        verify(mBatteryUtils).setForceAppStandby(UID_1, PACKAGE_NAME_1, AppOpsManager.MODE_ALLOWED);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        MetricsProto.MetricsEvent.ACTION_TIP_UNRESTRICT_APP,
                        METRICS_KEY,
                        PACKAGE_NAME_1,
                        0);
    }
}
