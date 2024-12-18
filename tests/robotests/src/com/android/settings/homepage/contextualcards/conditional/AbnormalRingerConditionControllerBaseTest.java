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

package com.android.settings.homepage.contextualcards.conditional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.homepage.contextualcards.ContextualCard;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class AbnormalRingerConditionControllerBaseTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;
    private TestCondition mCondition;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        mCondition = new TestCondition(mContext, mConditionManager);
    }

    @Test
    public void startMonitor_shouldMonitorRingerStateChangeBroadcast() {
        final Intent broadcast1 = new Intent("foo.bar.action");
        final Intent broadcast2 = new Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);

        mCondition.startMonitoringStateChange();

        mContext.sendBroadcast(broadcast1);
        ShadowLooper.idleMainLooper();

        verify(mConditionManager, never()).onConditionChanged();

        mContext.sendBroadcast(broadcast2);
        ShadowLooper.idleMainLooper();

        verify(mConditionManager).onConditionChanged();
    }

    private static class TestCondition extends AbnormalRingerConditionController {

        private TestCondition(Context appContext, ConditionManager conditionManager) {
            super(appContext, conditionManager);
        }


        @Override
        public long getId() {
            return 0;
        }

        @Override
        public boolean isDisplayable() {
            return false;
        }

        @Override
        public ContextualCard buildContextualCard() {
            return new ConditionalContextualCard.Builder().build();
        }
    }
}
