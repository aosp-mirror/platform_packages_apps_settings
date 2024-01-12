/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.notification.BadgingNotificationPreferenceController.OFF;
import static com.android.settings.notification.BadgingNotificationPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowActivityManager.class,
})
public class BubbleSummaryNotificationPreferenceControllerTest {

    private static final String KEY_NOTIFICATION_BUBBLES = "notification_bubbles";
    private Context mContext;

    private BubbleSummaryNotificationPreferenceController mController;
    private Preference mPreference;

    private ShadowActivityManager mActivityManager;


    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new BubbleSummaryNotificationPreferenceController(mContext,
                KEY_NOTIFICATION_BUBBLES);
        mPreference = new Preference(mContext);
        mActivityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
    }

    @Test
    public void getSummary_NOTIFICATION_BUBBLESIsOff_returnOffString() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, OFF);

        assertThat(mController.getSummary()).isEqualTo("Off");
    }

    @Test
    public void getSummary_NOTIFICATION_BUBBLESIsOff_returnOnString() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, ON);

        String onString = mContext.getString(R.string.notifications_bubble_setting_on_summary);
        assertThat(mController.getSummary()).isEqualTo(onString);
    }

    @Test
    public void isAvailable_lowRam_returnsUnsupported() {
        mActivityManager.setIsLowRamDevice(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isAvailable_notLowRam_returnsAvailable() {
        mActivityManager.setIsLowRamDevice(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
