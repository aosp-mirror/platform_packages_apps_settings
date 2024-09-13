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

import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
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
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeTriggerAddPreferenceControllerTest {

    private static final ZenMode CUSTOM_MANUAL_MODE = new TestModeBuilder()
            .setConditionId(ZenModeConfig.toCustomManualConditionId())
            .setPackage(SystemZenRules.PACKAGE_ANDROID)
            .setType(TYPE_OTHER)
            .setTriggerDescription("Will not be shown")
            .build();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    private ZenModeTriggerAddPreferenceController mController;

    private Context mContext;
    private Preference mPreference;
    @Mock private ZenModesBackend mBackend;
    @Mock private DashboardFragment mFragment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(mContext,
                R.xml.modes_rule_settings, null);

        mController = new ZenModeTriggerAddPreferenceController(mContext,
                "zen_add_automatic_trigger", mFragment, mBackend);
        mPreference = preferenceScreen.findPreference("zen_add_automatic_trigger");
    }

    @Test
    public void isAvailable_customManualMode_true() {
        mController.setZenMode(CUSTOM_MANUAL_MODE);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_systemMode_false() {
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_SCHEDULE_CALENDAR)
                .build();
        mController.setZenMode(mode);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_appProvidedMode_false() {
        ZenMode mode = new TestModeBuilder()
                .setPackage("com.some.package")
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
    public void updateState_customManualRule() {
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("Will not be shown")
                .build();

        mController.updateState(mPreference, mode);

        assertThat(mPreference.getTitle()).isEqualTo(
                mContext.getString(R.string.zen_mode_select_schedule));
        assertThat(mPreference.getSummary()).isNull();
        // Sets up a click listener to open the dialog.
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
        mController.updateZenMode(mPreference, originalMode);

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
}
