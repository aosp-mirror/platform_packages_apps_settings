/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import static com.android.settings.wifi.WifiSettings.WIFI_DIALOG_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkDetailsFragment2Test {

    final String TEST_PREFERENCE_KEY = "TEST_PREFERENCE_KEY";

    @Mock
    WifiEntry mWifiEntry;
    @Mock
    NetworkDetailsTracker mNetworkDetailsTracker;
    @Mock
    Menu mMenu;
    private WifiNetworkDetailsFragment2 mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mWifiEntry).when(mNetworkDetailsTracker).getWifiEntry();
        doReturn(true).when(mWifiEntry).isSaved();

        mFragment = new WifiNetworkDetailsFragment2();
        mFragment.mNetworkDetailsTracker = mNetworkDetailsTracker;
    }

    @Test
    public void getMetricsCategory_shouldReturnWifiNetworkDetails() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.WIFI_NETWORK_DETAILS);
    }

    @Test
    public void getDialogMetricsCategory_withCorrectId_shouldReturnDialogWifiApEdit() {
        assertThat(mFragment.getDialogMetricsCategory(WIFI_DIALOG_ID)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_AP_EDIT);
    }

    @Test
    public void getDialogMetricsCategory_withWrongId_shouldReturnZero() {
        assertThat(mFragment.getDialogMetricsCategory(-1 /* dialogId */)).isEqualTo(0);
    }

    @Test
    public void onCreateOptionsMenu_shouldSetCorrectIcon() {
        final MenuItem menuItem = mock(MenuItem.class);
        doReturn(menuItem).when(mMenu).add(anyInt(), eq(Menu.FIRST), anyInt(), anyInt());

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(menuItem).setIcon(com.android.internal.R.drawable.ic_mode_edit);
    }

    @Test
    public void onCreateOptionsMenu_isNotSavedNetwork_shouldNotAddEditMenu() {
        doReturn(false).when(mWifiEntry).isSaved();

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), eq(R.string.wifi_modify));
    }

    @Test
    public void refreshPreferences_controllerShouldUpdateStateAndDisplayPreference() {
        final FakeFragment fragment = spy(new FakeFragment());
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final Preference preference = mock(Preference.class);
        final TestController controller = mock(TestController.class);
        doReturn(screen).when(fragment).getPreferenceScreen();
        doReturn(preference).when(screen).findPreference(TEST_PREFERENCE_KEY);
        doReturn(TEST_PREFERENCE_KEY).when(controller).getPreferenceKey();
        fragment.mControllers = new ArrayList<>();
        fragment.mControllers.add(controller);
        fragment.addPreferenceController(controller);

        fragment.refreshPreferences();

        verify(controller).updateState(preference);
        verify(controller).displayPreference(screen);
    }

    // Fake WifiNetworkDetailsFragment2 to override the protected method as public.
    public class FakeFragment extends WifiNetworkDetailsFragment2 {

        @Override
        public void addPreferenceController(AbstractPreferenceController controller) {
            super.addPreferenceController(controller);
        }
    }

    public class TestController extends BasePreferenceController {

        public TestController() {
            super(RuntimeEnvironment.application, TEST_PREFERENCE_KEY);
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}
