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

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class NotificationActionActivityTest {
    private NotificationActionActivity mNotificationActivity;
    private ActivityController<NotificationActionActivity> mActivityController;
    @Mock
    private NotificationController mNotificationController;
    @Mock
    private ActivityResultLauncher<Intent> mLauncher;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnCreate_launchSystemLanguageSettings() throws Exception {
        String targetLocale = "ja-JP";
        int notificationId = 123;
        Intent intent = new Intent()
                .putExtra(EXTRA_APP_LOCALE, targetLocale)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        mActivityController = Robolectric.buildActivity(NotificationActionActivity.class, intent);
        mNotificationActivity = spy(mActivityController.get());
        doReturn(mNotificationController).when(mNotificationActivity).getNotificationController(
                any());
        doReturn(notificationId).when(mNotificationController).getNotificationId(eq(targetLocale));
        doReturn(mLauncher).when(mNotificationActivity).getLauncher();

        mNotificationActivity.onCreate(null);

        verify(mLauncher).launch(any(Intent.class));
        verify(mNotificationActivity).finish();
    }
}
