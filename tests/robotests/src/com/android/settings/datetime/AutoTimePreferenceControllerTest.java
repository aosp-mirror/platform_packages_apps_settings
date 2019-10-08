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

import android.content.Context;
import android.provider.Settings;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoTimePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private Context mContext;
    private RestrictedSwitchPreference mPreference;
    private AutoTimePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new RestrictedSwitchPreference(mContext);
        mController = new AutoTimePreferenceController(mContext, mCallback);
    }

    @Test
    public void testIsEnabled_shouldReadFromSettingsProvider() {
        // Disabled
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME, 0);
        assertThat(mController.isEnabled()).isFalse();

        // Enabled
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME, 1);
        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isEnabled()).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isEnabled()).isFalse();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }
}
