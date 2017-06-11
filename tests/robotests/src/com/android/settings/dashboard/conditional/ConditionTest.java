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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConditionTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;
    private TestCondition mCondition;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mCondition = new TestCondition(mConditionManager, mMetricsFeatureProvider);
        when(mConditionManager.getContext()).thenReturn(mContext);
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

    @Test
    public void onSilenceChanged_silenced_shouldRegisterReceiver() {
        mCondition.onSilenceChanged(true);

        verify(mContext).registerReceiver(
            TestCondition.mReceiver, TestCondition.TESTS_INTENT_FILTER);
    }

    @Test
    public void onSilenceChanged_notSilenced_registered_shouldUnregisterReceiver() {
        mCondition.onSilenceChanged(true);

        mCondition.onSilenceChanged(false);

        verify(mContext).unregisterReceiver(TestCondition.mReceiver);
    }

    @Test
    public void onSilenceChanged_notSilenced_notRegistered_shouldNotCrash() {
        mCondition.onSilenceChanged(false);

        verify(mContext, never()).unregisterReceiver(TestCondition.mReceiver);
        // no crash
    }

    private static final class TestCondition extends Condition {

        private static final int TEST_METRIC_CONSTANT = 1234;
        private static final IntentFilter TESTS_INTENT_FILTER = new IntentFilter("TestIntent");
        private static final BroadcastReceiver mReceiver = mock(BroadcastReceiver.class);

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

        @Override
        public BroadcastReceiver getReceiver() {
            return mReceiver;
        }

        @Override
        public IntentFilter getIntentFilter() {
            return TESTS_INTENT_FILTER;
        }

    }
}
