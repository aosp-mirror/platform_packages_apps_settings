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

import static android.app.AutomaticZenRule.TYPE_OTHER;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_CALENDAR;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.app.NotificationManager.EXTRA_AUTOMATIC_RULE_ID;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;
import static android.service.notification.ConditionProviderService.EXTRA_RULE_ID;

import static com.android.settings.notification.modes.ZenModeSetTriggerLinkPreferenceController.AUTOMATIC_TRIGGER_PREF_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ConditionProviderService;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;

import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

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
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeSetTriggerLinkPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private ZenModesBackend mBackend;
    private Context mContext;

    private PrimarySwitchPreference mPreference;

    @Mock
    private ZenServiceListing mServiceListing;
    @Mock
    private PackageManager mPm;

    @Mock
    private PreferenceCategory mPrefCategory;
    @Mock
    private DashboardFragment mFragment;

    private ZenModeSetTriggerLinkPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mPrefController = new ZenModeSetTriggerLinkPreferenceController(mContext,
                "zen_automatic_trigger_category", mFragment, mBackend, mPm);
        mPrefController.setServiceListing(mServiceListing);
        mPreference = new PrimarySwitchPreference(mContext);

        when(mPrefCategory.findPreference(AUTOMATIC_TRIGGER_PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void testIsAvailable() {
        // should not be available for manual DND
        ZenMode manualMode = ZenMode.manualDndMode(new AutomaticZenRule.Builder("Do Not Disturb",
                Uri.parse("manual"))
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .build(), true);

        mPrefController.updateZenMode(mPrefCategory, manualMode);
        assertThat(mPrefController.isAvailable()).isFalse();

        // should be available for other modes
        mPrefController.updateZenMode(mPrefCategory, TestModeBuilder.EXAMPLE);
        assertThat(mPrefController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateState() {
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();

        // Update preference controller with a zen mode that is not enabled
        mPrefController.updateZenMode(mPrefCategory, zenMode);
        assertThat(mPreference.getCheckedState()).isFalse();

        // Now with the rule enabled
        zenMode.getRule().setEnabled(true);
        mPrefController.updateZenMode(mPrefCategory, zenMode);
        assertThat(mPreference.getCheckedState()).isTrue();
    }

    @Test
    public void testOnPreferenceChange() {
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();

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
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setConditionId(ZenModeConfig.toEventConditionId(eventInfo))
                .setType(TYPE_SCHEDULE_CALENDAR)
                .setTriggerDescription("My events")
                .build();
        mPrefController.updateZenMode(mPrefCategory, mode);

        assertThat(mPreference.getTitle()).isNotNull();
        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.zen_mode_set_calendar_link));
        assertThat(mPreference.getSummary()).isNotNull();
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mode.getRule().getTriggerDescription());
        assertThat(mPreference.getIcon()).isNull();

        // Destination as written into the intent by SubSettingLauncher
        assertThat(mPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeSetCalendarFragment.class.getName());
    }

    @Test
    public void testRuleLink_schedule() {
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.THURSDAY};
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 15;
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toScheduleConditionId(scheduleInfo))
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_SCHEDULE_TIME)
                .setTriggerDescription("some schedule")
                .build();
        mPrefController.updateZenMode(mPrefCategory, mode);

        assertThat(mPreference.getTitle()).isNotNull();
        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.zen_mode_set_schedule_link));
        assertThat(mPreference.getSummary()).isNotNull();
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mode.getRule().getTriggerDescription());
        assertThat(mPreference.getIcon()).isNull();

        // Destination as written into the intent by SubSettingLauncher
        assertThat(mPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeSetScheduleFragment.class.getName());
    }

    @Test
    public void testRuleLink_manual() {
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("Will not be shown")
                .build();
        mPrefController.updateZenMode(mPrefCategory, mode);

        assertThat(mPreference.getTitle()).isNotNull();
        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.zen_mode_select_schedule));
        assertThat(mPreference.getIcon()).isNotNull();
        assertThat(mPreference.getSummary()).isNotNull();
        assertThat(mPreference.getSummary().toString()).isEqualTo("");

        // Set up a click listener to open the dialog.
        assertThat(mPreference.getOnPreferenceClickListener()).isNotNull();
    }

    @Test
    public void onScheduleChosen_updatesMode() {
        ZenMode originalMode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("")
                .build();
        mPrefController.updateZenMode(mPrefCategory, originalMode);

        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 12;
        scheduleInfo.endHour = 15;
        Uri scheduleUri = ZenModeConfig.toScheduleConditionId(scheduleInfo);

        mPrefController.mOnScheduleOptionListener.onScheduleSelected(scheduleUri);

        // verify the backend got asked to update the mode to be schedule-based.
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        ZenMode updatedMode = captor.getValue();
        assertThat(updatedMode.getType()).isEqualTo(TYPE_SCHEDULE_TIME);
        assertThat(updatedMode.getRule().getConditionId()).isEqualTo(scheduleUri);
        assertThat(updatedMode.getRule().getTriggerDescription()).isNotEmpty();
        assertThat(updatedMode.getRule().getOwner()).isEqualTo(
                ZenModeConfig.getScheduleConditionProvider());
    }

    @Test
    public void testGetAppRuleIntent_configActivity() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(mContext.getPackageName())
                .setConfigurationActivity(new ComponentName(mContext.getPackageName(), "test"))
                .setType(TYPE_OTHER)
                .setTriggerDescription("some rule")
                .build();

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mPrefController.getAppRuleIntent(mode);
        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(
                new ComponentName(mContext.getPackageName(), "test"));
    }

    @Test
    public void testGetAppRuleIntent_configActivity_wrongPackage() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setPackage(mContext.getPackageName())
                .setConfigurationActivity(new ComponentName("another", "test"))
                .setType(TYPE_OTHER)
                .build();

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mPrefController.getAppRuleIntent(mode);
        assertThat(res).isNull();
    }

    @Test
    public void testGetAppRuleIntent_configActivity_unspecifiedOwner() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(null)
                .setConfigurationActivity(new ComponentName("another", "test"))
                .setType(TYPE_OTHER)
                .build();

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mPrefController.getAppRuleIntent(mode);
        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(new ComponentName("another", "test"));
    }

    @Test
    public void testGetAppRuleIntent_cps() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(mContext.getPackageName())
                .setOwner(new ComponentName(mContext.getPackageName(), "service"))
                .build();

        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));

        when(mServiceListing.findService(new ComponentName(mContext.getPackageName(), "service")))
                .thenReturn(ci);
        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mPrefController.getAppRuleIntent(mode);
        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(
                new ComponentName(mContext.getPackageName(), "activity"));
    }

    @Test
    public void testGetAppRuleIntent_cps_wrongPackage() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setPackage("other")
                .setOwner(new ComponentName(mContext.getPackageName(), "service"))
                .setType(TYPE_OTHER)
                .build();

        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mPrefController.getAppRuleIntent(mode);
        assertThat(res).isNull();
    }
}
