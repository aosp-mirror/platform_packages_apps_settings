/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.wifi.WifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SavedAccessPointsPreferenceController2Test {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private SavedAccessPointsWifiSettings2 mSettings;
    private SavedAccessPointsPreferenceController2 mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new SavedAccessPointsWifiSettings2());
        mController = spy(new SavedAccessPointsPreferenceController2(mContext, "test_key"));
        mController.setHost(mSettings);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void getAvailability_noSavedAccessPoint_shouldNotAvailable() {
        mController.mWifiEntries = new ArrayList<>();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailability_oneSavedAccessPoint_shouldAvailable() {
        final WifiEntry mockWifiEntry = mock(WifiEntry.class);
        mController.mWifiEntries = Arrays.asList(mockWifiEntry);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_oneAccessPoint_shouldListIt() {
        final String title = "ssid_title";
        final WifiEntry mockWifiEntry = mock(WifiEntry.class);
        when(mockWifiEntry.getTitle()).thenReturn(title);
        final ArgumentCaptor<WifiEntryPreference> captor =
                ArgumentCaptor.forClass(WifiEntryPreference.class);

        mController.displayPreference(mPreferenceScreen, Arrays.asList(mockWifiEntry));

        verify(mPreferenceCategory).addPreference(captor.capture());

        final List<WifiEntryPreference> prefs = captor.getAllValues();
        assertThat(prefs.size()).isEqualTo(1);
        assertThat(prefs.get(0).getTitle()).isEqualTo(title);
    }

    @Test
    public void displayPreference_noAccessPoint_shouldRemoveIt() {
        final String title = "ssid_title";
        final String key = "key";
        final WifiEntry mockWifiEntry = mock(WifiEntry.class);
        when(mockWifiEntry.getTitle()).thenReturn(title);
        when(mockWifiEntry.getKey()).thenReturn(key);
        final WifiEntryPreference preference = new WifiEntryPreference(mContext, mockWifiEntry);
        preference.setKey(key);
        mPreferenceCategory.addPreference(preference);

        mController.displayPreference(mPreferenceScreen, new ArrayList<>());

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }
}
