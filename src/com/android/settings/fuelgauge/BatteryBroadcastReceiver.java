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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import com.android.settings.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use this broadcastReceiver to listen to the battery change and it will invoke {@link
 * OnBatteryChangedListener}
 */
public class BatteryBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BatteryBroadcastRcvr";

    /**
     * Callback if any of the monitored fields has been changed: <br>
     * <br>
     * Battery level(e.g. 100%->99%) Battery status(e.g. plugged->unplugged) <br>
     * Battery saver(e.g.off->on) <br>
     * Battery health(e.g. good->overheat) <br>
     * Battery charging status(e.g. default->long life)
     */
    public interface OnBatteryChangedListener {
        void onBatteryChanged(@BatteryUpdateType int type);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        BatteryUpdateType.MANUAL,
        BatteryUpdateType.BATTERY_LEVEL,
        BatteryUpdateType.BATTERY_SAVER,
        BatteryUpdateType.BATTERY_STATUS,
        BatteryUpdateType.BATTERY_HEALTH,
        BatteryUpdateType.CHARGING_STATUS,
        BatteryUpdateType.BATTERY_NOT_PRESENT
    })
    public @interface BatteryUpdateType {
        int MANUAL = 0;
        int BATTERY_LEVEL = 1;
        int BATTERY_SAVER = 2;
        int BATTERY_STATUS = 3;
        int BATTERY_HEALTH = 4;
        int CHARGING_STATUS = 5;
        int BATTERY_NOT_PRESENT = 6;
    }

    @VisibleForTesting String mBatteryLevel;
    @VisibleForTesting String mBatteryStatus;
    @VisibleForTesting int mChargingStatus;
    @VisibleForTesting int mBatteryHealth;
    private OnBatteryChangedListener mBatteryListener;
    private Context mContext;

    public BatteryBroadcastReceiver(Context context) {
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateBatteryStatus(intent, false /* forceUpdate */);
    }

    public void setBatteryChangedListener(OnBatteryChangedListener lsn) {
        mBatteryListener = lsn;
    }

    public void register() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        intentFilter.addAction(BatteryUtils.BYPASS_DOCK_DEFENDER_ACTION);
        intentFilter.addAction(UsbManager.ACTION_USB_PORT_COMPLIANCE_CHANGED);

        final Intent intent =
                mContext.registerReceiver(this, intentFilter, Context.RECEIVER_EXPORTED);
        updateBatteryStatus(intent, true /* forceUpdate */);
    }

    public void unRegister() {
        mContext.unregisterReceiver(this);
    }

    private void updateBatteryStatus(Intent intent, boolean forceUpdate) {
        if (intent == null || mBatteryListener == null) {
            return;
        }
        final String action = intent.getAction();
        Log.d(TAG, "updateBatteryStatus: action=" + action);
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            final String batteryLevel = Utils.getBatteryPercentage(intent);
            final String batteryStatus =
                    Utils.getBatteryStatus(mContext, intent, /* compactStatus= */ false);
            final int chargingStatus =
                    intent.getIntExtra(
                            BatteryManager.EXTRA_CHARGING_STATUS,
                            BatteryManager.CHARGING_POLICY_DEFAULT);
            final int batteryHealth =
                    intent.getIntExtra(
                            BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
            Log.d(
                    TAG,
                    "Battery changed: level: "
                            + batteryLevel
                            + "| status: "
                            + batteryStatus
                            + "| chargingStatus: "
                            + chargingStatus
                            + "| health: "
                            + batteryHealth);
            if (!Utils.isBatteryPresent(intent)) {
                Log.w(TAG, "Problem reading the battery meter.");
                mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_NOT_PRESENT);
            } else if (forceUpdate) {
                mBatteryListener.onBatteryChanged(BatteryUpdateType.MANUAL);
            } else if (chargingStatus != mChargingStatus) {
                mBatteryListener.onBatteryChanged(BatteryUpdateType.CHARGING_STATUS);
            } else if (batteryHealth != mBatteryHealth) {
                mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_HEALTH);
            } else if (!batteryLevel.equals(mBatteryLevel)) {
                mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_LEVEL);
            } else if (!batteryStatus.equals(mBatteryStatus)) {
                mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_STATUS);
            }
            mBatteryLevel = batteryLevel;
            mBatteryStatus = batteryStatus;
            mChargingStatus = chargingStatus;
            mBatteryHealth = batteryHealth;
        } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(action)) {
            mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_SAVER);
        } else if (BatteryUtils.BYPASS_DOCK_DEFENDER_ACTION.equals(action)
                || UsbManager.ACTION_USB_PORT_COMPLIANCE_CHANGED.equals(action)) {
            mBatteryListener.onBatteryChanged(BatteryUpdateType.BATTERY_STATUS);
        }
    }
}
