/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static com.google.common.truth.Truth.assertThat;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.net.Uri;
import android.service.notification.ZenPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ZenModesSummaryHelperTest {
    private Context mContext;
    private ZenModesBackend mBackend;

    private ZenModeSummaryHelper mSummaryHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mBackend = new ZenModesBackend(mContext);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, mBackend);
    }

    @Test
    public void getPeopleSummary_noOne() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().disallowAllSounds().build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("No one can interrupt");
    }

    @Test
    public void getPeopleSummary_some() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowCalls(PEOPLE_TYPE_CONTACTS).build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("Some people can interrupt");
    }

    @Test
    public void getPeopleSummary_all() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowCalls(PEOPLE_TYPE_ANYONE).
                        allowConversations(CONVERSATION_SENDERS_ANYONE)
                        .allowMessages(PEOPLE_TYPE_ANYONE).build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("All people can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_single() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_duo() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).allowMedia(true).build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms and media can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_trio() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and touch sounds can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_quad() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .allowReminders(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and 2 more can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_all() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .allowReminders(true)
                        .allowEvents(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and 3 more can interrupt");
    }
}
