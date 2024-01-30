/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PowerManager;
import android.provider.SettingsSlicesContract;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverButtonPreferenceControllerTest {

    private BatterySaverButtonPreferenceController mController;
    private Context mContext;
    private MainSwitchPreference mPreference;

    @Mock private PowerManager mPowerManager;
    @Mock private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = spy(new MainSwitchPreference(mContext, null /* AttributeSet */));

        doReturn(mPowerManager).when(mContext).getSystemService(Context.POWER_SERVICE);
        doReturn(mPreference).when(mPreferenceScreen).findPreference(anyString());

        mController = new BatterySaverButtonPreferenceController(mContext, "test_key");
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void getSliceUri_shouldUsePlatformAuthority() {
        assertThat(mController.getSliceUri().getAuthority())
                .isEqualTo(SettingsSlicesContract.AUTHORITY);
    }

    @Test
    public void updateState_lowPowerOn_preferenceIsChecked() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        mPreference.updateStatus(mPowerManager.isPowerSaveMode());

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_lowPowerOff_preferenceIsUnchecked() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        mPreference.updateStatus(mPowerManager.isPowerSaveMode());

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_on_setPowerSaveMode() {
        mController.setChecked(true);

        verify(mPowerManager).setPowerSaveModeEnabled(true);
    }

    @Test
    public void setChecked_off_unsetPowerSaveMode() {
        mController.setChecked(false);

        verify(mPowerManager).setPowerSaveModeEnabled(false);
    }

    @Test
    public void onBatteryChanged_pluggedIn_preferenceDisabled() {
        mController.onBatteryChanged(/* pluggedIn */ true);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onBatteryChanged_unplugged_preferenceEnabled() {
        mController.onBatteryChanged(/* pluggedIn */ false);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
