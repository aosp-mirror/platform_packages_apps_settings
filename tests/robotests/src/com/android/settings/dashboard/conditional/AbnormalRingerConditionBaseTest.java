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

package com.android.settings.dashboard.conditional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class AbnormalRingerConditionBaseTest {

    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;
    private TestCondition mCondition;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mConditionManager.getContext()).thenReturn(mContext);
        mCondition = new TestCondition(mConditionManager);
    }

    @Test
    public void newInstance_shouldMonitorRingerStateChangeBroadcast() {
        final Intent broadcast1 = new Intent("foo.bar.action");
        final Intent broadcast2 = new Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);

        mContext.sendBroadcast(broadcast1);
        assertThat(mCondition.mRefreshCalled).isFalse();

        mContext.sendBroadcast(broadcast2);
        assertThat(mCondition.mRefreshCalled).isTrue();
    }

    private static class TestCondition extends AbnormalRingerConditionBase {
        private boolean mRefreshCalled;

        TestCondition(ConditionManager manager) {
            super(manager);
        }

        @Override
        public void refreshState() {
            mRefreshCalled = true;
        }

        @Override
        public int getMetricsConstant() {
            return 0;
        }

        @Override
        public Drawable getIcon() {
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

    }
}
