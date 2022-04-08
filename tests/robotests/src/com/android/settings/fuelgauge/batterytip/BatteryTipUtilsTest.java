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
 * limitations under the License.
 */

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.actions.BatterySaverAction;
import com.android.settings.fuelgauge.batterytip.actions.OpenBatterySaverAction;
import com.android.settings.fuelgauge.batterytip.actions.OpenRestrictAppFragmentAction;
import com.android.settings.fuelgauge.batterytip.actions.RestrictAppAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class BatteryTipUtilsTest {

    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    private RestrictAppTip mRestrictAppTip;
    private EarlyWarningTip mEarlyWarningTip;
    private LowBatteryTip mLowBatteryTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest();
        when(mSettingsActivity.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mFragment.getContext()).thenReturn(RuntimeEnvironment.application);
        mRestrictAppTip = spy(new RestrictAppTip(BatteryTip.StateType.NEW, new ArrayList<>()));
        mEarlyWarningTip = spy(
                new EarlyWarningTip(BatteryTip.StateType.NEW, true /* powerSaveModeOn */));
        mLowBatteryTip = spy(
                new LowBatteryTip(BatteryTip.StateType.NEW, false /* powerSaveModeOn */,
                        "" /* summary */));
    }

    @Test
    public void testGetActionForBatteryTip_typeRestrictStateNew_returnActionRestrict() {
        when(mRestrictAppTip.getState()).thenReturn(BatteryTip.StateType.NEW);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mRestrictAppTip, mSettingsActivity,
                mFragment)).isInstanceOf(RestrictAppAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeRestrictStateHandled_returnActionOpen() {
        when(mRestrictAppTip.getState()).thenReturn(BatteryTip.StateType.HANDLED);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mRestrictAppTip, mSettingsActivity,
                mFragment)).isInstanceOf(OpenRestrictAppFragmentAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeEarlyWarningStateNew_returnActionBatterySaver() {
        when(mEarlyWarningTip.getState()).thenReturn(BatteryTip.StateType.NEW);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mEarlyWarningTip, mSettingsActivity,
                mFragment)).isInstanceOf(BatterySaverAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeEarlyWarningStateHandled_returnActionOpen() {
        when(mEarlyWarningTip.getState()).thenReturn(BatteryTip.StateType.HANDLED);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mEarlyWarningTip, mSettingsActivity,
                mFragment)).isInstanceOf(OpenBatterySaverAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeLowBatteryStateNew_returnActionBatterySaver() {
        when(mLowBatteryTip.getState()).thenReturn(BatteryTip.StateType.NEW);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mLowBatteryTip, mSettingsActivity,
                mFragment)).isInstanceOf(BatterySaverAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeLowBatteryStateHandled_returnActionOpen() {
        when(mLowBatteryTip.getState()).thenReturn(BatteryTip.StateType.HANDLED);

        assertThat(BatteryTipUtils.getActionForBatteryTip(mLowBatteryTip, mSettingsActivity,
                mFragment)).isInstanceOf(OpenBatterySaverAction.class);
    }
}
