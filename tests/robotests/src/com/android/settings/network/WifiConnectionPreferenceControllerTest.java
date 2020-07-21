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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.wifi.WifiConnectionPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

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

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class WifiConnectionPreferenceControllerTest {
    private static final String KEY = "wifi_connection";

    @Mock
    WifiTracker mWifiTracker;
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
        WifiTrackerFactory.setTestingWifiTracker(mWifiTracker);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mScreen.findPreference(eq(KEY))).thenReturn(mPreferenceCategory);
        when(mScreen.getContext()).thenReturn(mContext);
        mUpdateListener = () -> mOnChildUpdatedCount++;

        mController = new WifiConnectionPreferenceController(mContext, mLifecycle, mUpdateListener,
                KEY, 0, 0);
    }

    @Test
    public void isAvailable_noWiFiConnection_availableIsFalse() {
        when(mWifiTracker.isConnected()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_noWiFiConnection_noPreferenceAdded() {
        when(mWifiTracker.isConnected()).thenReturn(false);
        when(mWifiTracker.getAccessPoints()).thenReturn(new ArrayList<>());
        mController.displayPreference(mScreen);
        verify(mPreferenceCategory, never()).addPreference(any());
    }

    @Test
    public void displayPreference_hasWiFiConnection_preferenceAdded() {
        when(mWifiTracker.isConnected()).thenReturn(true);
        final AccessPoint accessPoint = mock(AccessPoint.class);
        when(accessPoint.isActive()).thenReturn(true);
        when(mWifiTracker.getAccessPoints()).thenReturn(Arrays.asList(accessPoint));
        mController.displayPreference(mScreen);
        verify(mPreferenceCategory).addPreference(any(AccessPointPreference.class));
    }

    @Test
    public void onConnectedChanged_wifiBecameDisconnected_preferenceRemoved() {
        when(mWifiTracker.isConnected()).thenReturn(true);
        final AccessPoint accessPoint = mock(AccessPoint.class);

        when(accessPoint.isActive()).thenReturn(true);
        when(mWifiTracker.getAccessPoints()).thenReturn(Arrays.asList(accessPoint));
        mController.displayPreference(mScreen);
        final ArgumentCaptor<AccessPointPreference> captor = ArgumentCaptor.forClass(
                AccessPointPreference.class);
        verify(mPreferenceCategory).addPreference(captor.capture());
        final AccessPointPreference pref = captor.getValue();

        when(mWifiTracker.isConnected()).thenReturn(false);
        when(mWifiTracker.getAccessPoints()).thenReturn(new ArrayList<>());
        final int onUpdatedCountBefore = mOnChildUpdatedCount;
        mController.onConnectedChanged();
        verify(mPreferenceCategory).removePreference(pref);
        assertThat(mOnChildUpdatedCount).isEqualTo(onUpdatedCountBefore + 1);
    }


    @Test
    public void onAccessPointsChanged_wifiBecameConnectedToDifferentAP_preferenceReplaced() {
        when(mWifiTracker.isConnected()).thenReturn(true);
        final AccessPoint accessPoint1 = mock(AccessPoint.class);

        when(accessPoint1.isActive()).thenReturn(true);
        when(mWifiTracker.getAccessPoints()).thenReturn(Arrays.asList(accessPoint1));
        mController.displayPreference(mScreen);
        final ArgumentCaptor<AccessPointPreference> captor = ArgumentCaptor.forClass(
                AccessPointPreference.class);


        final AccessPoint accessPoint2 = mock(AccessPoint.class);
        when(accessPoint1.isActive()).thenReturn(false);
        when(accessPoint2.isActive()).thenReturn(true);
        when(mWifiTracker.getAccessPoints()).thenReturn(Arrays.asList(accessPoint1, accessPoint2));
        final int onUpdatedCountBefore = mOnChildUpdatedCount;
        mController.onAccessPointsChanged();

        verify(mPreferenceCategory, times(2)).addPreference(captor.capture());
        final AccessPointPreference pref1 = captor.getAllValues().get(0);
        final AccessPointPreference pref2 = captor.getAllValues().get(1);
        assertThat(pref1.getAccessPoint()).isEqualTo(accessPoint1);
        assertThat(pref2.getAccessPoint()).isEqualTo(accessPoint2);
        verify(mPreferenceCategory).removePreference(eq(pref1));
        assertThat(mOnChildUpdatedCount).isEqualTo(onUpdatedCountBefore + 1);
    }
}
