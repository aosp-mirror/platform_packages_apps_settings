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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import android.app.AppOpsManager;
import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RestrictAppActionTest {
    private static final String PACKAGE_NAME_1 = "com.android.app1";
    private static final String PACKAGE_NAME_2 = "com.android.app2";

    @Mock
    private BatteryUtils mBatteryUtils;
    private Context mContext;
    private RestrictAppAction mRestrictAppAction;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        final List<AppInfo> mAppInfos = new ArrayList<>();
        mAppInfos.add(new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME_1)
                .build());
        mAppInfos.add(new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME_2)
                .build());

        mRestrictAppAction = new RestrictAppAction(mContext, new RestrictAppTip(
                BatteryTip.StateType.NEW, mAppInfos));
        mRestrictAppAction.mBatteryUtils = mBatteryUtils;
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testHandlePositiveAction() {
        mRestrictAppAction.handlePositiveAction();

        verify(mBatteryUtils).setForceAppStandby(anyInt(), eq(PACKAGE_NAME_1),
                eq(AppOpsManager.MODE_IGNORED));
        verify(mBatteryUtils).setForceAppStandby(anyInt(), eq(PACKAGE_NAME_2),
                eq(AppOpsManager.MODE_IGNORED));
    }

}
