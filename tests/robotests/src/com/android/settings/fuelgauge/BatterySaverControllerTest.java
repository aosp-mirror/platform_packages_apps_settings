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

import android.content.Context;
import android.os.PowerManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatterySaverControllerTest {
    @Mock
    private MasterSwitchPreference mBatterySaverPref;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private Context mContext;
    @Mock
    private Lifecycle mLifecycle;
    private BatterySaverController mBatterySaverController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBatterySaverController = spy(new BatterySaverController(mContext, mLifecycle));
        ReflectionHelpers.setField(mBatterySaverController, "mPowerManager", mPowerManager);
        ReflectionHelpers.setField(mBatterySaverController, "mBatterySaverPref", mBatterySaverPref);
        doNothing().when(mBatterySaverController).refreshConditionManager();
    }

    @Test
    public void testOnPreferenceChange_TurnOnBatterySaver_BatterySaverOn() {
        testOnPreferenceChangeInner(true);
    }

    @Test
    public void testOnPreferenceChange_TurnOffBatterySaver_BatterySaverOff() {
        testOnPreferenceChangeInner(false);
    }

    @Test
    public void testUpdateState_SaverModeOn_PreferenceChecked() {
        testUpdateStateInner(true);
    }

    @Test
    public void testUpdateState_SaverModeOff_PreferenceUnChecked() {
        testUpdateStateInner(false);
    }

    @Test
    public void testOnBatteryChanged_pluggedIn_setDisable() {
        mBatterySaverController.onBatteryChanged(true /* pluggedIn */);

        verify(mBatterySaverPref).setSwitchEnabled(false);
    }

    @Test
    public void testOnBatteryChanged_notPluggedIn_setEnable() {
        mBatterySaverController.onBatteryChanged(false /* pluggedIn */);

        verify(mBatterySaverPref).setSwitchEnabled(true);
    }

    private void testOnPreferenceChangeInner(final boolean saverOn) {
        when(mPowerManager.setPowerSaveMode(saverOn)).thenReturn(true);
        when(mPowerManager.isPowerSaveMode()).thenReturn(!saverOn);

        mBatterySaverController.onPreferenceChange(mBatterySaverPref, saverOn);
        verify(mPowerManager).setPowerSaveMode(saverOn);
    }

    private void testUpdateStateInner(final boolean saverOn) {
        when(mPowerManager.isPowerSaveMode()).thenReturn(saverOn);

        mBatterySaverController.updateState(mBatterySaverPref);
        verify(mBatterySaverPref).setChecked(saverOn);
    }
}
