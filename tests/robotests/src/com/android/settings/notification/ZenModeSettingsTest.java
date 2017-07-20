/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.NotificationManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertTrue;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ZenModeSettingsTest {

    private ZenModeSettings.SummaryBuilder mBuilder;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mBuilder = new ZenModeSettings.SummaryBuilder(mContext);
    }

    @Test
    public void testAppend_conditionFalse_shouldNotAppend() {
        String original = "test";

        final String result = mBuilder.append(original, false, R.string.zen_mode_alarms);

        assertThat(result).isEqualTo(original);
    }

    @Test
    public void testAppend_conditionTrue_shouldAppend() {
        String original = "test";
        String alarm = mContext.getString(R.string.zen_mode_alarms);

        final String result = mBuilder.append(original, true, R.string.zen_mode_alarms);

        assertThat(result).contains(alarm);
        assertThat(result).contains(original);
        assertTrue(result.indexOf(original) < result.indexOf(alarm));
    }

    @Test
    public void testPrepend() {
        String original = mContext.getString(R.string.zen_mode_alarms);
        String reminders = mContext.getString(R.string.zen_mode_reminders);

        final String result = mBuilder.prepend(original, true, R.string.zen_mode_reminders);
        assertThat(result).contains(original);
        assertThat(result).contains(reminders);
        assertTrue(result.indexOf(reminders) < result.indexOf(original));
    }

    @Test
    public void testGetPrioritySettingSummary_sameOrderAsTargetPage() {
        NotificationManager.Policy policy = new NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS
                        | NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS,
                0, 0);
        final String result = mBuilder.getPrioritySettingSummary(policy);

        String alarms = mContext.getString(R.string.zen_mode_alarms);
        String reminders = mContext.getString(R.string.zen_mode_reminders);
        String events = mContext.getString(R.string.zen_mode_events);

        assertThat(result).contains(alarms);
        assertThat(result).contains(reminders);
        assertThat(result).contains(events);
        assertTrue(result.indexOf(reminders) < result.indexOf(events) &&
                result.indexOf(events) < result.indexOf(alarms));
    }

}
