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
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.android.settings.notification.modes.CharSequenceTruth.assertThat;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

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
public class ZenModeTriggerUpdatePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    private ZenModeTriggerUpdatePreferenceController mController;

    private PrimarySwitchPreference mPreference;
    @Mock private ZenModesBackend mBackend;
    @Mock private PackageManager mPm;
    @Mock private ConfigurationActivityHelper mConfigurationActivityHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();

        PreferenceManager preferenceManager = new PreferenceManager(context);
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(context,
                R.xml.modes_rule_settings, null);

        mController = new ZenModeTriggerUpdatePreferenceController(context,
                "zen_automatic_trigger_settings", mBackend, mPm,
                mConfigurationActivityHelper, mock(ZenServiceListing.class));

        mPreference = preferenceScreen.findPreference("zen_automatic_trigger_settings");

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
    public void isAvailable_systemModeNotCustomManual_true() {
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_SCHEDULE_CALENDAR)
                .build();
        mController.setZenMode(mode);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_appProvidedMode_true() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("com.some.package")
                .setType(TYPE_OTHER)
                .build();
        mController.setZenMode(mode);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_customManualMode_false() {
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .build();
        mController.setZenMode(mode);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_manualDND_false() {
        mController.setZenMode(TestModeBuilder.MANUAL_DND_INACTIVE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_switchCheckedIfRuleEnabled() {
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();

        // Update preference controller with a zen mode that is not enabled
        mController.updateZenMode(mPreference, zenMode);
        assertThat(mPreference.getCheckedState()).isFalse();

        // Now with the rule enabled
        zenMode.getRule().setEnabled(true);
        mController.updateZenMode(mPreference, zenMode);
        assertThat(mPreference.getCheckedState()).isTrue();
    }

    @Test
    public void onPreferenceChange_toggleOn_enablesModeAfterConfirmation() {
        // Start with a disabled mode
        ZenMode zenMode = new TestModeBuilder().setName("The mode").setEnabled(false).build();
        mController.updateZenMode(mPreference, zenMode);

        // Flip the switch
        mPreference.callChangeListener(true);
        verify(mBackend, never()).updateMode(any());

        AlertDialog confirmDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(confirmDialog).isNotNull();
        assertThat(confirmDialog.isShowing()).isTrue();
        assertThat(((TextView) confirmDialog.findViewById(com.android.internal.R.id.alertTitle))
                .getText()).isEqualTo("Enable The mode?");

        // Oh wait, I forgot to confirm! Let's do that
        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
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
        ZenMode zenMode = new TestModeBuilder().setName("The mode").setEnabled(true).build();
        mController.updateZenMode(mPreference, zenMode);

        // Flip the switch
        mPreference.callChangeListener(false);
        verify(mBackend, never()).updateMode(any());

        AlertDialog confirmDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(confirmDialog).isNotNull();
        assertThat(confirmDialog.isShowing()).isTrue();
        assertThat(((TextView) confirmDialog.findViewById(com.android.internal.R.id.alertTitle))
                .getText()).isEqualTo("Disable The mode?");

        // Oh wait, I forgot to confirm! Let's do that
        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
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
        mController.updateZenMode(mPreference, zenMode);

        // Flip the switch, then have second thoughts about it
        mPreference.callChangeListener(true);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify nothing changed, and the switch shows the correct (pre-change) value.
        verify(mBackend, never()).updateMode(any());
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isFalse();
    }

    @Test
    public void onPreferenceChange_ifExitingDialog_doesNotUpdateMode() {
        // Start with a disabled mode
        ZenMode zenMode = new TestModeBuilder().setEnabled(false).build();
        mController.updateZenMode(mPreference, zenMode);

        // Flip the switch, but close the dialog without selecting either button.
        mPreference.callChangeListener(true);
        ShadowAlertDialog.getLatestAlertDialog().dismiss();
        shadowOf(Looper.getMainLooper()).idle();

        // Verify nothing changed, and the switch shows the correct (pre-change) value.
        verify(mBackend, never()).updateMode(any());
        assertThat(mPreference.isChecked()).isFalse();
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

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("Calendar events");
        assertThat(mPreference.getSummary()).isEqualTo("My events");
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                mPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
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

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("1:00 AM - 3:00 PM");
        assertThat(mPreference.getSummary()).isEqualTo("Mon - Tue, Thu");
        // Destination as written into the intent by SubSettingLauncher
        assertThat(
                mPreference.getIntent().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeSetScheduleFragment.class.getName());
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

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("App settings");
        assertThat(mPreference.getSummary()).isEqualTo("When The Music's Over");
        assertThat(mPreference.getIntent()).isEqualTo(configurationIntent);
    }

    @Test
    public void updateState_appWithoutConfigActivity_showsWithoutLinkToConfigActivity() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .setTriggerDescription("When the saints go marching in")
                .build();
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(null);

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("App settings");
        assertThat(mPreference.getSummary()).isEqualTo("When the saints go marching in");
        assertThat(mPreference.getIntent()).isNull();
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

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("App settings");
        assertThat(mPreference.getSummary()).isEqualTo("Info and settings in The App Name");
    }

    @Test
    public void updateState_appWithoutTriggerDescriptionNorConfigActivity_showsAppNameInSummary() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("some.package")
                .build();
        when(mConfigurationActivityHelper.getConfigurationActivityIntentForMode(any(), any()))
                .thenReturn(null);
        when(mPm.getText(any(), anyInt(), any())).thenReturn("The App Name");

        mController.updateState(mPreference, mode);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("App settings");
        assertThat(mPreference.getSummary()).isEqualTo("Managed by The App Name");
    }
}
