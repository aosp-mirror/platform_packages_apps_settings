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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.WifiConnectionPreferenceController;
import com.android.settings.wifi.WifiEntryPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiConnectionPreferenceControllerTest {
    private static final String KEY = "wifi_connection";

    @Mock
    WifiPickerTracker mWifiPickerTracker;
    @Mock
    PreferenceScreen mScreen;
    @Mock
    PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private WifiConnectionPreferenceController mController;
    private int mOnChildUpdatedCount;
    private WifiConnectionPreferenceController.UpdateListener mUpdateListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        when(fakeFeatureFactory.wifiTrackerLibProvider.createWifiPickerTracker(
                any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mWifiPickerTracker);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mScreen.findPreference(eq(KEY))).thenReturn(mPreferenceCategory);
        when(mScreen.getContext()).thenReturn(mContext);
        mUpdateListener = () -> mOnChildUpdatedCount++;

        mController = new WifiConnectionPreferenceController(mContext, mLifecycle, mUpdateListener,
                KEY, 0, 0);
        mController.mWifiPickerTracker = mWifiPickerTracker;
    }

    @Test
    public void isAvailable_noConnectedWifiEntry_availableIsFalse() {
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_noConnectedWifiEntry_noPreferenceAdded() {
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(null);

        mController.displayPreference(mScreen);

        verify(mPreferenceCategory, never()).addPreference(any());
    }

    @Test
    public void displayPreference_hasConnectedWifiEntry_preferenceAdded() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(wifiEntry);

        mController.displayPreference(mScreen);
        verify(mPreferenceCategory).addPreference(any(WifiEntryPreference.class));
    }

    @Test
    public void onConnectedChanged_wifiBecameDisconnected_preferenceRemoved() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(wifiEntry);

        mController.displayPreference(mScreen);
        final ArgumentCaptor<WifiEntryPreference> captor = ArgumentCaptor.forClass(
                WifiEntryPreference.class);
        verify(mPreferenceCategory).addPreference(captor.capture());
        final WifiEntryPreference pref = captor.getValue();

        // Become disconnected.
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(null);
        final int onUpdatedCountBefore = mOnChildUpdatedCount;

        mController.onWifiStateChanged();

        verify(mPreferenceCategory).removePreference(pref);
        assertThat(mOnChildUpdatedCount).isEqualTo(onUpdatedCountBefore + 1);
    }


    @Test
    public void onAccessPointsChanged_wifiBecameConnectedToDifferentAP_preferenceReplaced() {
        final WifiEntry wifiEntry1 = mock(WifiEntry.class);
        when(wifiEntry1.getKey()).thenReturn("KEY_1");
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(wifiEntry1);
        mController.displayPreference(mScreen);
        final ArgumentCaptor<WifiEntryPreference> captor = ArgumentCaptor.forClass(
                WifiEntryPreference.class);

        final WifiEntry wifiEntry2 = mock(WifiEntry.class);
        when(wifiEntry1.getKey()).thenReturn("KEY_2");
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(wifiEntry2);
        final int onUpdatedCountBefore = mOnChildUpdatedCount;
        mController.onWifiEntriesChanged();

        verify(mPreferenceCategory, times(2)).addPreference(captor.capture());
        final WifiEntryPreference pref1 = captor.getAllValues().get(0);
        final WifiEntryPreference pref2 = captor.getAllValues().get(1);
        assertThat(pref1.getWifiEntry()).isEqualTo(wifiEntry1);
        assertThat(pref2.getWifiEntry()).isEqualTo(wifiEntry2);
        verify(mPreferenceCategory).removePreference(eq(pref1));
        assertThat(mOnChildUpdatedCount).isEqualTo(onUpdatedCountBefore + 1);
    }
}
