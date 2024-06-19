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
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_AMBIENT;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_LIGHTS;

import static com.google.common.truth.Truth.assertThat;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.net.Uri;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.LinkedHashSet;
import java.util.Set;

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

    @Test
    public void getBlockedEffectsSummary_none() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .allowAlarms(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);
        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications shown");
    }

    @Test
    public void getBlockedEffectsSummary_some() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_AMBIENT, false)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);
        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications partially hidden");
    }

    @Test
    public void getBlockedEffectsSummary_all() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .hideAllVisualEffects()
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);
        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications hidden");
    }

    @Test
    public void getDisplayEffectsSummary_single_notifVis() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_AMBIENT, false)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications partially hidden");
    }

    @Test
    public void getDisplayEffectsSummary_single_notifVis_unusedEffect() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_LIGHTS, false)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications shown");
    }

    @Test
    public void getDisplayEffectsSummary_single_displayEffect() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().showAllVisualEffects().build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_duo() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().showAllVisualEffects().build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .setShouldDisplayGrayscale(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Grayscale and dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_trio() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .hideAllVisualEffects()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDisplayGrayscale(true)
                        .setShouldDimWallpaper(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications hidden, grayscale, and dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_quad() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_AMBIENT, false)
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .setShouldDisplayGrayscale(true)
                        .setShouldUseNightMode(true)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications partially hidden, grayscale, and 2 more");
    }

    @Test
    public void getAppsSummary_all() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenMode.CHANNEL_POLICY_ALL)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getAppsSummary(zenMode, new LinkedHashSet<>())).isEqualTo("All");
    }

    @Test
    public void getAppsSummary_none() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_NONE)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getAppsSummary(zenMode, new LinkedHashSet<>())).isEqualTo("None");
    }

    @Test
    public void getAppsSummary_priorityAppsNoList() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);

        assertThat(mSummaryHelper.getAppsSummary(zenMode, null)).isEqualTo("Selected apps");
    }

    @Test
    public void getAppsSummary_formatAppsListEmpty() {
        Set<String> apps = new LinkedHashSet<>();
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("No apps can interrupt");
    }

    @Test
    public void getAppsSummary_formatAppsListSingle() {
        Set<String> apps = Set.of("My App");
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App can interrupt");
    }

    @Test
    public void getAppsSummary_formatAppsListTwo() {
        Set<String> apps = Set.of("My App", "SecondApp");
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App and SecondApp "
                + "can interrupt");
    }

    @Test
    public void getAppsSummary_formatAppsListThree() {
        Set<String> apps = Set.of("My App", "SecondApp", "ThirdApp");
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App, SecondApp, "
                + "and ThirdApp can interrupt");
    }

    @Test
    public void getAppsSummary_formatAppsListMany() {
        Set<String> apps = Set.of("My App", "SecondApp", "ThirdApp", "FourthApp",
                "FifthApp", "SixthApp");
        // Note that apps are selected alphabetically.
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("FifthApp, FourthApp, "
                + "and 4 more can interrupt");
    }

    @Test
    public void getAppsSummary_priorityApps() {
        AutomaticZenRule rule = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();
        ZenMode zenMode = new ZenMode("id", rule, true);
        Set<String> apps = Set.of("My App", "SecondApp", "ThirdApp", "FourthApp",
                "FifthApp", "SixthApp");

        assertThat(mSummaryHelper.getAppsSummary(zenMode, apps)).isEqualTo("FifthApp, FourthApp, "
                + "and 4 more can interrupt");
    }

}
