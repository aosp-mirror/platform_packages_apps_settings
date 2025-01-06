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
package com.android.settings.accessibility;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.ACTION_RESTORED;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.ACTION_OPEN_SETTINGS;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.NOTIFICATION_CHANNEL;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.NOTIFICATION_ID;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.PromptState.PROMPT_SHOWN;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.PromptState.PROMPT_UNNECESSARY;
import static com.android.settings.accessibility.HighContrastTextMigrationReceiver.PromptState.UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.graphics.hwui.flags.Flags;
import com.android.settings.R;
import com.android.settings.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

/** Tests for {@link HighContrastTextMigrationReceiver}. */
@RunWith(RobolectricTestRunner.class)
public class HighContrastTextMigrationReceiverTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private HighContrastTextMigrationReceiver mReceiver;
    private ShadowApplication mShadowApplication;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = Shadows.shadowOf(notificationManager);
        mShadowApplication = Shadows.shadowOf((Application) mContext);

        // Setup Settings app as a system app
        ShadowPackageManager shadowPm = Shadows.shadowOf(mContext.getPackageManager());
        ComponentName textReadingComponent = new ComponentName(Utils.SETTINGS_PACKAGE_NAME,
                com.android.settings.Settings.TextReadingSettingsActivity.class.getName());
        ActivityInfo activityInfo = shadowPm.addActivityIfNotPresent(textReadingComponent);
        activityInfo.applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        shadowPm.addOrUpdateActivity(activityInfo);

        mReceiver = new HighContrastTextMigrationReceiver();
    }

    @Test
    @DisableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onReceive_flagOff_settingsNotSet() {
        mReceiver.onReceive(mContext, new Intent(ACTION_RESTORED));

        assertPromptStateAndHctState(/* promptState= */ UNKNOWN, /* hctState= */ OFF);
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onRestored_hctStateOn_showPromptHctKeepsOn() {
        setPromptStateAndHctState(/* promptState= */ UNKNOWN, /* hctState= */ ON);

        mReceiver.onReceive(mContext, new Intent(ACTION_RESTORED));

        assertPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, ON);
        verifyNotificationSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onRestored_hctStateOff_showPromptHctKeepsOff() {
        setPromptStateAndHctState(/* promptState= */ UNKNOWN, /* hctState= */ OFF);

        mReceiver.onReceive(mContext, new Intent(ACTION_RESTORED));

        assertPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, OFF);
        verifyNotificationSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateUnknownHctOn_showPromptAndAutoDisableHct() {
        setPromptStateAndHctState(/* promptState= */ UNKNOWN, /* hctState= */ ON);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, /* hctState= */ OFF);
        verifyNotificationSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateUnknownAndHctOff_promptIsUnnecessaryHctKeepsOff() {
        setPromptStateAndHctState(/* promptState= */ UNKNOWN, /* hctState= */ OFF);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_UNNECESSARY, /* hctState= */ OFF);
        verifyNotificationNotSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateShownAndHctOn_promptStateUnchangedHctKeepsOn() {
        setPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, /* hctState= */ ON);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, /* hctState= */ ON);
        verifyNotificationNotSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateShownAndHctOff_promptStateUnchangedHctKeepsOff() {
        setPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, /* hctState= */ OFF);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_SHOWN, /* hctState= */ OFF);
        verifyNotificationNotSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateUnnecessaryAndHctOn_promptStateUnchangedHctKeepsOn() {
        setPromptStateAndHctState(/* promptState= */ PROMPT_UNNECESSARY, /* hctState= */ ON);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_UNNECESSARY, /* hctState= */ ON);
        verifyNotificationNotSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onPreBootCompleted_promptStateUnnecessaryHctOff_promptStateUnchangedHctKeepsOff() {
        setPromptStateAndHctState(/* promptState= */ PROMPT_UNNECESSARY, /* hctState= */ OFF);

        Intent intent = new Intent(Intent.ACTION_PRE_BOOT_COMPLETED);
        mReceiver.onReceive(mContext, intent);

        assertPromptStateAndHctState(/* promptState= */ PROMPT_UNNECESSARY, /* hctState= */ OFF);
        verifyNotificationNotSent();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIGH_CONTRAST_TEXT_SMALL_TEXT_RECT)
    public void onReceive_openSettingsIntent_openHighContrastTextPreference() {
        Intent intent = new Intent(ACTION_OPEN_SETTINGS);
        mReceiver.onReceive(mContext, intent);

        List<Intent> broadcastIntents = mShadowApplication.getBroadcastIntents();
        assertThat(broadcastIntents.size()).isEqualTo(1);
        assertThat(broadcastIntents.get(0).getAction())
                .isEqualTo(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        Intent startedActivitie = mShadowApplication.getNextStartedActivity();
        assertThat(startedActivitie).isNotNull();
        Bundle fragmentArgs = startedActivitie.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(fragmentArgs).isNotNull();
        assertThat(fragmentArgs.getString(EXTRA_FRAGMENT_ARG_KEY))
                .isEqualTo(TextReadingPreferenceFragment.HIGH_TEXT_CONTRAST_KEY);

        Notification notification = mShadowNotificationManager.getNotification(NOTIFICATION_ID);
        assertThat(notification).isNull();
    }

    private void verifyNotificationNotSent() {
        Notification notification = mShadowNotificationManager.getNotification(NOTIFICATION_ID);
        assertThat(notification).isNull();
    }

    private void verifyNotificationSent() {
        // Verify hct channel created
        assertThat(mShadowNotificationManager.getNotificationChannels().stream().anyMatch(
                channel -> channel.getId().equals(NOTIFICATION_CHANNEL))).isTrue();

        // Verify hct notification is sent with correct content
        Notification notification = mShadowNotificationManager.getNotification(NOTIFICATION_ID);
        assertThat(notification).isNotNull();

        ShadowNotification shadowNotification = Shadows.shadowOf(notification);
        assertThat(shadowNotification.getContentTitle()).isEqualTo(mContext.getString(
                R.string.accessibility_notification_high_contrast_text_title));
        assertThat(shadowNotification.getContentText()).isEqualTo(
                mContext.getString(R.string.accessibility_notification_high_contrast_text_content));

        assertThat(notification.actions.length).isEqualTo(1);
        assertThat(notification.actions[0].title.toString()).isEqualTo(
                mContext.getString(R.string.accessibility_notification_high_contrast_text_action));
    }

    private void assertPromptStateAndHctState(
            @HighContrastTextMigrationReceiver.PromptState int promptState,
            @AccessibilityUtil.State int hctState) {
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HCT_RECT_PROMPT_STATUS, UNKNOWN))
                .isEqualTo(promptState);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, OFF))
                .isEqualTo(hctState);
    }

    private void setPromptStateAndHctState(
            @HighContrastTextMigrationReceiver.PromptState int promptState,
            @AccessibilityUtil.State int hctState) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HCT_RECT_PROMPT_STATUS, promptState);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, hctState);
    }
}
