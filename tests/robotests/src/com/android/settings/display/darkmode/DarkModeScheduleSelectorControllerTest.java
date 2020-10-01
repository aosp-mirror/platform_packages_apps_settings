/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.PowerManager;

import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DarkModeScheduleSelectorControllerTest {
    private DarkModeScheduleSelectorController mController;
    private String mPreferenceKey = "key";
    @Mock
    private DropDownPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    @Mock
    private UiModeManager mUiService;
    @Mock
    private LocationManager mLocationManager;
    @Mock
    private PowerManager mPM;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mUiService);
        when(mContext.getSystemService(PowerManager.class)).thenReturn(mPM);
        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);
        when(mContext.getString(R.string.dark_ui_auto_mode_never)).thenReturn("never");
        when(mContext.getString(R.string.dark_ui_auto_mode_auto)).thenReturn("auto");
        when(mContext.getString(R.string.dark_ui_auto_mode_custom)).thenReturn("custom");
        mPreference = spy(new DropDownPreference(mContext));
        mPreference.setEntryValues(new CharSequence[]{
                mContext.getString(R.string.dark_ui_auto_mode_never),
                mContext.getString(R.string.dark_ui_auto_mode_auto)
        });
        doNothing().when(mPreference).setValueIndex(anyInt());
        when(mLocationManager.isLocationEnabled()).thenReturn(true);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mUiService.setNightModeActivated(anyBoolean())).thenReturn(true);
        mController = new DarkModeScheduleSelectorController(mContext, mPreferenceKey);
    }

    @Test
    public void nightMode_preferenceChange_preferenceChangeTrueWhenChangedOnly() {
        when(mUiService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        mController.displayPreference(mScreen);
        boolean changed = mController
                .onPreferenceChange(mScreen, mContext.getString(R.string.dark_ui_auto_mode_auto));
        assertTrue(changed);
        changed = mController
                .onPreferenceChange(mScreen, mContext.getString(R.string.dark_ui_auto_mode_auto));
        assertFalse(changed);
    }

    @Test
    public void nightMode_updateStateNone_dropDownValueChangedToNone() {
        when(mUiService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        mController.displayPreference(mScreen);
        mController.updateState(mScreen);
        verify(mPreference).setValueIndex(0);
    }

    @Test
    public void nightMode_updateStateNone_dropDownValueChangedToAuto() {
        when(mUiService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_AUTO);
        mController.displayPreference(mScreen);
        mController.updateState(mScreen);
        verify(mPreference).setValueIndex(1);
    }

    @Test
    public void batterySaver_dropDown_disabledSelector() {
        when(mPM.isPowerSaveMode()).thenReturn(true);
        mController.displayPreference(mScreen);
        mController.updateState(mScreen);
        verify(mPreference).setEnabled(eq(false));
    }
}
