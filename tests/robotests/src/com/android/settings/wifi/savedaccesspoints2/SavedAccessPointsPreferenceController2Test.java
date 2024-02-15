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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.wifi.WifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SavedAccessPointsPreferenceController2Test {

    private static final String PREFERENCE_KEY = "preference_key";
    private static final String TEST_KEY = "key";
    private static final String TEST_TITLE = "ssid_title";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceGroup mPreferenceGroup;
    @Mock
    private WifiEntryPreference mWifiEntryPreference;
    @Mock
    private WifiEntry mWifiEntry;

    private SavedAccessPointsWifiSettings2 mSettings;
    private SavedAccessPointsPreferenceController2 mController;

    @Before
    public void setUp() {
        when(mPreferenceScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreferenceGroup);
        when(mPreferenceGroup.getContext()).thenReturn(mContext);
        when(mWifiEntryPreference.getKey()).thenReturn(TEST_KEY);
        when(mWifiEntryPreference.getTitle()).thenReturn(TEST_TITLE);
        when(mWifiEntry.getKey()).thenReturn(TEST_KEY);
        when(mWifiEntry.getTitle()).thenReturn(TEST_TITLE);

        mSettings = spy(new SavedAccessPointsWifiSettings2());
        mController = spy(new SavedAccessPointsPreferenceController2(mContext, PREFERENCE_KEY));
        mController.setHost(mSettings);
    }

    @Test
    public void getAvailability_noSavedAccessPoint_shouldNotAvailable() {
        mController.mWifiEntries = new ArrayList<>();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailability_oneSavedAccessPoint_shouldAvailable() {
        mController.mWifiEntries = Arrays.asList(mWifiEntry);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_newWifiEntry_addPreference() {
        when(mPreferenceGroup.getPreferenceCount()).thenReturn(0);

        mController.displayPreference(mPreferenceScreen, Arrays.asList(mWifiEntry));

        ArgumentCaptor<WifiEntryPreference> captor =
                ArgumentCaptor.forClass(WifiEntryPreference.class);
        verify(mPreferenceGroup).addPreference(captor.capture());
        List<WifiEntryPreference> prefs = captor.getAllValues();
        assertThat(prefs.size()).isEqualTo(1);
        assertThat(prefs.get(0).getTitle()).isEqualTo(TEST_TITLE);
    }

    @Test
    public void displayPreference_sameWifiEntry_preferenceSetWifiEntry() {
        when(mPreferenceGroup.getPreferenceCount()).thenReturn(1);
        when(mPreferenceGroup.getPreference(0)).thenReturn(mWifiEntryPreference);

        mController.displayPreference(mPreferenceScreen, Arrays.asList(mWifiEntry));

        verify(mWifiEntryPreference).setWifiEntry(mWifiEntry);
    }

    @Test
    public void displayPreference_removedWifiEntry_removePreference() {
        when(mPreferenceGroup.getPreferenceCount()).thenReturn(1);
        when(mPreferenceGroup.getPreference(0)).thenReturn(mWifiEntryPreference);

        mController.displayPreference(mPreferenceScreen, new ArrayList<>());

        verify(mPreferenceGroup).removePreference(any());
    }

    @Test
    public void onPreferenceClick_shouldCallShowWifiPage() {
        doNothing().when(mContext).startActivity(any());
        doReturn(mContext).when(mSettings).getContext();

        mController.onPreferenceClick(mWifiEntryPreference);

        verify(mSettings).showWifiPage(TEST_KEY, TEST_TITLE);
    }
}
