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
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.android.settings.notification.modes.ZenModeSetTriggerLinkPreferenceController.ADD_TRIGGER_KEY;
import static com.android.settings.notification.modes.ZenModeSetTriggerLinkPreferenceController.AUTOMATIC_TRIGGER_KEY;
import static com.android.settings.notification.modes.ZenModeSetTriggerLinkPreferenceControllerTest.CharSequenceTruth.assertThat;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeSetTriggerLinkPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private ZenModesBackend mBackend;
    private Context mContext;

    @Mock
    private PackageManager mPm;
    @Mock
    private ConfigurationActivityHelper mConfigurationActivityHelper;

    private PreferenceCategory mPrefCategory;
    private PrimarySwitchPreference mConfigPreference;
    private Preference mAddPreference;

    @Mock
    private DashboardFragment mFragment;

    private ZenModeSetTriggerLinkPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(mContext,
                R.xml.modes_rule_settings, null);

        mController = new ZenModeSetTriggerLinkPreferenceController(mContext,
                "zen_automatic_trigger_category", mFragment, mBackend, mPm,
                mConfigurationActivityHelper, mock(ZenServiceListing.class));

        mPrefCategory = preferenceScreen.findPreference("zen_automatic_trigger_category");
        mConfigPreference = checkNotNull(mPrefCategory).findPreference(AUTOMATIC_TRIGGER_KEY);
        mAddPreference = checkNotNull(mPrefCategory).findPreference(ADD_TRIGGER_KEY);

        when(mPm.getApplicationInfo(any(), anyInt())).then(
                (Answer<ApplicationInfo>) invocationOnMock -> {
                    ApplicationInfo appInfo = new ApplicationInfo();
                    appInfo.packageName = invocationOnMock.getArgument(0);
                    appInfo.labelRes = 1; // Whatever, but != 0 so that loadLabel calls PM.getText()
                    return appInfo;
                });
        when(mPm.getText(any(), anyInt(), any())).then(
                (Answer<CharSequence>) invocationOnMock ->
                        "App named " + invocationOnMock.getArgument(0));
    }

    @Test
    public void testIsAvailable() {
        // should not be available for manual DND
        ZenMode manualMode = ZenMode.manualDndMode(new AutomaticZenRule.Builder("Do Not Disturb",
                Uri.parse("manual"))
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .build(), true);

        mController.updateZenMode(mPrefCategory, manualMode);
        assertThat(mController.isAvailable()).isFalse();

        // should be available for other modes
        mController.updateZenMode(mPrefCategory, TestModeBuilder.EXAMPLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_switchCheckedIfRuleEnabled() {
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();

        // Update preference controller with a zen mode that is not enabled
        mController.updateZenMode(mPrefCategory, zenMode);
        assertThat(mConfigPreference.getCheckedState()).isFalse();

        // Now with the rule enabled
        zenMode.getRule().setEnabled(true);
        mController.updateZenMode(mPrefCategory, zenMode);
        assertThat(mConfigPreference.getCheckedState()).isTrue();
    }

    @Test
    public void onPreferenceChange_toggleOn_enablesModeAfterConfirmation() {
        // Start with a disabled mode
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();
        mController.updateZenMode(mPrefCategory, zenMode);

        // Flip the switch
        mConfigPreference.callChangeListener(true);
        verify(mBackend, never()).updateMode(any());

        // Oh wait, I forgot to confirm! Let's do that
        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNotNull();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isTrue();
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify the backend got asked to update the mode to be enabled
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().isEnabled()).isTrue();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isFalse();
    }

    @Test
    public void onPreferenceChange_toggleOff_disablesModeAfterConfirmation() {
        // Start with an enabled mode
        ZenMode zenMode = new TestModeBuilder().setEnabled(true).build();
        mController.updateZenMode(mPrefCategory, zenMode);

        // Flip the switch
        mConfigPreference.callChangeListener(false);
        verify(mBackend, never()).updateMode(any());

        // Oh wait, I forgot to confirm! Let's do that
        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNotNull();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isTrue();
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify the backend got asked to update the mode to be disabled
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().isEnabled()).isFalse();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isFalse();
    }

    @Test
    public void onPreferenceChange_ifPressCancelButton_doesNotUpdateMode() {
        // Start with a disabled mode
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();
        mController.updateZenMode(mPrefCategory, zenMode);

        // Flip the switch, then have second thoughts about it
        mConfigPreference.callChangeListener(true);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify nothing changed, and the switch shows the correct (pre-change) value.
        verify(mBackend, never()).updateMode(any());
        assertThat(mConfigPreference.isChecked()).isFalse();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isFalse();
    }

    @Test
    public void onPreferenceChange_ifExitingDialog_doesNotUpdateMode() {
        // Start with a disabled mode
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();
        mController.updateZenMode(mPrefCategory, zenMode);

        // Flip the switch, but close the dialog without selecting either button.
        mConfigPreference.callChangeListener(true);
        ShadowAlertDialog.getLatestAlertDialog().dismiss();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify nothing changed, and the switch shows the correct (pre-change) value.
        verify(mBackend, never()).updateMode(any());
        assertThat(mConfigPreference.isChecked()).isFalse();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isFalse();
    }

    @Test
    public void updateState_scheduleCalendarRule() {
        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendarId = 1L;
        eventInfo.calName = "My events";
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setConditionId(ZenModeConfig.toEventConditionId(eventInfo))
                .setType(TYPE_SCHEDULE_CALENDAR)
                .setTriggerDescription("My events")
                .build();

        mController.updateState(mPrefCategory, mode);

        assertThat(mAddPreference.isVisible()).isFalse();
        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("Calendar events");
        assertThat(mConfigPreference.getSummary()).isEqualTo("My events");
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                mConfigPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeSetCalendarFragment.class.getName());
    }

    @Test
    public void updateState_scheduleTimeRule() {
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

        mController.updateState(mPrefCategory, mode);

        assertThat(mAddPreference.isVisible()).isFalse();
        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("1:00 AM - 3:00 PM");
        assertThat(mConfigPreference.getSummary()).isEqualTo("Mon - Tue, Thu");
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                mConfigPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeSetScheduleFragment.class.getName());
    }

    @Test
    public void updateState_customManualRule() {
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("Will not be shown")
                .build();

        mController.updateState(mPrefCategory, mode);

        assertThat(mConfigPreference.isVisible()).isFalse();
        assertThat(mAddPreference.isVisible()).isTrue();
        assertThat(mAddPreference.getTitle()).isEqualTo(
                mContext.getString(R.string.zen_mode_select_schedule));
        assertThat(mAddPreference.getSummary()).isNull();
        // Sets up a click listener to open the dialog.
        assertThat(mAddPreference.getOnPreferenceClickListener()).isNotNull();
    }

    @Test
    public void updateState_appWithConfigActivity_showsLinkToConfigActivity() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .setTriggerDescription("When The Music's Over")
                .build();
        Intent configurationIntent = new Intent("configure the mode");
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(configurationIntent);

        mController.updateState(mPrefCategory, mode);

        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("Linked to app");
        assertThat(mConfigPreference.getSummary()).isEqualTo("When The Music's Over");
        assertThat(mConfigPreference.getIntent()).isEqualTo(configurationIntent);
    }

    @Test
    public void updateState_appWithoutConfigActivity_showsWithoutLinkToConfigActivity() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .setTriggerDescription("When the saints go marching in")
                .build();
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(null);

        mController.updateState(mPrefCategory, mode);

        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("Linked to app");
        assertThat(mConfigPreference.getSummary()).isEqualTo("When the saints go marching in");
        assertThat(mConfigPreference.getIntent()).isNull();
    }

    @Test
    public void updateState_appWithoutTriggerDescriptionWithConfigActivity_showsAppNameInSummary() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .build();
        Intent configurationIntent = new Intent("configure the mode");
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(configurationIntent);
        when(mPm.getText(any(), anyInt(), any())).thenReturn("The App Name");

        mController.updateState(mPrefCategory, mode);

        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("Linked to app");
        assertThat(mConfigPreference.getSummary()).isEqualTo("Info and settings in The App Name");
    }

    @Test
    public void updateState_appWithoutTriggerDescriptionNorConfigActivity_showsAppNameInSummary() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .build();
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(null);
        when(mPm.getText(any(), anyInt(), any())).thenReturn("The App Name");

        mController.updateState(mPrefCategory, mode);

        assertThat(mConfigPreference.isVisible()).isTrue();
        assertThat(mConfigPreference.getTitle()).isEqualTo("Linked to app");
        assertThat(mConfigPreference.getSummary()).isEqualTo("Managed by The App Name");
    }

    @Test
    public void onScheduleChosen_updatesMode() {
        ZenMode originalMode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("")
                .build();
        mController.updateZenMode(mPrefCategory, originalMode);

        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 12;
        scheduleInfo.endHour = 15;
        Uri scheduleUri = ZenModeConfig.toScheduleConditionId(scheduleInfo);

        mController.mOnScheduleOptionListener.onScheduleSelected(scheduleUri);

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

    static class CharSequenceTruth {
        /**
         * Shortcut version of {@link Truth#assertThat(String)} suitable for {@link CharSequence}.
         * {@link CharSequence} doesn't necessarily provide a good {@code equals()} implementation;
         * however we don't care about formatting here, so we want to assert on the resulting
         * string (without needing to worry that {@code assertThat(x.getText().toString())} can
         * throw if the text is null).
         */
        static StringSubject assertThat(@Nullable CharSequence actual) {
            return Truth.assertThat((String) (actual != null ? actual.toString() : null));
        }
    }
}
