/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link VibrationMainSwitchPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class VibrationMainSwitchPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    @Mock private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private Context mContext;
    private VibrationMainSwitchPreferenceController mController;
    private MainSwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = ApplicationProvider.getApplicationContext();
        mController = new VibrationMainSwitchPreferenceController(mContext, PREFERENCE_KEY);
        mLifecycle.addObserver(mController);
        mPreference = new MainSwitchPreference(mContext);
        mPreference.setTitle("Test title");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_shouldReturnTheSettingState() {
        updateSetting(Settings.System.VIBRATE_ON, ON);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(Settings.System.VIBRATE_ON, OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_updatesSetting() throws Settings.SettingNotFoundException {
        updateSetting(Settings.System.VIBRATE_ON, OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        mController.setChecked(true);
        assertThat(readSetting(Settings.System.VIBRATE_ON)).isEqualTo(ON);

        mController.setChecked(false);
        assertThat(readSetting(Settings.System.VIBRATE_ON)).isEqualTo(OFF);
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSetting(String settingKey) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), settingKey);
    }
}
