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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.app.AppOpsManager;
import android.content.Context;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
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
public class AnomalyActionTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 111;
    private static final int ACTION_KEY = 2;
    private static final int METRIC_KEY = 3;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AppOpsManager mAppOpsManagerr;
    private Anomaly mAnomaly;
    private TestAnomalyAction mTestAnomalyAction;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        doReturn(mAppOpsManagerr).when(mContext).getSystemService(Context.APP_OPS_SERVICE);

        mAnomaly = new Anomaly.Builder()
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .build();
        mTestAnomalyAction = new TestAnomalyAction(mContext);
    }

    @Test
    public void testHandlePositiveAction_logAction() {
        mTestAnomalyAction.handlePositiveAction(mAnomaly, METRIC_KEY);

        verify(mFeatureFactory.metricsFeatureProvider).action(mContext, ACTION_KEY, PACKAGE_NAME,
                Pair.create(MetricsProto.MetricsEvent.FIELD_CONTEXT, METRIC_KEY));
    }

    /**
     * Test class for {@link AnomalyAction}
     */
    public class TestAnomalyAction extends AnomalyAction {
        public TestAnomalyAction(Context context) {
            super(context);
            mActionMetricKey = ACTION_KEY;
        }

        @Override
        public boolean isActionActive(Anomaly anomaly) {
            return false;
        }

        @Override
        public int getActionType() {
            return 0;
        }
    }
}
