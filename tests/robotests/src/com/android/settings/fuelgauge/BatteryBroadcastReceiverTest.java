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

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.PowerManager;

import com.android.settings.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryBroadcastReceiverTest {

    private static final String BATTERY_INIT_LEVEL = "100%";
    private static final String BATTERY_INIT_STATUS = "Not charging";
    private static final int BATTERY_INTENT_LEVEL = 80;
    private static final int BATTERY_INTENT_SCALE = 100;

    @Mock private BatteryBroadcastReceiver.OnBatteryChangedListener mBatteryListener;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    private Context mContext;
    private Intent mChargingIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
        mBatteryBroadcastReceiver.mBatteryLevel = BATTERY_INIT_LEVEL;
        mBatteryBroadcastReceiver.mBatteryStatus = BATTERY_INIT_STATUS;
        mBatteryBroadcastReceiver.mBatteryHealth = BatteryManager.BATTERY_HEALTH_UNKNOWN;
        mBatteryBroadcastReceiver.mChargingStatus = BatteryManager.CHARGING_POLICY_DEFAULT;
        mBatteryBroadcastReceiver.setBatteryChangedListener(mBatteryListener);

        mChargingIntent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        mChargingIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_INTENT_LEVEL);
        mChargingIntent.putExtra(BatteryManager.EXTRA_SCALE, BATTERY_INTENT_SCALE);
        mChargingIntent.putExtra(
                BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING);
    }

    @Test
    public void onReceive_batteryLevelChanged_dataUpdated() {
        mBatteryBroadcastReceiver.onReceive(mContext, mChargingIntent);

        assertThat(mBatteryBroadcastReceiver.mBatteryLevel)
                .isEqualTo(Utils.getBatteryPercentage(mChargingIntent));
        assertThat(mBatteryBroadcastReceiver.mBatteryStatus)
                .isEqualTo(
                        Utils.getBatteryStatus(
                                mContext, mChargingIntent, /* compactStatus= */ false));
        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_LEVEL);
    }

    @Test
    public void onReceive_batteryHealthChanged_dataUpdated() {
        mChargingIntent.putExtra(
                BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_OVERHEAT);
        mBatteryBroadcastReceiver.onReceive(mContext, mChargingIntent);

        assertThat(mBatteryBroadcastReceiver.mBatteryHealth)
                .isEqualTo(BatteryManager.BATTERY_HEALTH_OVERHEAT);
        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_HEALTH);
    }

    @Test
    public void onReceive_chargingStatusChanged_dataUpdated() {
        mChargingIntent.putExtra(
                BatteryManager.EXTRA_CHARGING_STATUS,
                BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE);
        mBatteryBroadcastReceiver.onReceive(mContext, mChargingIntent);

        assertThat(mBatteryBroadcastReceiver.mChargingStatus)
                .isEqualTo(BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE);
        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.CHARGING_STATUS);
    }

    @Test
    public void onReceive_batteryNotPresent_shouldShowHelpMessage() {
        mChargingIntent.putExtra(BatteryManager.EXTRA_PRESENT, false);

        mBatteryBroadcastReceiver.onReceive(mContext, mChargingIntent);

        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_NOT_PRESENT);
    }

    @Test
    public void onReceive_powerSaveModeChanged_listenerInvoked() {
        mBatteryBroadcastReceiver.onReceive(
                mContext, new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));

        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_SAVER);
    }

    @Test
    public void onReceive_batteryDataNotChanged_listenerNotInvoked() {
        final String batteryLevel = Utils.getBatteryPercentage(mChargingIntent);
        final String batteryStatus =
                Utils.getBatteryStatus(mContext, mChargingIntent, /* compactStatus= */ false);
        mBatteryBroadcastReceiver.mBatteryLevel = batteryLevel;
        mBatteryBroadcastReceiver.mBatteryStatus = batteryStatus;

        mBatteryBroadcastReceiver.onReceive(mContext, mChargingIntent);

        assertThat(mBatteryBroadcastReceiver.mBatteryLevel).isEqualTo(batteryLevel);
        assertThat(mBatteryBroadcastReceiver.mBatteryStatus).isEqualTo(batteryStatus);
        assertThat(mBatteryBroadcastReceiver.mBatteryHealth)
                .isEqualTo(BatteryManager.BATTERY_HEALTH_UNKNOWN);
        assertThat(mBatteryBroadcastReceiver.mChargingStatus)
                .isEqualTo(BatteryManager.CHARGING_POLICY_DEFAULT);
        verify(mBatteryListener, never()).onBatteryChanged(anyInt());
    }

    @Test
    public void onReceive_dockDefenderBypassed_listenerInvoked() {
        mBatteryBroadcastReceiver.onReceive(
                mContext, new Intent(BatteryUtils.BYPASS_DOCK_DEFENDER_ACTION));

        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_STATUS);
    }

    @Test
    public void onReceive_usbPortComplianceChanged_listenerInvoked() {
        mBatteryBroadcastReceiver.onReceive(
                mContext, new Intent(UsbManager.ACTION_USB_PORT_COMPLIANCE_CHANGED));

        verify(mBatteryListener).onBatteryChanged(BatteryUpdateType.BATTERY_STATUS);
    }

    @Test
    public void register_updateBatteryStatus() {
        doReturn(mChargingIntent).when(mContext).registerReceiver(any(), any(), anyInt());

        mBatteryBroadcastReceiver.register();
        mBatteryBroadcastReceiver.register();

        assertThat(mBatteryBroadcastReceiver.mBatteryLevel)
                .isEqualTo(Utils.getBatteryPercentage(mChargingIntent));
        assertThat(mBatteryBroadcastReceiver.mBatteryStatus)
                .isEqualTo(
                        Utils.getBatteryStatus(
                                mContext, mChargingIntent, /* compactStatus= */ false));
        assertThat(mBatteryBroadcastReceiver.mBatteryHealth)
                .isEqualTo(BatteryManager.BATTERY_HEALTH_UNKNOWN);
        assertThat(mBatteryBroadcastReceiver.mChargingStatus)
                .isEqualTo(BatteryManager.CHARGING_POLICY_DEFAULT);
        // 2 times because register will force update the battery
        verify(mBatteryListener, times(2)).onBatteryChanged(BatteryUpdateType.MANUAL);
    }

    @Test
    public void register_registerExpectedIntent() {
        mBatteryBroadcastReceiver.register();

        ArgumentCaptor<IntentFilter> captor = ArgumentCaptor.forClass(IntentFilter.class);
        verify(mContext)
                .registerReceiver(
                        eq(mBatteryBroadcastReceiver),
                        captor.capture(),
                        eq(Context.RECEIVER_EXPORTED));
        assertAction(captor, Intent.ACTION_BATTERY_CHANGED);
        assertAction(captor, PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        assertAction(captor, BatteryUtils.BYPASS_DOCK_DEFENDER_ACTION);
        assertAction(captor, UsbManager.ACTION_USB_PORT_COMPLIANCE_CHANGED);
    }

    private void assertAction(ArgumentCaptor<IntentFilter> captor, String action) {
        assertThat(captor.getValue().hasAction(action)).isTrue();
    }
}
