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
package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowApplication.Wrapper;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class TetherServiceTest {

    @Mock
    private Context mContext;

    private Context mAppContext;
    private TetherService mService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppContext = RuntimeEnvironment.application;
        mService = new TetherService();
        ReflectionHelpers.setField(mService, "mBase", mAppContext);
        mService.setHotspotOffReceiver(new HotspotOffReceiver(mContext));
    }

    @Test
    public void scheduleAlarm_shouldRegisterReceiver() {
        mService.setHotspotOffReceiver(new HotspotOffReceiver(mAppContext));

        mService.scheduleAlarm();

        boolean found = false;
        for (Wrapper wrapper : ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (wrapper.intentFilter.matchAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                found = true;
                break;
            }
        }

        assertThat(found).isTrue();
    }

    @Test
    public void cancelAlarmIfNecessary_hasActiveTethers_shouldNotUnregisterReceiver() {
        mService.scheduleAlarm();
        final ArrayList<Integer> tethers = new ArrayList<>();
        tethers.add(1);
        ReflectionHelpers.setField(mService, "mCurrentTethers", tethers);

        mService.cancelAlarmIfNecessary();
        verify(mContext, never()).unregisterReceiver(any(HotspotOffReceiver.class));
    }

    @Test
    public void cancelAlarmIfNecessary_noActiveTethers_shouldUnregisterReceiver() {
        final ArrayList<Integer> tethers = new ArrayList<>();
        ReflectionHelpers.setField(mService, "mCurrentTethers", tethers);
        mService.scheduleAlarm();

        mService.cancelAlarmIfNecessary();
        verify(mContext).unregisterReceiver(any(HotspotOffReceiver.class));
    }

    @Test
    public void onDestroy_shouldUnregisterReceiver() {
        final ArrayList<Integer> tethers = new ArrayList<>();
        ReflectionHelpers.setField(mService, "mCurrentTethers", tethers);
        ReflectionHelpers.setField(mService, "mBase", mContext);
        final SharedPreferences prefs = mock(SharedPreferences .class);
        final SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        when(prefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        final HotspotOffReceiver hotspotOffReceiver = mock(HotspotOffReceiver.class);
        mService.setHotspotOffReceiver(hotspotOffReceiver);

        mService.onDestroy();

        verify(hotspotOffReceiver).unregister();
    }
}
