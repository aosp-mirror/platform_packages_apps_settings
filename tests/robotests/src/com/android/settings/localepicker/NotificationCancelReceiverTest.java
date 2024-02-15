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

import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_NOTIFICATION_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotificationCancelReceiverTest {
    private Context mContext;
    private NotificationCancelReceiver mReceiver;
    @Mock
    private NotificationController mNotificationController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mReceiver = spy(new NotificationCancelReceiver());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        doReturn(mNotificationController).when(mReceiver).getNotificationController(any());
    }

    @Test
    public void testOnReceive_incrementDismissCount() {
        String locale = "en-US";
        int notificationId = 100;
        Intent intent = new Intent()
                .putExtra(EXTRA_APP_LOCALE, locale)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        when(mNotificationController.getNotificationId(locale)).thenReturn(notificationId);

        mReceiver.onReceive(mContext, intent);

        verify(mNotificationController).incrementDismissCount(eq(locale));
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(), eq(SettingsEnums.ACTION_NOTIFICATION_SWIPE_FOR_SYSTEM_LOCALE));
    }
}
