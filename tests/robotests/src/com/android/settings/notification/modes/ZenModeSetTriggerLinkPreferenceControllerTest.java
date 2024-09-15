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

import static android.app.AutomaticZenRule.TYPE_SCHEDULE_CALENDAR;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.android.settings.notification.modes.ZenModeSetTriggerLinkPreferenceController.AUTOMATIC_TRIGGER_PREF_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;

import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
public class ZenModeSetTriggerLinkPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private ZenModesBackend mBackend;
    private Context mContext;

    @Mock
    private PreferenceCategory mPrefCategory;
    @Mock
    private PrimarySwitchPreference mPreference;
    private ZenModeSetTriggerLinkPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mPrefController = new ZenModeSetTriggerLinkPreferenceController(mContext,
                "zen_automatic_trigger_category", mBackend);
        when(mPrefCategory.findPreference(AUTOMATIC_TRIGGER_PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testIsAvailable() {
        // should not be available for manual DND
        ZenMode manualMode = ZenMode.manualDndMode(new AutomaticZenRule.Builder("Do Not Disturb",
                        Uri.parse("manual"))
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .build(), true);

        mPrefController.updateZenMode(mPrefCategory, manualMode);
        assertThat(mPrefController.isAvailable()).isFalse();

        // should be available for other modes
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                        .setEnabled(false)
                        .build(), false);
        mPrefController.updateZenMode(mPrefCategory, zenMode);
        assertThat(mPrefController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateState() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                        .setEnabled(false)
                        .build(), false);

        // Update preference controller with a zen mode that is not enabled
        mPrefController.updateZenMode(mPrefCategory, zenMode);
        verify(mPreference).setChecked(false);

        // Now with the rule enabled
        zenMode.getRule().setEnabled(true);
        mPrefController.updateZenMode(mPrefCategory, zenMode);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                        .setEnabled(false)
                        .build(), false);

        // start with disabled rule
        mPrefController.updateZenMode(mPrefCategory, zenMode);

        // then update the preference to be checked
        mPrefController.mSwitchChangeListener.onPreferenceChange(mPreference, true);

        // verify the backend got asked to update the mode to be enabled
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().isEnabled()).isTrue();
    }

    @Test
    public void testRuleLink_calendar() {
        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendarId = 1L;
        eventInfo.calName = "My events";
        ZenMode mode = new ZenMode("id", new AutomaticZenRule.Builder("name",
                ZenModeConfig.toEventConditionId(eventInfo))
                .setType(TYPE_SCHEDULE_CALENDAR)
                .setTriggerDescription("My events")
                .build(),
                true);  // is active
        mPrefController.updateZenMode(mPrefCategory, mode);

        verify(mPreference).setTitle(R.string.zen_mode_set_calendar_link);
        verify(mPreference).setSummary(mode.getRule().getTriggerDescription());

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mPreference).setIntent(captor.capture());
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                captor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ZenModeSetCalendarFragment.class.getName());
    }

    @Test
    public void testRuleLink_schedule() {
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY, Calendar.TUESDAY, Calendar.THURSDAY };
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 15;
        ZenMode mode = new ZenMode("id", new AutomaticZenRule.Builder("name",
                ZenModeConfig.toScheduleConditionId(scheduleInfo))
                .setType(TYPE_SCHEDULE_TIME)
                .setTriggerDescription("some schedule")
                .build(),
                true);  // is active
        mPrefController.updateZenMode(mPrefCategory, mode);

        verify(mPreference).setTitle(R.string.zen_mode_set_schedule_link);
        verify(mPreference).setSummary(mode.getRule().getTriggerDescription());

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mPreference).setIntent(captor.capture());
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                captor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ZenModeSetScheduleFragment.class.getName());
    }
}
