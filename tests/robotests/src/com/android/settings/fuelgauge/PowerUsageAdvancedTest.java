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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PowerUsageAdvancedTest {
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private MenuItem mToggleAppsMenu;
    private Context mContext;
    private PowerUsageAdvanced mFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mToggleAppsMenu.getItemId()).thenReturn(PowerUsageAdvanced.MENU_TOGGLE_APPS);

        mFragment = spy(new PowerUsageAdvanced());
        mFragment.onAttach(mContext);
    }

    @Test
    public void testSaveInstanceState_showAllAppsRestored() {
        Bundle bundle = new Bundle();
        mFragment.mShowAllApps = true;
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        mFragment.onSaveInstanceState(bundle);
        mFragment.restoreSavedInstance(bundle);

        assertThat(mFragment.mShowAllApps).isTrue();
    }

    @Test
    public void testOptionsMenu_menuAppToggle_metricEventInvoked() {
        mFragment.mShowAllApps = false;
        doNothing().when(mFragment).restartBatteryStatsLoader(anyInt());

        mFragment.onOptionsItemSelected(mToggleAppsMenu);

        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_APPS_TOGGLE), eq(true));
    }

    @Test
    public void testOptionsMenu_toggleAppsEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isPowerAccountingToggleEnabled())
                .thenReturn(true);
        mFragment.mShowAllApps = false;

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, PowerUsageAdvanced.MENU_TOGGLE_APPS, Menu.NONE,
                R.string.show_all_apps);
    }
}
