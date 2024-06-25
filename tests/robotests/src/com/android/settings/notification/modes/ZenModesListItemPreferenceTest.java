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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.service.notification.ZenModeConfig;

import com.android.settingslib.notification.modes.ZenMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class ZenModesListItemPreferenceTest {

    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void constructor_setsMode() {
        ZenModesListItemPreference preference = new ZenModesListItemPreference(mContext,
                TestModeBuilder.EXAMPLE);

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

        ZenModesListItemPreference preference = new ZenModesListItemPreference(mContext, mode);
        ShadowLooper.idleMainLooper(); // To load icon.

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

        ZenModesListItemPreference preference = new ZenModesListItemPreference(mContext, mode);
        ShadowLooper.idleMainLooper();

        assertThat(preference.getTitle()).isEqualTo("Active mode");
        assertThat(preference.getSummary()).isEqualTo("ON • When Birnam forest comes to Dunsinane");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_modeDisabledByApp() {
        ZenModeConfig.ZenRule configRule = new ZenModeConfig.ZenRule();
        configRule.enabled = false;
        configRule.disabledOrigin = ZenModeConfig.UPDATE_ORIGIN_APP;
        ZenMode mode = new TestModeBuilder()
                .setName("Mode disabled by app")
                .setTriggerDescription("When the cat's away")
                .setEnabled(false)
                .setConfigZenRule(configRule)
                .build();

        ZenModesListItemPreference preference = new ZenModesListItemPreference(mContext, mode);
        ShadowLooper.idleMainLooper();

        assertThat(preference.getTitle()).isEqualTo("Mode disabled by app");
        assertThat(preference.getSummary()).isEqualTo("Tap to set up");
        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void setZenMode_modeDisabledByUser() {
        ZenModeConfig.ZenRule configRule = new ZenModeConfig.ZenRule();
        configRule.enabled = false;
        configRule.disabledOrigin = ZenModeConfig.UPDATE_ORIGIN_USER;
        ZenMode mode = new TestModeBuilder()
                .setName("Mode disabled by user")
                .setTriggerDescription("When the Levee Breaks")
                .setEnabled(false)
                .setConfigZenRule(configRule)
                .build();

        ZenModesListItemPreference preference = new ZenModesListItemPreference(mContext, mode);
        ShadowLooper.idleMainLooper();

        assertThat(preference.getTitle()).isEqualTo("Mode disabled by user");
        assertThat(preference.getSummary()).isEqualTo("Paused");
        assertThat(preference.getIcon()).isNotNull();
    }
}
