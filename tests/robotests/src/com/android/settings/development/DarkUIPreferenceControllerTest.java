/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.development;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class DarkUIPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UiModeManager mUiModeManager;
    private DarkUIPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DarkUIPreferenceController(mContext, mUiModeManager);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_setAuto() {
        mController.onPreferenceChange(mPreference, "auto");
        verify(mUiModeManager).setNightMode(eq(UiModeManager.MODE_NIGHT_AUTO));
    }

    @Test
    public void onPreferenceChanged_setNightMode() {
        mController.onPreferenceChange(mPreference, "yes");
        verify(mUiModeManager).setNightMode(eq(UiModeManager.MODE_NIGHT_YES));
    }

    @Test
    public void onPreferenceChanged_setDayMode() {
        mController.onPreferenceChange(mPreference, "no");
        verify(mUiModeManager).setNightMode(eq(UiModeManager.MODE_NIGHT_NO));
    }

    public int getCurrentMode() {
        final UiModeManager uiModeManager = mContext.getSystemService(UiModeManager.class);
        return uiModeManager.getNightMode();
    }
}
