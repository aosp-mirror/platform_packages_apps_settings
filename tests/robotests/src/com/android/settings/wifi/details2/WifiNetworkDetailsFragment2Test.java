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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkDetailsFragment2Test {

    private WifiNetworkDetailsFragment2 mFragment;

    @Before
    public void setUp() {
        mFragment = new WifiNetworkDetailsFragment2();
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
        final Menu menu = mock(Menu.class);
        final MenuItem menuItem = mock(MenuItem.class);
        doReturn(menuItem).when(menu).add(anyInt(), eq(Menu.FIRST), anyInt(), anyInt());

        mFragment.onCreateOptionsMenu(menu, mock(MenuInflater.class));

        verify(menuItem).setIcon(com.android.internal.R.drawable.ic_mode_edit);
    }
}
