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

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StopAndBackgroundActionTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 111;
    private static final int METRICS_KEY = 3;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private BackgroundCheckAction mBackgroundCheckAction;
    @Mock
    private ForceStopAction mForceStopAction;
    private StopAndBackgroundCheckAction mStopAndBackgroundCheckAction;
    private Anomaly mAnomaly;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mAnomaly = new Anomaly.Builder()
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .build();

        FakeFeatureFactory.setupForTest(mContext);
        mStopAndBackgroundCheckAction = new StopAndBackgroundCheckAction(mContext, mForceStopAction,
                mBackgroundCheckAction);
    }

    @Test
    public void testHandlePositiveAction_stopAndBackgroundCheck() {
        mStopAndBackgroundCheckAction.handlePositiveAction(mAnomaly, METRICS_KEY);

        verify(mBackgroundCheckAction).handlePositiveAction(mAnomaly, METRICS_KEY);
        verify(mForceStopAction).handlePositiveAction(mAnomaly, METRICS_KEY);
    }

    @Test
    public void testIsActionActive_restrictionEnabled_returnFalse() {
        doReturn(true).when(mForceStopAction).isActionActive(mAnomaly);

        assertThat(mStopAndBackgroundCheckAction.isActionActive(mAnomaly)).isFalse();
    }

    @Test
    public void testIsActionActive_appNotRunning_returnFalse() {
        doReturn(true).when(mBackgroundCheckAction).isActionActive(mAnomaly);

        assertThat(mStopAndBackgroundCheckAction.isActionActive(mAnomaly)).isFalse();
    }

    @Test
    public void testIsActionActive_appStoppedAndRestrictionOn_returnFalse() {
        assertThat(mStopAndBackgroundCheckAction.isActionActive(mAnomaly)).isFalse();
    }
}
