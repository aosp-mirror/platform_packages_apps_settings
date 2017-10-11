/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import android.app.AlarmManager;
import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatePreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private AlarmManager mAlarmManager;
    @Mock
    private DatePreferenceController.DatePreferenceHost mHost;
    @Mock
    private AutoTimePreferenceController mAutoTimePreferenceController;

    private Preference mPreference;
    private DatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mAlarmManager);
        mPreference = new Preference(ShadowApplication.getInstance().getApplicationContext());
        mController = new DatePreferenceController(mContext, mHost, mAutoTimePreferenceController);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void shouldHandleDateSetCallback() {
        mController.onDateSet(null, 2016, 1, 1);
        verify(mHost).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void updateState_autoTimeEnabled_shouldDisablePref() {
        when(mAutoTimePreferenceController.isEnabled()).thenReturn(true);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_autoTimeDisabled_shouldEnablePref() {
        when(mAutoTimePreferenceController.isEnabled()).thenReturn(false);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void clickPreference_showDatePicker() {
        // Click a preference that's not controlled by this controller.
        mPreference.setKey("fake_key");
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();

        // Click a preference controlled by this controller.
        mPreference.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(mPreference);
        // Should show date picker
        verify(mHost).showDatePicker();
    }
}
