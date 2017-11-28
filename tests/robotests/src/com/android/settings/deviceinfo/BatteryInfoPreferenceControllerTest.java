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

package com.android.settings.deviceinfo;

import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.deviceinfo.BatteryInfoPreferenceController
        .BATTERY_INFO_RECEIVER_INTENT_FILTER;
import static com.android.settings.deviceinfo.BatteryInfoPreferenceController.KEY_BATTERY_LEVEL;
import static com.android.settings.deviceinfo.BatteryInfoPreferenceController.KEY_BATTERY_STATUS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class BatteryInfoPreferenceControllerTest {

    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;

    private Preference mBatteryLevel;
    private Preference mBatteryStatus;
    private Lifecycle mLifecycle;
    private BatteryInfoPreferenceController mController;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mController = new BatteryInfoPreferenceController(mContext, mLifecycle);
        mBatteryLevel = new Preference(mContext);
        mBatteryStatus = new Preference(mContext);
        when(mScreen.findPreference(KEY_BATTERY_STATUS)).thenReturn(mBatteryStatus);
        when(mScreen.findPreference(KEY_BATTERY_LEVEL)).thenReturn(mBatteryLevel);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.getPreferenceKey()).isNull();
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void runThroughLifecycle_shouldRegisterUnregisterBatteryInfoReceiver() {
        final Context context = mock(Context.class);
        mController = new BatteryInfoPreferenceController(context, mLifecycle);
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        verify(context).registerReceiver(mController.mBatteryInfoReceiver,
                BATTERY_INFO_RECEIVER_INTENT_FILTER);
        verify(context).unregisterReceiver(mController.mBatteryInfoReceiver);
    }

    @Test
    public void onReceiveBatteryInfoBroadcast_shouldUpdatePreferences() {
        mController.displayPreference(mScreen);
        final Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, 50);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING);

        mController.mBatteryInfoReceiver.onReceive(mContext, intent);

        assertThat(mBatteryLevel.getSummary()).isEqualTo("50%");
        assertThat(mBatteryStatus.getSummary())
                .isEqualTo(mContext.getText(R.string.battery_info_status_charging));
    }
}
