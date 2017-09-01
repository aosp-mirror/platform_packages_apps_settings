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

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

import android.app.INotificationManager;
import android.app.Instrumentation;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChannelGroupNotificationSettingsTest {

    private Context mTargetContext;
    private Instrumentation mInstrumentation;
    private NotificationManager mNm;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();
        mNm  = (NotificationManager) mTargetContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Test
    public void launchNotificationSetting_displaysChannels() {
        NotificationChannelGroup group =
                new NotificationChannelGroup(this.getClass().getName(), this.getClass().getName());
        group.setDescription("description");
        NotificationChannel channel = new NotificationChannel(this.getClass().getName(),
                "channel" + this.getClass().getName(), IMPORTANCE_MIN);
        channel.setGroup(this.getClass().getName());
        NotificationChannel channel2 = new NotificationChannel("2"+this.getClass().getName(),
                "2channel" + this.getClass().getName(), IMPORTANCE_MIN);
        channel2.setGroup(this.getClass().getName());

        mNm.createNotificationChannelGroup(group);
        mNm.createNotificationChannel(channel);
        mNm.createNotificationChannel(channel2);

        final Intent intent = new Intent(Settings.ACTION_CHANNEL_GROUP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_GROUP_ID, group.getId());

        mInstrumentation.startActivitySync(intent);

        onView(allOf(withText(group.getName().toString()))).check(matches(isDisplayed()));
        onView(allOf(withText(channel.getName().toString()))).check(
                matches(isDisplayed()));
        onView(allOf(withText(group.getDescription().toString()))).check(
                matches(isDisplayed()));
        onView(allOf(withText(channel2.getName().toString()))).check(
                matches(isDisplayed()));
        try {
            onView(allOf(withText("Android is blocking this group of notifications from"
                    + " appearing on this device"))).check(matches(isDisplayed()));
            fail("Blocking footer erroneously appearing");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void launchNotificationSettings_blockedGroup() throws Exception {
        NotificationChannelGroup blocked =
                new NotificationChannelGroup("blocked", "blocked");
        NotificationChannel channel =
                new NotificationChannel("channel", "channel", IMPORTANCE_HIGH);
        channel.setGroup(blocked.getId());
        mNm.createNotificationChannelGroup(blocked);
        mNm.createNotificationChannel(channel);

        INotificationManager sINM = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        blocked.setBlocked(true);
        sINM.updateNotificationChannelGroupForPackage(
                mTargetContext.getPackageName(), Process.myUid(), blocked);

        final Intent intent = new Intent(Settings.ACTION_CHANNEL_GROUP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_GROUP_ID, blocked.getId());
        mInstrumentation.startActivitySync(intent);

        onView(allOf(withText("Off"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText("Android is blocking this group of notifications from"
                + " appearing on this device"))).check(matches(isDisplayed()));

        try {
            onView(allOf(withText(channel.getName().toString()))).check(matches(isDisplayed()));
            fail("settings appearing for blocked group");
        } catch (Exception e) {
            // expected
        }
    }
}
