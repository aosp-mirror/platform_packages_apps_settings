/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowDateFormat.class,
})
public class DarkModeCustomPreferenceControllerTest {
    private DarkModeCustomPreferenceController mController;
    @Mock
    private UiModeManager mService;
    @Mock
    private Context mContext;
    @Mock
    private Preference mPreference;
    @Mock
    private Resources mResources;
    @Mock
    private ContentResolver mCR;
    @Mock
    private TimeFormatter mFormat;
    @Mock
    private DarkModeSettingsFragment mFragment;
    private Configuration mConfig = new Configuration();


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getConfiguration()).thenReturn(mConfig);
        when(mContext.getContentResolver()).thenReturn(mCR);
        mService = mock(UiModeManager.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);
        mController = new DarkModeCustomPreferenceController(mContext, "key", mFragment, mFormat);
        when(mFormat.is24HourFormat()).thenReturn(false);
        when(mFormat.of(any())).thenReturn("10:00 AM");
    }

    @Test
    public void getAvailabilityStatus_nightModeManualOn_unavailable() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_YES;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nightModeManualOff_unavailable() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_NO);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_NO;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nightModeCustomOn_available() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_YES;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nightModeCustomOff_available() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_NO;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nightModeCustomBedtimeOn_unavailable() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        when(mService.getNightModeCustomType())
                .thenReturn(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_YES;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nightModeCustomBedtimeOff_unavailable() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        when(mService.getNightModeCustomType())
                .thenReturn(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_NO;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void nightMode_customOn_setSummaryTo10Am() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_YES;

        mController.refreshSummary(mPreference);

        verify(mPreference).setSummary(eq("10:00 AM"));
    }

    @Test
    public void nightMode_customOff_setSummaryTo10Am() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mConfig.uiMode = Configuration.UI_MODE_NIGHT_NO;

        mController.refreshSummary(mPreference);

        verify(mPreference).setSummary(eq("10:00 AM"));
    }
}
