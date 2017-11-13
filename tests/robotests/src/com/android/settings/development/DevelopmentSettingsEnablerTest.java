/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DevelopmentSettingsEnablerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SharedPreferences mDevelopmentPreferences;
    private Context mContext;
    private DevelopmentSettingsEnabler mEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mEnabler = new DevelopmentSettingsEnabler(mContext, null);
        ReflectionHelpers.setField(mEnabler, "mDevelopmentPreferences", mDevelopmentPreferences);
    }

    @Test
    public void constructor_shouldInitEnabledState() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        mEnabler = new DevelopmentSettingsEnabler(mContext, null);

        assertThat(mEnabler.getLastEnabledState()).isTrue();
    }

    @Test
    public void onResume_shouldReadStateFromSettingProvider() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        mEnabler.onResume();

        assertThat(mEnabler.getLastEnabledState()).isTrue();

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        mEnabler.onResume();

        assertThat(mEnabler.getLastEnabledState()).isFalse();
    }

    @Test
    public void disable_shouldChangeSettingProviderValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        mEnabler.disableDevelopmentSettings();

        assertThat(mEnabler.getLastEnabledState()).isFalse();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)).isEqualTo(0);
    }

    @Test
    public void enable_shouldChangeSettingProviderValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        mEnabler.enableDevelopmentSettings();

        assertThat(mEnabler.getLastEnabledState()).isTrue();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)).isEqualTo(1);
    }
}
