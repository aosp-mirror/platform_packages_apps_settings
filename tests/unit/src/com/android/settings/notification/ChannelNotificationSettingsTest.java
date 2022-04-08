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

import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

import android.app.INotificationManager;
import android.app.Instrumentation;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChannelNotificationSettingsTest {
    private static final String WM_DISMISS_KEYGUARD_COMMAND = "wm dismiss-keyguard";

    private UiDevice mUiDevice;
    private Context mTargetContext;
    private Instrumentation mInstrumentation;
    private NotificationChannel mNotificationChannel;
    private NotificationManager mNm;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();

        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mUiDevice.wakeUp();
        mUiDevice.executeShellCommand(WM_DISMISS_KEYGUARD_COMMAND);

        mNm  = (NotificationManager) mTargetContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationChannel = new NotificationChannel(this.getClass().getName(),
                this.getClass().getName(), IMPORTANCE_MIN);
        mNm.createNotificationChannel(mNotificationChannel);
    }

    @Test
    public void launchNotificationSetting_shouldNotCrash() {
        final Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, mNotificationChannel.getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);

        onView(allOf(withText(mNotificationChannel.getName().toString()))).check(
                matches(isDisplayed()));
    }

    @Test
    public void launchNotificationSettings_blockedChannel() throws Exception {
        NotificationChannel blocked =
                new NotificationChannel("blocked", "blocked", IMPORTANCE_NONE);
        mNm.createNotificationChannel(blocked);

        INotificationManager sINM = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        blocked.setImportance(IMPORTANCE_NONE);
        sINM.updateNotificationChannelForPackage(
                mTargetContext.getPackageName(), Process.myUid(), blocked);

        final Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, mTargetContext.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, blocked.getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);

        onView(allOf(withText("At your request, Android is blocking this category of notifications"
                + " from appearing on this device"))).check(matches(isDisplayed()));

        try {
            onView(allOf(withText("On the lock screen"))).check(matches(isDisplayed()));
            fail("settings appearing for blocked channel");
        } catch (Exception e) {
            // expected
        }
    }
}
