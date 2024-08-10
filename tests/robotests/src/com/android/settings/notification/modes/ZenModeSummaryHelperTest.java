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

import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_AMBIENT;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_LIGHTS;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;

import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Random;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeSummaryHelperTest {
    private static final int WORK_PROFILE_ID = 3;

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

        UserInfo workProfile = new UserInfo(WORK_PROFILE_ID, "Work Profile", 0);
        workProfile.userType = UserManager.USER_TYPE_PROFILE_MANAGED;
        UserManager userManager = mContext.getSystemService(UserManager.class);
        shadowOf(userManager).addProfile(mContext.getUserId(), WORK_PROFILE_ID, workProfile);
    }

    @Test
    public void getPeopleSummary_noOne() {
        ZenPolicy policy = new ZenPolicy.Builder().disallowAllSounds().build();

        assertThat(mSummaryHelper.getPeopleSummary(policy)).isEqualTo("No one can interrupt");
    }

    @Test
    public void getPeopleSummary_some() {
        ZenPolicy policy = new ZenPolicy.Builder().allowCalls(PEOPLE_TYPE_CONTACTS).build();

        assertThat(mSummaryHelper.getPeopleSummary(policy)).isEqualTo("Some people can interrupt");
    }

    @Test
    public void getPeopleSummary_onlyRepeatCallers() {
        ZenPolicy policy = new ZenPolicy.Builder()
                .disallowAllSounds()
                .allowRepeatCallers(true)
                .build();

        assertThat(mSummaryHelper.getPeopleSummary(policy)).isEqualTo(
                "Repeat callers can interrupt");
    }

    @Test
    public void getPeopleSummary_all() {
        ZenPolicy policy = new ZenPolicy.Builder()
                .allowCalls(PEOPLE_TYPE_ANYONE)
                .allowConversations(CONVERSATION_SENDERS_ANYONE)
                .allowMessages(PEOPLE_TYPE_ANYONE)
                .build();

        assertThat(mSummaryHelper.getPeopleSummary(policy)).isEqualTo("All people can interrupt");
    }

    @Test
    public void getMessagesSettingSummary_allMessages() {
        ZenPolicy policy1 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_ANYONE)
                .build();
        ZenPolicy policy2 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_ANYONE)
                .allowConversations(CONVERSATION_SENDERS_IMPORTANT)
                .build();
        ZenPolicy policy3 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_ANYONE)
                .allowConversations(CONVERSATION_SENDERS_ANYONE)
                .build();

        assertThat(mSummaryHelper.getMessagesSettingSummary(policy1)).isEqualTo("Anyone");
        assertThat(mSummaryHelper.getMessagesSettingSummary(policy2)).isEqualTo("Anyone");
        assertThat(mSummaryHelper.getMessagesSettingSummary(policy3)).isEqualTo("Anyone");
    }

    @Test
    public void getMessagesSettingSummary_noMessagesButSomeConversations() {
        ZenPolicy policy1 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_NONE)
                .allowConversations(CONVERSATION_SENDERS_IMPORTANT)
                .build();
        ZenPolicy policy2 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_NONE)
                .allowConversations(CONVERSATION_SENDERS_ANYONE)
                .build();

        assertThat(mSummaryHelper.getMessagesSettingSummary(policy1)).isEqualTo(
                "Priority conversations");
        assertThat(mSummaryHelper.getMessagesSettingSummary(policy2)).isEqualTo(
                "All conversations");
    }

    @Test
    public void getMessagesSettingSummary_contactsAndConversations() {
        ZenPolicy policy1 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_STARRED)
                .allowConversations(CONVERSATION_SENDERS_IMPORTANT)
                .build();
        ZenPolicy policy2 = new ZenPolicy.Builder()
                .allowMessages(PEOPLE_TYPE_STARRED)
                .allowConversations(CONVERSATION_SENDERS_ANYONE)
                .build();

        assertThat(mSummaryHelper.getMessagesSettingSummary(policy1)).isEqualTo(
                "Starred contacts and priority conversations");
        assertThat(mSummaryHelper.getMessagesSettingSummary(policy2)).isEqualTo(
                "Starred contacts and all conversations");
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

        assertThat(mSummaryHelper.getAppsSummary(zenMode, ImmutableList.of())).isEqualTo("None");
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
    public void formatAppsList_listEmpty() {
        ImmutableList<AppEntry> apps = ImmutableList.of();
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("No apps can interrupt");
    }

    @Test
    public void formatAppsList_single() {
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App"));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App can interrupt");
    }

    @Test
    public void formatAppsList_two() {
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App"),
                newAppEntry("SecondApp"));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App and SecondApp "
                + "can interrupt");
    }

    @Test
    public void formatAppsList_three() {
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App"),
                newAppEntry("SecondApp"), newAppEntry("ThirdApp"));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App, SecondApp, "
                + "and ThirdApp can interrupt");
    }

    @Test
    public void formatAppsList_many() {
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App"),
                newAppEntry("SecondApp"), newAppEntry("ThirdApp"), newAppEntry("FourthApp"),
                newAppEntry("FifthApp"), newAppEntry("SixthApp"));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App, SecondApp, "
                + "and 4 more can interrupt");
    }

    @Test
    public void formatAppsList_singleWorkProfile() {
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App", WORK_PROFILE_ID));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App (Work) can interrupt");
    }

    @Test
    public void formatAppsList_mixOfProfiles() {
        ImmutableList<AppEntry> apps = ImmutableList.of(
                newAppEntry("My App", mContext.getUserId()),
                newAppEntry("My App", WORK_PROFILE_ID),
                newAppEntry("SecondApp", mContext.getUserId()));
        assertThat(mSummaryHelper.formatAppsList(apps)).isEqualTo("My App, My App (Work), "
                + "and SecondApp can interrupt");
    }

    @Test
    public void getAppsSummary_priorityApps() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();
        ImmutableList<AppEntry> apps = ImmutableList.of(newAppEntry("My App"),
                newAppEntry("SecondApp"), newAppEntry("ThirdApp"), newAppEntry("FourthApp"),
                newAppEntry("FifthApp"), newAppEntry("SixthApp"));

        assertThat(mSummaryHelper.getAppsSummary(zenMode, apps)).isEqualTo("My App, SecondApp, "
                + "and 4 more can interrupt");
    }

    private AppEntry newAppEntry(String name) {
        return newAppEntry(name, mContext.getUserId());
    }

    private AppEntry newAppEntry(String name, int userId) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.uid = UserHandle.getUid(userId, new Random().nextInt(100));
        AppEntry appEntry = new AppEntry(mContext, applicationInfo, 1);
        appEntry.label = name;
        return appEntry;
    }

    @Test
    public void getModesSummary_noRules_noSummary() {
        String summary = mSummaryHelper.getModesSummary(ImmutableList.of());
        assertThat(summary).isEmpty();
    }

    @Test
    public void getModesSummary_onlyDndAndNotActive_noSummary() {
        ImmutableList<ZenMode> modes = ImmutableList.of(TestModeBuilder.MANUAL_DND_INACTIVE);
        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEmpty();
    }

    @Test
    public void getModesSummary_noRulesActive_countsOnlyEnabledAutomaticModes() {
        ImmutableList<ZenMode> modes = ImmutableList.of(
                TestModeBuilder.MANUAL_DND_INACTIVE, // Not automatic
                new TestModeBuilder().setName("Auto 1").build(), // App provided automatic
                new TestModeBuilder()
                        .setName("Custom manual 1")
                        .setPackage(SystemZenRules.PACKAGE_ANDROID)
                        .setType(AutomaticZenRule.TYPE_OTHER)
                        .setConditionId(ZenModeConfig.toCustomManualConditionId())
                        .build(), // Custom manual, not automatic
                new TestModeBuilder()
                        .setName("Disabled 1")
                        .setEnabled(false)
                        .build(), // Would be automatic, but it's disabled.
                new TestModeBuilder()
                        .setName("Sleep")
                        .setPackage(SystemZenRules.PACKAGE_ANDROID)
                        .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)
                        .build() // Time based, automatic.
        );

        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEqualTo("2 modes can turn on automatically");
    }

    @Test
    public void getModesSummary_oneModeActive_listsMode() {
        ImmutableList<ZenMode> modes = ImmutableList.of(
                TestModeBuilder.MANUAL_DND_ACTIVE,
                new TestModeBuilder().setName("Inactive").setActive(false).build());

        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEqualTo("Do Not Disturb is active");
    }

    @Test
    public void getModesSummary_twoModesActive_listsModes() {
        ImmutableList<ZenMode> modes = ImmutableList.of(
                TestModeBuilder.MANUAL_DND_ACTIVE,
                new TestModeBuilder().setName("Inactive").setActive(false).build(),
                new TestModeBuilder().setName("Active #1").setActive(true).build());

        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEqualTo("Do Not Disturb and Active #1 are active");
    }

    @Test
    public void getModesSummary_threeModesActive_listsModes() {
        ImmutableList<ZenMode> modes = ImmutableList.of(
                TestModeBuilder.MANUAL_DND_INACTIVE,
                new TestModeBuilder().setName("Inactive #1").setActive(false).build(),
                new TestModeBuilder().setName("Active #1").setActive(true).build(),
                new TestModeBuilder().setName("Active #2").setActive(true).build(),
                new TestModeBuilder().setName("Inactive #2").setActive(false).build(),
                new TestModeBuilder().setName("Active #3").setActive(true).build());

        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEqualTo("Active #1, Active #2, and Active #3 are active");
    }

    @Test
    public void getModesSummary_manyModesActive_listsACouple() {
        ImmutableList<ZenMode> modes = ImmutableList.of(
                TestModeBuilder.MANUAL_DND_ACTIVE,
                new TestModeBuilder().setName("Inactive #1").setActive(false).build(),
                new TestModeBuilder().setName("Active #1").setActive(true).build(),
                new TestModeBuilder().setName("Active #2").setActive(true).build(),
                new TestModeBuilder().setName("Inactive #2").setActive(false).build(),
                new TestModeBuilder().setName("Active #3").setActive(true).build());

        String summary = mSummaryHelper.getModesSummary(modes);
        assertThat(summary).isEqualTo("Do Not Disturb, Active #1, and 2 more are active");
    }
}
