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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS;
import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import static com.google.common.truth.Truth.assertThat;

import android.app.AutomaticZenRule;
import android.net.Uri;
import android.service.notification.ZenPolicy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ZenModeTest {

    private static final ZenPolicy ZEN_POLICY = new ZenPolicy.Builder().allowAllSounds().build();

    private static final AutomaticZenRule ZEN_RULE =
            new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                    .setType(AutomaticZenRule.TYPE_DRIVING)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(ZEN_POLICY)
                    .build();

    @Test
    public void testBasicMethods() {
        ZenMode zenMode = new ZenMode("id", ZEN_RULE);

        assertThat(zenMode.getId()).isEqualTo("id");
        assertThat(zenMode.getRule()).isEqualTo(ZEN_RULE);
        assertThat(zenMode.isManualDnd()).isFalse();
        assertThat(zenMode.canBeDeleted()).isTrue();
    }

    @Test
    public void getZenPolicy_interruptionFilterPriority_returnsZenPolicy() {
        ZenMode zenMode = new ZenMode("id", new AutomaticZenRule.Builder("Rule", Uri.EMPTY)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(ZEN_POLICY)
                .build());

        assertThat(zenMode.getPolicy()).isEqualTo(ZEN_POLICY);
    }

    @Test
    public void getZenPolicy_interruptionFilterAll_returnsPolicyAllowingAll() {
        ZenMode zenMode = new ZenMode("id", new AutomaticZenRule.Builder("Rule", Uri.EMPTY)
                .setInterruptionFilter(INTERRUPTION_FILTER_ALL)
                .setZenPolicy(ZEN_POLICY) // should be ignored
                .build());

        assertThat(zenMode.getPolicy()).isEqualTo(
                new ZenPolicy.Builder().allowAllSounds().showAllVisualEffects().build());
    }

    @Test
    public void getZenPolicy_interruptionFilterAlarms_returnsPolicyAllowingAlarms() {
        ZenMode zenMode = new ZenMode("id", new AutomaticZenRule.Builder("Rule", Uri.EMPTY)
                .setInterruptionFilter(INTERRUPTION_FILTER_ALARMS)
                .setZenPolicy(ZEN_POLICY) // should be ignored
                .build());

        assertThat(zenMode.getPolicy()).isEqualTo(
                new ZenPolicy.Builder()
                        .disallowAllSounds()
                        .allowAlarms(true)
                        .allowMedia(true)
                        .allowPriorityChannels(false)
                        .build());
    }

    @Test
    public void getZenPolicy_interruptionFilterNone_returnsPolicyAllowingNothing() {
        ZenMode zenMode = new ZenMode("id", new AutomaticZenRule.Builder("Rule", Uri.EMPTY)
                .setInterruptionFilter(INTERRUPTION_FILTER_NONE)
                .setZenPolicy(ZEN_POLICY) // should be ignored
                .build());

        assertThat(zenMode.getPolicy()).isEqualTo(
                new ZenPolicy.Builder()
                        .disallowAllSounds()
                        .hideAllVisualEffects()
                        .allowPriorityChannels(false)
                        .build());
    }
}
