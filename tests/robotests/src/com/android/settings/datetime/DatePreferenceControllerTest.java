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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.timedetector.TimeDetector;
import android.content.Context;

import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DatePreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private TimeDetector mTimeDetector;
    @Mock
    private DatePreferenceController.DatePreferenceHost mHost;
    @Mock
    private AutoTimePreferenceController mAutoTimePreferenceController;

    private RestrictedPreference mPreference;
    private DatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(TimeDetector.class)).thenReturn(mTimeDetector);
        mPreference = new RestrictedPreference(RuntimeEnvironment.application);
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
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        when(mAutoTimePreferenceController.isEnabled()).thenReturn(true);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_autoTimeDisabled_shouldEnablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

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
