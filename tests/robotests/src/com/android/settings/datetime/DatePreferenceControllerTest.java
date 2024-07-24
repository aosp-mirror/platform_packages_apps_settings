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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.DatePickerDialog;
import android.app.time.Capabilities;
import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.app.timedetector.TimeDetector;
import android.app.timedetector.TimeDetectorHelper;
import android.content.Context;
import android.os.UserHandle;

import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class DatePreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private DatePreferenceController.DatePreferenceHost mHost;
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private TimeDetector mTimeDetector;

    private RestrictedPreference mPreference;
    private DatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeDetector.class)).thenReturn(mTimeDetector);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        mPreference = new RestrictedPreference(RuntimeEnvironment.application);
        mController = new DatePreferenceController(mContext, "test_key");
        mController.setHost(mHost);
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

        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* suggestManualAllowed= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_autoTimeDisabled_shouldEnablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* suggestManualAllowed= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
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

    @Test
    public void testBuildDatePicker() {
        TimeDetectorHelper timeDetectorHelper = mock(TimeDetectorHelper.class);
        when(timeDetectorHelper.getManualDateSelectionYearMin()).thenReturn(2015);
        when(timeDetectorHelper.getManualDateSelectionYearMax()).thenReturn(2020);

        Context context = RuntimeEnvironment.application;
        DatePickerDialog dialog = mController.buildDatePicker(context, timeDetectorHelper);

        GregorianCalendar calendar = new GregorianCalendar();

        long minDate = dialog.getDatePicker().getMinDate();
        calendar.setTimeInMillis(minDate);
        assertEquals(2015, calendar.get(Calendar.YEAR));

        long maxDate = dialog.getDatePicker().getMaxDate();
        calendar.setTimeInMillis(maxDate);
        assertEquals(2020, calendar.get(Calendar.YEAR));
    }

    static TimeCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean suggestManualAllowed) {
        int suggestManualCapability = suggestManualAllowed ? Capabilities.CAPABILITY_POSSESSED
                : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeCapabilities capabilities = new TimeCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setSetManualTimeCapability(suggestManualCapability)
                .build();
        TimeConfiguration config = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(!suggestManualAllowed)
                .build();
        return new TimeCapabilitiesAndConfig(capabilities, config);
    }
}
