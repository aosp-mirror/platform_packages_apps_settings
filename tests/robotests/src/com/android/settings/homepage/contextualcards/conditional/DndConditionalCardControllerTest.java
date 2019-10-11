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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.ZenModeConfig;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.testutils.shadow.ShadowNotificationManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@Config(shadows = ShadowNotificationManager.class)
@RunWith(RobolectricTestRunner.class)
public class DndConditionalCardControllerTest {

    @Mock
    private ConditionManager mConditionManager;
    private Context mContext;
    private DndConditionCardController mController;
    private ShadowNotificationManager mNotificationManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new DndConditionCardController(mContext, mConditionManager);
        mNotificationManager = Shadow.extract(mContext.getSystemService(NotificationManager.class));
    }

    @Test
    public void cycleMonitoring_shouldRegisterAndUnregisterReceiver() {
        mController.startMonitoringStateChange();
        mController.stopMonitoringStateChange();

        verify(mContext).registerReceiver(any(DndConditionCardController.Receiver.class),
                eq(DndConditionCardController.DND_FILTER));
        verify(mContext).unregisterReceiver(any(DndConditionCardController.Receiver.class));
    }

    @Test
    public void buildContextualCard_allSoundsMuted_shouldHavePhoneMutedSummary() {
        mNotificationManager.setZenModeConfig(getMutedAllConfig());

        final ContextualCard card = mController.buildContextualCard();

        assertThat(card.getSummaryText()).isEqualTo(
                mContext.getString(R.string.condition_zen_summary_phone_muted));
    }

    @Test
    public void buildContextualCard_allowSomeSounds_shouldHaveWittExceptionsSummary() {
        mNotificationManager.setZenModeConfig(getCustomConfig());

        final ContextualCard card = mController.buildContextualCard();

        assertThat(card.getSummaryText()).isEqualTo(
                mContext.getString(R.string.condition_zen_summary_with_exceptions));
    }

    private ZenModeConfig getCustomConfig() {
        final ZenModeConfig config = new ZenModeConfig();
        // Some sounds allowed
        config.allowAlarms = true;
        config.allowMedia = false;
        config.allowSystem = false;
        config.allowCalls = true;
        config.allowRepeatCallers = true;
        config.allowMessages = false;
        config.allowReminders = false;
        config.allowEvents = false;
        config.areChannelsBypassingDnd = false;
        config.allowCallsFrom = ZenModeConfig.SOURCE_ANYONE;
        config.allowMessagesFrom = ZenModeConfig.SOURCE_ANYONE;
        config.suppressedVisualEffects = 0;
        return config;
    }

    private ZenModeConfig getMutedAllConfig() {
        final ZenModeConfig config = new ZenModeConfig();
        // No sounds allowed
        config.allowAlarms = false;
        config.allowMedia = false;
        config.allowSystem = false;
        config.allowCalls = false;
        config.allowRepeatCallers = false;
        config.allowMessages = false;
        config.allowReminders = false;
        config.allowEvents = false;
        config.areChannelsBypassingDnd = false;
        config.suppressedVisualEffects = 0;
        return config;
    }
}
