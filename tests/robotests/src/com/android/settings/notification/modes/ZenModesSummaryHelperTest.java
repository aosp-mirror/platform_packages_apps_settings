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

import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_OFF;
import static android.service.notification.Condition.SOURCE_UNKNOWN;
import static android.service.notification.Condition.STATE_TRUE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_AMBIENT;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_LIGHTS;

import static com.google.common.truth.Truth.assertThat;

import android.app.Flags;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.Condition;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;

import org.junit.Before;
import org.junit.Rule;
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
    private ZenHelperBackend mBackend;

    private ZenModeSummaryHelper mSummaryHelper;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(
            SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mBackend = new ZenHelperBackend(mContext);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, mBackend);
    }

    @Test
    public void getPeopleSummary_noOne() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().disallowAllSounds().build())
                .build();

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("No one can interrupt");
    }

    @Test
    public void getPeopleSummary_some() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowCalls(PEOPLE_TYPE_CONTACTS).build())
                .build();

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("Some people can interrupt");
    }

    @Test
    public void getPeopleSummary_all() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowCalls(PEOPLE_TYPE_ANYONE).
                        allowConversations(CONVERSATION_SENDERS_ANYONE)
                        .allowMessages(PEOPLE_TYPE_ANYONE).build())
                .build();

        assertThat(mSummaryHelper.getPeopleSummary(zenMode)).isEqualTo("All people can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_single() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                .build();

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_duo() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).allowMedia(true).build())
                .build();

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms and media can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_trio() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and touch sounds can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_quad() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .allowReminders(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and 2 more can interrupt");
    }

    @Test
    public void getOtherSoundCategoriesSummary_all() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowSystem(true)
                        .allowReminders(true)
                        .allowEvents(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode)).isEqualTo(
                "Alarms, media, and 3 more can interrupt");
    }

    @Test
    public void getBlockedEffectsSummary_none() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .allowAlarms(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications shown");
    }

    @Test
    public void getBlockedEffectsSummary_some() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_AMBIENT, false)
                        .build())
                .build();

        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications partially hidden");
    }

    @Test
    public void getBlockedEffectsSummary_all() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .hideAllVisualEffects()
                        .build())
                .build();

        assertThat(mSummaryHelper.getBlockedEffectsSummary(zenMode))
                .isEqualTo("Notifications hidden");
    }

    @Test
    public void getDisplayEffectsSummary_single_notifVis() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_AMBIENT, false)
                        .build())
                .build();

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications partially hidden");
    }

    @Test
    public void getDisplayEffectsSummary_single_notifVis_unusedEffect() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .showAllVisualEffects()
                        .showVisualEffect(VISUAL_EFFECT_LIGHTS, false)
                        .build())
                .build();

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications shown");
    }

    @Test
    public void getDisplayEffectsSummary_single_displayEffect() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().showAllVisualEffects().build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_duo() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().showAllVisualEffects().build())
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .setShouldDisplayGrayscale(true)
                        .build())
                .build();

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Grayscale and dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_trio() {
        ZenMode zenMode = new TestModeBuilder()
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

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications hidden, grayscale, and dim the wallpaper");
    }

    @Test
    public void getDisplayEffectsSummary_quad() {
        ZenMode zenMode = new TestModeBuilder()
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

        assertThat(mSummaryHelper.getDisplayEffectsSummary(zenMode)).isEqualTo(
                "Notifications partially hidden, grayscale, and 2 more");
    }

    @Test
    public void getAppsSummary_none() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_NONE)
                        .build())
                .build();

        assertThat(mSummaryHelper.getAppsSummary(zenMode, new LinkedHashSet<>())).isEqualTo("None");
    }

    @Test
    public void getAppsSummary_priorityAppsNoList() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();

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
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();
        Set<String> apps = Set.of("My App", "SecondApp", "ThirdApp", "FourthApp",
                "FifthApp", "SixthApp");

        assertThat(mSummaryHelper.getAppsSummary(zenMode, apps)).isEqualTo("FifthApp, FourthApp, "
                + "and 4 more can interrupt");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_off_noRules() {
        ZenModeConfig config = new ZenModeConfig();

        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_OFF, config)).isEqualTo("Off");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_off_oneRule() {
        ZenModeConfig config = new ZenModeConfig();
        ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
        rule.enabled = true;
        config.automaticRules.put("key", rule);

        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_OFF, config))
                .isEqualTo("Off / 1 mode can turn on automatically");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_off_twoRules() {
        ZenModeConfig config = new ZenModeConfig();
        ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
        rule.enabled = true;
        ZenModeConfig.ZenRule rule2 = new ZenModeConfig.ZenRule();
        rule2.enabled = true;
        config.automaticRules.put("key", rule);
        config.automaticRules.put("key2", rule2);

        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_OFF, config))
                .isEqualTo("Off / 2 modes can turn on automatically");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_on_noDescription() {
        ZenModeConfig config = new ZenModeConfig();
        config.manualRule.conditionId = Uri.EMPTY;
        config.manualRule.pkg = "android";
        config.manualRule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        config.manualRule.condition = new Condition(Uri.EMPTY, "", STATE_TRUE, SOURCE_UNKNOWN);
        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_IMPORTANT_INTERRUPTIONS, config))
                .isEqualTo("On");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_on_manualDescription() {
        ZenModeConfig config = new ZenModeConfig();
        config.manualRule.conditionId = ZenModeConfig.toCountdownConditionId(
                System.currentTimeMillis() + 10000, false);
        config.manualRule.pkg = "android";
        config.manualRule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        config.manualRule.condition = new Condition(Uri.EMPTY, "", STATE_TRUE, SOURCE_UNKNOWN);
        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_IMPORTANT_INTERRUPTIONS, config))
                .startsWith("On /");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getSoundSummary_on_automatic() {
        ZenModeConfig config = new ZenModeConfig();
        ZenModeConfig.ZenRule rule = new ZenModeConfig.ZenRule();
        rule.configurationActivity = new ComponentName("a", "a");
        rule.component = new ComponentName("b", "b");
        rule.conditionId = new Uri.Builder().scheme("hello").build();
        rule.condition = new Condition(rule.conditionId, "", STATE_TRUE);
        rule.enabled = true;
        rule.creationTime = 123;
        rule.id = "id";
        rule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        rule.modified = true;
        rule.name = "name";
        rule.snoozing = false;
        rule.pkg = "b";
        config.automaticRules.put("key", rule);

        assertThat(mSummaryHelper.getSoundSummary(ZEN_MODE_IMPORTANT_INTERRUPTIONS, config))
                .startsWith("On /");
    }
}
