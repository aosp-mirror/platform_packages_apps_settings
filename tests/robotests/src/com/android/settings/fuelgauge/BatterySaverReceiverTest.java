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

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.PowerManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverReceiverTest {

    @Mock private BatterySaverReceiver.BatterySaverListener mBatterySaverListener;
    @Mock private Context mContext;
    private BatterySaverReceiver mBatterySaverReceiver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBatterySaverReceiver = new BatterySaverReceiver(mContext);
        mBatterySaverReceiver.setBatterySaverListener(mBatterySaverListener);
    }

    @Test
    public void testOnReceive_devicePluggedIn_pluggedInTrue() {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC);

        mBatterySaverReceiver.onReceive(mContext, intent);

        verify(mBatterySaverListener).onBatteryChanged(true);
    }

    @Test
    public void testOnReceive_deviceNotPluggedIn_pluggedInFalse() {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);

        mBatterySaverReceiver.onReceive(mContext, intent);

        verify(mBatterySaverListener).onBatteryChanged(false);
    }

    @Test
    public void testOnReceive_powerSaveModeChanged_invokeCallback() {
        Intent intent = new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);

        mBatterySaverReceiver.onReceive(mContext, intent);

        verify(mBatterySaverListener).onPowerSaveModeChanged();
    }
}
