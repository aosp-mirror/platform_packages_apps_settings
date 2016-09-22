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
 * limitations under the License
 */
package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.graphics.drawable.Icon;

import com.android.internal.logging.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConditionTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private TestCondition mCondition;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCondition = new TestCondition(mConditionManager, mMetricsFeatureProvider);
    }

    @Test
    public void initialize_shouldNotBeSilenced() {
        assertThat(mCondition.isSilenced()).isFalse();
    }

    @Test
    public void silence_shouldNotifyDataChangeAndLog() {
        mCondition.silence();

        assertThat(mCondition.isSilenced()).isTrue();
        verify(mConditionManager).notifyChanged(mCondition);
        verify(mMetricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_CONDITION_DISMISS),
                eq(TestCondition.TEST_METRIC_CONSTANT));
    }

    private static final class TestCondition extends Condition {

        private static final int TEST_METRIC_CONSTANT = 1234;

        TestCondition(ConditionManager manager,
                MetricsFeatureProvider metricsFeatureProvider) {
            super(manager, metricsFeatureProvider);
        }

        @Override
        public void refreshState() {

        }

        @Override
        public int getMetricsConstant() {
            return TEST_METRIC_CONSTANT;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public CharSequence getSummary() {
            return null;
        }

        @Override
        public CharSequence[] getActions() {
            return new CharSequence[0];
        }

        @Override
        public void onPrimaryClick() {

        }

        @Override
        public void onActionClick(int index) {

        }
    }
}
