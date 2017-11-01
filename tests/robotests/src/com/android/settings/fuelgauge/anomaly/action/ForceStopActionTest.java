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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

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
public class ForceStopActionTest {
    private static final String PACKAGE_NAME = "com.android.app";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private PackageManager mPackageManager;
    private Anomaly mAnomaly;
    private ForceStopAction mForceStopAction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        doReturn(mActivityManager).when(mContext).getSystemService(Context.ACTIVITY_SERVICE);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME,
                PackageManager.GET_META_DATA);

        mAnomaly = new Anomaly.Builder()
                .setPackageName(PACKAGE_NAME)
                .build();
        mForceStopAction = new ForceStopAction(mContext);
    }

    @Test
    public void testHandlePositiveAction_forceStopPackage() {
        mForceStopAction.handlePositiveAction(mAnomaly, 0 /* metricskey */);

        verify(mActivityManager).forceStopPackage(PACKAGE_NAME);
    }

    @Test
    public void testIsActionActive_appStopped_returnFalse() {
        mApplicationInfo.flags = ApplicationInfo.FLAG_STOPPED;

        assertThat(mForceStopAction.isActionActive(mAnomaly)).isFalse();
    }

    @Test
    public void testIsActionActive_appRunning_returnTrue() {
        mApplicationInfo.flags = 0;

        assertThat(mForceStopAction.isActionActive(mAnomaly)).isTrue();
    }
}
