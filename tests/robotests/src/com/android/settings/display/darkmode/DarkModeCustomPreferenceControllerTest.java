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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
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
    public void nightMode_customOff_hidePreference() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        mController.refreshSummary(mPreference);
        verify(mPreference).setVisible(eq(false));
    }

    @Test
    public void nightMode_customOff_showPreference() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mController.refreshSummary(mPreference);
        verify(mPreference).setVisible(eq(true));
    }

    @Test
    public void nightMode_customOff_setSummaryNotNull() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        mController.refreshSummary(mPreference);
        verify(mPreference).setSummary(eq(mFormat.of(null)));
    }
}
