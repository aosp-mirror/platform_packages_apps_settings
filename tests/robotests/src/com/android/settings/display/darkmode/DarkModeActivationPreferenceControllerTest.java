/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import androidx.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@RunWith(RobolectricTestRunner.class)
public class DarkModeActivationPreferenceControllerTest {
    private DarkModeActivationPreferenceController mController;
    private String mPreferenceKey = "key";
    @Mock
    private LayoutPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Resources res;
    @Mock
    private Context mContext;
    @Mock
    private UiModeManager mService;
    @Mock
    private Button mTurnOffButton;
    @Mock
    private Button mTurnOnButton;
    @Mock
    private PowerManager mPM;

    private Configuration configNightYes = new Configuration();
    private Configuration configNightNo = new Configuration();;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mService = mock(UiModeManager.class);
        when(mContext.getResources()).thenReturn(res);
        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);
        when(mContext.getSystemService(PowerManager.class)).thenReturn(mPM);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mPreference.findViewById(
                eq(R.id.dark_ui_turn_on_button))).thenReturn(mTurnOnButton);
        when(mPreference.findViewById(
                eq(R.id.dark_ui_turn_off_button))).thenReturn(mTurnOffButton);
        when(mService.setNightModeActivated(anyBoolean())).thenReturn(true);
        when(mContext.getString(
                R.string.dark_ui_activation_off_auto)).thenReturn("off_auto");
        when(mContext.getString(
                R.string.dark_ui_activation_on_auto)).thenReturn("on_auto");
        when(mContext.getString(
                R.string.dark_ui_activation_off_manual)).thenReturn("off_manual");
        when(mContext.getString(
                R.string.dark_ui_activation_on_manual)).thenReturn("on_manual");
        when(mContext.getString(
                R.string.dark_ui_summary_off_auto_mode_auto)).thenReturn("summary_off_auto");
        when(mContext.getString(
                R.string.dark_ui_summary_on_auto_mode_auto)).thenReturn("summary_on_auto");
        when(mContext.getString(
                R.string.dark_ui_summary_off_auto_mode_never)).thenReturn("summary_off_manual");
        when(mContext.getString(
                R.string.dark_ui_summary_on_auto_mode_never)).thenReturn("summary_on_manual");
        mController = new DarkModeActivationPreferenceController(mContext, mPreferenceKey);
        mController.displayPreference(mScreen);
        configNightNo.uiMode = Configuration.UI_MODE_NIGHT_NO;
        configNightYes.uiMode = Configuration.UI_MODE_NIGHT_YES;
    }

    @Test
    public void nightMode_toggleButton_offManual() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        when(res.getConfiguration()).thenReturn(configNightYes);

        mController.updateState(mPreference);

        verify(mTurnOnButton).setVisibility(eq(View.GONE));
        verify(mTurnOffButton).setVisibility(eq(View.VISIBLE));
        verify(mTurnOffButton).setText(eq(mContext.getString(
                R.string.dark_ui_activation_off_manual)));
    }


    @Test
    public void nightMode_toggleButton_onAutoWhenModeIsYes() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        when(res.getConfiguration()).thenReturn(configNightNo);

        mController.updateState(mPreference);

        verify(mTurnOffButton).setVisibility(eq(View.GONE));
        verify(mTurnOnButton).setVisibility(eq(View.VISIBLE));
        verify(mTurnOnButton).setText(eq(mContext.getString(
                R.string.dark_ui_activation_on_manual)));
    }

    @Test
    public void nightMode_toggleButton_onAutoWhenModeIsAuto() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_AUTO);
        when(res.getConfiguration()).thenReturn(configNightNo);

        mController.updateState(mPreference);

        verify(mTurnOffButton).setVisibility(eq(View.GONE));
        verify(mTurnOnButton).setVisibility(eq(View.VISIBLE));
        verify(mTurnOnButton).setText(eq(mContext.getString(
                R.string.dark_ui_activation_on_auto)));
    }

    @Test
    public void nightModeSummary_buttonText_onManual() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_NO);
        when(res.getConfiguration()).thenReturn(configNightYes);

        assertEquals(mController.getSummary(), mContext.getString(
                R.string.dark_ui_summary_on_auto_mode_never));
    }

    @Test
    public void nightModeSummary_buttonText_offAuto() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_AUTO);
        when(res.getConfiguration()).thenReturn(configNightNo);

        assertEquals(mController.getSummary(), mContext.getString(
                R.string.dark_ui_summary_off_auto_mode_auto));
    }

    @Test
    public void buttonVisisbility_hideButton_offWhenInPowerSaveMode() {
        when(mPM.isPowerSaveMode()).thenReturn(true);
        mController.updateState(mPreference);
        verify(mTurnOffButton).setVisibility(eq(View.GONE));
        verify(mTurnOnButton).setVisibility(eq(View.GONE));
    }
}
