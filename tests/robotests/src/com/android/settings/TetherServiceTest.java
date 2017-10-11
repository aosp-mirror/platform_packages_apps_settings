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
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TetherServiceTest {

    @Mock
    private Context mContext;

    private ShadowApplication mShadowApplication;
    private Context mAppContext;
    private TetherService mService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowApplication = ShadowApplication.getInstance();
        mAppContext = mShadowApplication.getApplicationContext();
        mService = new TetherService();
        ReflectionHelpers.setField(mService, "mBase", mAppContext);
        mService.setHotspotOffReceiver(new HotspotOffReceiver(mContext));
    }

    @Test
    public void scheduleAlarm_shouldRegisterReceiver() {
        mService.setHotspotOffReceiver(new HotspotOffReceiver(mAppContext));

        mService.scheduleAlarm();

        assertThat(mShadowApplication.hasReceiverForIntent(
            new Intent(WifiManager.WIFI_AP_STATE_CHANGED_ACTION))).isTrue();
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
}
