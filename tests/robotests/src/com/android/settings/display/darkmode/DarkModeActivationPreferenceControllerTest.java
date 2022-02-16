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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.PowerManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class DarkModeActivationPreferenceControllerTest {
    private DarkModeActivationPreferenceController mController;
    private String mPreferenceKey = "key";

    @Mock
    private MainSwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Resources res;
    @Mock
    private UiModeManager mService;
    @Mock
    private PowerManager mPM;
    @Mock
    private TimeFormatter mFormat;

    private Context mContext;
    private Configuration mConfigNightYes = new Configuration();
    private Configuration mConfigNightNo = new Configuration();
    private Locale mLocal = new Locale("ENG");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        mService = mock(UiModeManager.class);
        when(mContext.getResources()).thenReturn(res);
        when(res.getConfiguration()).thenReturn(mConfigNightNo);
        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);
        when(mContext.getSystemService(PowerManager.class)).thenReturn(mPM);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mService.setNightModeActivated(anyBoolean())).thenReturn(true);
        when(mFormat.of(any())).thenReturn("10:00 AM");
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
        when(mContext.getString(
                R.string.dark_ui_summary_on_auto_mode_custom)).thenReturn("summary_on_custom");
        when(mContext.getString(
                R.string.dark_ui_summary_off_auto_mode_custom)).thenReturn("summary_off_custom");
        mController = new DarkModeActivationPreferenceController(mContext, mPreferenceKey, mFormat);
        mController.displayPreference(mScreen);
        mConfigNightNo.uiMode = Configuration.UI_MODE_NIGHT_NO;
        mConfigNightYes.uiMode = Configuration.UI_MODE_NIGHT_YES;
        mConfigNightNo.locale = mLocal;
        mConfigNightYes.locale = mLocal;
    }

    @Test
    public void nightMode_toggleButton_offManual() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        when(res.getConfiguration()).thenReturn(mConfigNightYes);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext);

        mController.updateState(mPreference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void nightMode_toggleButton_offCustom() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        when(res.getConfiguration()).thenReturn(mConfigNightYes);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void nightMode_toggleButton_onCustom() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_CUSTOM);
        when(res.getConfiguration()).thenReturn(mConfigNightYes);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void nightMode_toggleButton_onAutoWhenModeIsYes() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_YES);
        when(res.getConfiguration()).thenReturn(mConfigNightNo);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext, null);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void nightMode_toggleButton_onAutoWhenModeIsAuto() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_AUTO);
        when(res.getConfiguration()).thenReturn(mConfigNightNo);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void nightModeSummary_buttonText_onManual() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_NO);
        when(res.getConfiguration()).thenReturn(mConfigNightYes);

        assertEquals(mController.getSummary(), mContext.getString(
                R.string.dark_ui_summary_on_auto_mode_never));
    }

    @Test
    public void nightModeSummary_buttonText_offAuto() {
        when(mService.getNightMode()).thenReturn(UiModeManager.MODE_NIGHT_AUTO);
        when(res.getConfiguration()).thenReturn(mConfigNightNo);

        assertEquals(mController.getSummary(), mContext.getString(
                R.string.dark_ui_summary_off_auto_mode_auto));
    }

    @Test
    public void buttonVisisbility_hideButton_offWhenInPowerSaveMode() {
        when(mPM.isPowerSaveMode()).thenReturn(true);
        final MainSwitchPreference preference = new MainSwitchPreference(mContext);

        mController.updateState(preference);
        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_returnsAVAILABLE_UNSEARCHABLE() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }
}
