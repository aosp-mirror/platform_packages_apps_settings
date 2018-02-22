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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.actions.OpenRestrictAppFragmentAction;
import com.android.settings.fuelgauge.batterytip.actions.RestrictAppAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryTipUtilsTest {
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    private RestrictAppTip mRestrictAppTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest();
        doReturn(RuntimeEnvironment.application).when(mFragment).getContext();
        mRestrictAppTip = spy(new RestrictAppTip(BatteryTip.StateType.NEW, new ArrayList<>()));
    }

    @Test
    public void testGetActionForBatteryTip_typeRestrictStateNew_returnActionRestrict() {
        doReturn(BatteryTip.StateType.NEW).when(mRestrictAppTip).getState();

        assertThat(BatteryTipUtils.getActionForBatteryTip(mRestrictAppTip, mSettingsActivity,
                mFragment)).isInstanceOf(RestrictAppAction.class);
    }

    @Test
    public void testGetActionForBatteryTip_typeRestrictStateHandled_returnActionOpen() {
        doReturn(BatteryTip.StateType.HANDLED).when(mRestrictAppTip).getState();

        assertThat(BatteryTipUtils.getActionForBatteryTip(mRestrictAppTip, mSettingsActivity,
                mFragment)).isInstanceOf(OpenRestrictAppFragmentAction.class);
    }

}
