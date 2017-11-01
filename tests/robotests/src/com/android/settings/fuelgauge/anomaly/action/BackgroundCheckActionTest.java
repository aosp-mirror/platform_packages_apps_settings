/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly.action;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BackgroundCheckActionTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 111;
    private static final int SDK_VERSION = Build.VERSION_CODES.L;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private BatteryUtils mBatteryUtils;
    private Anomaly mAnomaly;
    private BackgroundCheckAction mBackgroundCheckAction;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        doReturn(mAppOpsManager).when(mContext).getSystemService(Context.APP_OPS_SERVICE);

        mAnomaly = new Anomaly.Builder()
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setTargetSdkVersion(SDK_VERSION)
                .build();
        mBackgroundCheckAction = new BackgroundCheckAction(mContext);
        mBackgroundCheckAction.mBatteryUtils = mBatteryUtils;
    }

    @Test
    public void testHandlePositiveAction_forceStopPackage() {
        mBackgroundCheckAction.handlePositiveAction(mAnomaly, 0 /* metricskey */);

        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, UID, PACKAGE_NAME,
                AppOpsManager.MODE_IGNORED);
    }

    @Test
    public void testIsActionActive_modeAllowed_returnTrue() {
        doReturn(false).when(mBatteryUtils).isBackgroundRestrictionEnabled(SDK_VERSION, UID,
                PACKAGE_NAME);

        assertThat(mBackgroundCheckAction.isActionActive(mAnomaly)).isTrue();
    }

    @Test
    public void testIsActionActive_modeIgnored_returnFalse() {
        doReturn(true).when(mBatteryUtils).isBackgroundRestrictionEnabled(SDK_VERSION, UID,
                PACKAGE_NAME);

        assertThat(mBackgroundCheckAction.isActionActive(mAnomaly)).isFalse();
    }

    @Test
    public void testConstructor_batteryUtilsNotNull() {
        mBackgroundCheckAction = new BackgroundCheckAction(mContext);

        assertThat(mBackgroundCheckAction.mBatteryUtils).isNotNull();
    }
}
