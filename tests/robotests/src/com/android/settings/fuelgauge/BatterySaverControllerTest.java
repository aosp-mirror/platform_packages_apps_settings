/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverControllerTest {

    @Mock
    private PowerManager mPowerManager;

    private BatterySaverController mBatterySaverController;
    private PrimarySwitchPreference mBatterySaverPref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context mContext = spy(RuntimeEnvironment.application);

        mBatterySaverPref = new PrimarySwitchPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder mHolder =
                PreferenceViewHolder.createInstanceForTests(inflater.inflate(
                com.android.settingslib.R.layout.preference_two_target, null));
        final LinearLayout mWidgetView = mHolder.itemView.findViewById(android.R.id.widget_frame);
        inflater.inflate(R.layout.restricted_preference_widget_primary_switch, mWidgetView, true);
        mBatterySaverPref.onBindViewHolder(mHolder);

        doReturn(mPowerManager).when(mContext).getSystemService(Context.POWER_SERVICE);

        mBatterySaverController = new BatterySaverController(mContext);
        mBatterySaverController.mBatterySaverPref = mBatterySaverPref;
    }

    @Test
    public void onBatteryChanged_true_switchEnabled() {
        mBatterySaverController.onBatteryChanged(true);

        assertThat(mBatterySaverPref.getSwitch().isEnabled()).isFalse();
    }

    @Test
    public void onBatteryChanged_false_switchDisabled() {
        mBatterySaverController.onBatteryChanged(false);

        assertThat(mBatterySaverPref.getSwitch().isEnabled()).isTrue();
    }

    @Test
    public void onPowerSaveModeChanged_differentState_updateToIsChecked() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        assertThat(mBatterySaverPref.isChecked()).isFalse();

        mBatterySaverController.onPowerSaveModeChanged();

        assertThat(mBatterySaverPref.isChecked()).isTrue();
    }

    @Test
    public void onPowerSaveModeChanged_differentState_updateToUnChecked() {
        mBatterySaverPref.setChecked(true);

        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        assertThat(mBatterySaverPref.isChecked()).isTrue();

        mBatterySaverController.onPowerSaveModeChanged();

        assertThat(mBatterySaverPref.isChecked()).isFalse();
    }

    @Test
    public void onPowerSaveModeChanged_sameState_noUpdate() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        assertThat(mBatterySaverPref.isChecked()).isFalse();

        mBatterySaverController.onPowerSaveModeChanged();

        assertThat(mBatterySaverPref.isChecked()).isFalse();
    }

    @Test
    public void setChecked_on_setPowerSaveMode() {
        mBatterySaverController.setChecked(true);

        verify(mPowerManager).setPowerSaveModeEnabled(true);
    }

    @Test
    public void setChecked_off_unsetPowerSaveMode() {
        mBatterySaverController.setChecked(false);

        verify(mPowerManager).setPowerSaveModeEnabled(false);
    }

    @Test
    public void isChecked_on_powerSaveModeOn() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        assertThat(mBatterySaverController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_off_powerSaveModeOff() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        assertThat(mBatterySaverController.isChecked()).isFalse();
    }
}
