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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeManager;
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
public class TimePreferenceControllerTest {

    private Context mContext;
    @Mock
    private TimePreferenceController.TimePreferenceHost mHost;
    @Mock
    private TimeManager mTimeManager;

    private TimePreferenceController mController;
    private RestrictedPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        mPreference = new RestrictedPreference(mContext);
        mController = new TimePreferenceController(mContext, "test_key");
        mController.setHost(mHost);
    }

    @Test
    public void updateState_dateEntryDisabled_shouldDisablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        TimeCapabilitiesAndConfig capabilitiesAndConfig =
                DatePreferenceControllerTest.createCapabilitiesAndConfig(/* suggestManualAllowed= */
                        false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_dateEntryEnabled_shouldEnablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        TimeCapabilitiesAndConfig capabilitiesAndConfig =
                DatePreferenceControllerTest.createCapabilitiesAndConfig(/* suggestManualAllowed= */
                        true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void clickPreference_showTimePicker() {
        // Click a preference that's not controlled by this controller.
        mPreference.setKey("fake_key");
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();

        // Click a preference controlled by this controller.
        mPreference.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(mPreference);
        // Should show date picker
        verify(mHost).showTimePicker();
    }
}
