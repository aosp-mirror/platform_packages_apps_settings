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
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import android.app.Flags;
import android.content.Context;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeTriggerCategoryPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    private ZenModeTriggerCategoryPreferenceController mController;

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

        mController = new ZenModeTriggerCategoryPreferenceController(mContext,
                "zen_automatic_trigger_category");
        mPreference = preferenceScreen.findPreference("zen_automatic_trigger_category");
    }

    @Test
    public void isAvailable_customManualMode_true() {
        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toCustomManualConditionId())
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .setType(TYPE_OTHER)
                .setTriggerDescription("Will not be shown")
                .build();
        mController.setZenMode(mode);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_systemMode_true() {
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
    public void isAvailable_manualDND_false() {
        mController.setZenMode(TestModeBuilder.MANUAL_DND_INACTIVE);
        assertThat(mController.isAvailable()).isFalse();
    }
}
