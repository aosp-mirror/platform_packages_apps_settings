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

import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.service.notification.SystemZenRules.PACKAGE_ANDROID;

import static com.google.common.truth.Truth.assertThat;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;
import android.text.Spanned;
import android.text.style.TtsSpan;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModesListItemPreferenceTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private final ZenIconLoader mIconLoader = new ZenIconLoader(
            MoreExecutors.newDirectExecutorService());

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void constructor_setsMode() {
        ZenModesListItemPreference preference = newPreference(TestModeBuilder.EXAMPLE);

        assertThat(preference.getKey()).isEqualTo(TestModeBuilder.EXAMPLE.getId());
        assertThat(preference.getZenMode()).isEqualTo(TestModeBuilder.EXAMPLE);
    }

    @Test
    public void setZenMode_modeEnabled() {
        ZenMode mode = new TestModeBuilder()
                .setName("Enabled mode")
                .setTriggerDescription("When the thrush knocks")
                .setEnabled(true)
                .build();

        ZenModesListItemPreference preference = newPreference(mode);

        assertThat(preference.getTitle()).isEqualTo("Enabled mode");
        assertThat(preference.getSummary()).isEqualTo("When the thrush knocks");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_modeActive() {
        ZenMode mode = new TestModeBuilder()
                .setName("Active mode")
                .setTriggerDescription("When Birnam forest comes to Dunsinane")
                .setEnabled(true)
                .setActive(true)
                .build();

        ZenModesListItemPreference preference = newPreference(mode);

        assertThat(preference.getTitle()).isEqualTo("Active mode");
        assertThat(preference.getSummary()).isEqualTo("ON • When Birnam forest comes to Dunsinane");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_modeDisabledByApp() {
        ZenMode mode = new TestModeBuilder()
                .setName("Mode disabled by app")
                .setTriggerDescription("When the cat's away")
                .setEnabled(false, /* byUser= */ false)
                .build();

        ZenModesListItemPreference preference = newPreference(mode);

        assertThat(preference.getTitle()).isEqualTo("Mode disabled by app");
        assertThat(preference.getSummary()).isEqualTo("Not set");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_modeDisabledByUser() {
        ZenMode mode = new TestModeBuilder()
                .setName("Mode disabled by user")
                .setTriggerDescription("When the Levee Breaks")
                .setEnabled(false, /* byUser= */ true)
                .build();

        ZenModesListItemPreference preference = newPreference(mode);

        assertThat(preference.getTitle()).isEqualTo("Mode disabled by user");
        assertThat(preference.getSummary()).isEqualTo("Disabled");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_scheduleTime_hasCustomTtsInSummary() {
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY };
        scheduleInfo.startHour = 11;
        scheduleInfo.endHour = 15;
        ZenMode mode = new TestModeBuilder()
                .setPackage(PACKAGE_ANDROID)
                .setType(TYPE_SCHEDULE_TIME)
                .setConditionId(ZenModeConfig.toScheduleConditionId(scheduleInfo))
                .setTriggerDescription(
                        SystemZenRules.getTriggerDescriptionForScheduleTime(mContext, scheduleInfo))
                .build();

        ZenModesListItemPreference preference = newPreference(mode);

        assertThat(preference.getSummary()).isInstanceOf(Spanned.class);
        Spanned summary = (Spanned) preference.getSummary();
        TtsSpan[] ttsSpans = summary.getSpans(0, summary.length(), TtsSpan.class);
        assertThat(ttsSpans).hasLength(1);
        assertThat(ttsSpans[0].getType()).isEqualTo(TtsSpan.TYPE_TEXT);
        assertThat(ttsSpans[0].getArgs().getString(TtsSpan.ARG_TEXT)).isEqualTo(
                "Monday to Wednesday, 11:00 AM - 3:00 PM");
    }

    private ZenModesListItemPreference newPreference(ZenMode zenMode) {
        return new ZenModesListItemPreference(mContext, mIconLoader, MoreExecutors.directExecutor(),
                zenMode);
    }
}
