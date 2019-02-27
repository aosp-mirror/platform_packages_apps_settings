/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppNotificationSettingsTest {
    private static final String WM_DISMISS_KEYGUARD_COMMAND = "wm dismiss-keyguard";

    private UiDevice mUiDevice;
    private Context mTargetContext;
    private Instrumentation mInstrumentation;

    NotificationManager mNm;
    private NotificationChannelGroup mGroup1;
    private NotificationChannel mGroup1Channel1;
    private NotificationChannel mGroup1Channel2;
    private NotificationChannelGroup mGroup2;
    private NotificationChannel mGroup2Channel1;
    private NotificationChannel mUngroupedChannel;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();

        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mUiDevice.wakeUp();
        mUiDevice.executeShellCommand(WM_DISMISS_KEYGUARD_COMMAND);

        mNm  = (NotificationManager) mTargetContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mGroup1 = new NotificationChannelGroup(this.getClass().getName() + "1", "group1");
        mGroup2 = new NotificationChannelGroup(this.getClass().getName() + "2", "group2");
        mNm.createNotificationChannelGroup(mGroup1);
        mNm.createNotificationChannelGroup(mGroup2);

        mGroup1Channel1 = createChannel(mGroup1, this.getClass().getName()+ "c1-1");
        mGroup1Channel2 = createChannel(mGroup1, this.getClass().getName()+ "c1-2");
        mGroup2Channel1 = createChannel(mGroup2, this.getClass().getName()+ "c2-1");
        mUngroupedChannel = createChannel(null, this.getClass().getName()+ "c");
    }

    @Test
    public void launchNotificationSetting_shouldNotHaveAppInfoLink() {
        final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mInstrumentation.startActivitySync(intent);

        onView(allOf(withId(android.R.id.button1),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .check(doesNotExist());
    }

    @Test
    public void launchNotificationSetting_showGroupsWithMultipleChannels() {
        final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);
        onView(allOf(withText(mGroup1.getName().toString()))).check(
                matches(isDisplayed()));
        onView(allOf(withText(mGroup1Channel1.getName().toString()))).check(
                matches(isDisplayed()));
        onView(allOf(withText(mGroup1Channel2.getName().toString()))).check(
                matches(isDisplayed()));
    }

    @Test
    public void launchNotificationSetting_showUngroupedChannels() {
        final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);
        onView(allOf(withText(mUngroupedChannel.getName().toString())))
                .check(matches(isDisplayed()));
    }

    @Test
    public void launchNotificationSetting_showGroupsWithOneChannel() {
        final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);

        onView(allOf(withText(mGroup2.getName().toString())))
                .check(matches(isDisplayed()));
        onView(allOf(withText(mGroup2Channel1.getName().toString())))
                .check(matches(isDisplayed()));
    }

    private NotificationChannel createChannel(NotificationChannelGroup group,
            String id) {
        NotificationChannel channel = new NotificationChannel(id, id, IMPORTANCE_DEFAULT);
        if (group != null) {
            channel.setGroup(group.getId());
        }
        mNm.createNotificationChannel(channel);
        return channel;
    }
}
