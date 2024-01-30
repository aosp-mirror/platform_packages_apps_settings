/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.localepicker;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.LocaleList;
import android.os.SystemClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Calendar;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class NotificationControllerTest {
    private Context mContext;
    private LocaleNotificationDataManager mDataManager;
    private NotificationController mNotificationController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mNotificationController = NotificationController.getInstance(mContext);
        mDataManager = mNotificationController.getDataManager();
        LocaleList.setDefault(LocaleList.forLanguageTags("en-CA"));
    }

    @After
    public void tearDown() {
        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    public void incrementDismissCount_addOne() throws Exception {
        String enUS = "en-US";
        Set<Integer> uidSet = Set.of(100, 101);
        long lastNotificationTime = Calendar.getInstance().getTimeInMillis();
        int id = (int) SystemClock.uptimeMillis();
        initSharedPreference(enUS, uidSet, 0, 1, lastNotificationTime, id);

        mNotificationController.incrementDismissCount(enUS);
        NotificationInfo result = mDataManager.getNotificationInfo(enUS);

        assertThat(result.getDismissCount()).isEqualTo(1); // dismissCount increments
        assertThat(result.getUidCollection()).isEqualTo(uidSet);
        assertThat(result.getNotificationCount()).isEqualTo(1);
        assertThat(result.getLastNotificationTimeMs()).isEqualTo(lastNotificationTime);
        assertThat(result.getNotificationId()).isEqualTo(id);
    }

    @Test
    public void testShouldTriggerNotification_inSystemLocale_returnFalse() throws Exception {
        int uid = 102;
        // As checking whether app's locales exist in system locales, both app locales and system
        // locales have to remove the u extension first when doing the comparison. The following
        // three locales are all in the system locale after removing the u extension so it's
        // unnecessary to trigger a notification for the suggestion.
        String locale1 = "en-CA";
        String locale2 = "ar-JO-u-nu-latn";
        String locale3 = "ar-JO";

        LocaleList.setDefault(
                LocaleList.forLanguageTags("en-CA-u-mu-fahrenhe,ar-JO-u-mu-fahrenhe-nu-latn"));

        assertThat(mNotificationController.shouldTriggerNotification(uid, locale1)).isFalse();
        assertThat(mNotificationController.shouldTriggerNotification(uid, locale2)).isFalse();
        assertThat(mNotificationController.shouldTriggerNotification(uid, locale3)).isFalse();
    }

    @Test
    public void testShouldTriggerNotification_noNotification_returnFalse() throws Exception {
        int uid = 100;
        String locale = "en-US";

        boolean triggered = mNotificationController.shouldTriggerNotification(uid, locale);

        assertThat(triggered).isFalse();
    }

    @Test
    public void testShouldTriggerNotification_return1stTrue() throws Exception {
        // Initialze proto with en-US locale. Its uid contains 100.
        Set<Integer> uidSet = Set.of(100);
        String locale = "en-US";
        long lastNotificationTime = 0L;
        int notificationId = 0;
        initSharedPreference(locale, uidSet, 0, 1, lastNotificationTime, notificationId);

        // When the second app is configured to "en-US", the notification is triggered.
        int uid = 101;
        boolean triggered = mNotificationController.shouldTriggerNotification(uid, locale);

        assertThat(triggered).isTrue();
    }

    @Test
    public void testShouldTriggerNotification_returnFalse_dueToOddCount() throws Exception {
        // Initialze proto with en-US locale. Its uid contains 100,101.
        Set<Integer> uidSet = Set.of(100, 101);
        String locale = "en-US";
        long lastNotificationTime = Calendar.getInstance().getTimeInMillis();
        int id = (int) SystemClock.uptimeMillis();
        initSharedPreference(locale, uidSet, 0, 1, lastNotificationTime, id);

        // When the other app is configured to "en-US", the notification is not triggered because
        // the app count is odd.
        int uid = 102;
        boolean triggered = mNotificationController.shouldTriggerNotification(uid, locale);

        assertThat(triggered).isFalse();
    }

    @Test
    public void testShouldTriggerNotification_returnFalse_dueToFrequency() throws Exception {
        // Initialze proto with en-US locale. Its uid contains 100,101,102.
        Set<Integer> uidSet = Set.of(100, 101, 102);
        String locale = "en-US";
        long lastNotificationTime = Calendar.getInstance().getTimeInMillis();
        int id = (int) SystemClock.uptimeMillis();
        initSharedPreference(locale, uidSet, 0, 1, lastNotificationTime, id);

        // When the other app is configured to "en-US", the notification is not triggered because it
        // is too frequent.
        int uid = 103;
        boolean triggered = mNotificationController.shouldTriggerNotification(uid, locale);

        assertThat(triggered).isFalse();
    }

    @Test
    public void testShouldTriggerNotification_return2ndTrue() throws Exception {
        // Initialze proto with en-US locale. Its uid contains 100,101,102,103,104.
        Set<Integer> uidSet = Set.of(100, 101, 102, 103, 104);
        String locale = "en-US";
        int id = (int) SystemClock.uptimeMillis();
        Calendar time = Calendar.getInstance();
        time.add(Calendar.MINUTE, 86400 * 8 * (-1));
        long lastNotificationTime = time.getTimeInMillis();
        initSharedPreference(locale, uidSet, 0, 1, lastNotificationTime, id);

        // When the other app is configured to "en-US", the notification is triggered.
        int uid = 105;
        boolean triggered = mNotificationController.shouldTriggerNotification(uid, locale);

        assertThat(triggered).isTrue();
    }

    private void initSharedPreference(String locale, Set<Integer> uidCollection, int dismissCount,
            int notificationCount, long lastNotificationTime, int notificationId)
            throws Exception {
        NotificationInfo info = new NotificationInfo(uidCollection, notificationCount, dismissCount,
                lastNotificationTime, notificationId);
        mDataManager.putNotificationInfo(locale, info);
    }
}
