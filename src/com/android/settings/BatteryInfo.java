/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;

public class BatteryInfo extends Activity {
    private TextView mStatus;
    private TextView mPower;
    private TextView mLevel;
    private TextView mScale;
    private TextView mHealth;
    private TextView mVoltage;
    private TextView mTemperature;
    private TextView mTechnology;
    private TextView mUptime;
    private IBatteryStats mBatteryStats;
    private IPowerManager mScreenStats;
    
    private static final int EVENT_TICK = 1;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TICK:
                    updateBatteryStats();
                    sendEmptyMessageDelayed(EVENT_TICK, 1000);
                    
                    break;
            }
        }
    };

    /**
     * Format a number of tenths-units as a decimal string without using a
     * conversion to float.  E.g. 347 -> "34.7", -99 -> "-9.9"
     */
    private final String tenthsToFixedString(int x) {
        int tens = x / 10;
        // use Math.abs to avoid "-9.-9" about -99
        return Integer.toString(tens) + "." + Math.abs(x - 10 * tens);
    }

   /**
    *Listens for intent broadcasts
    */
    private IntentFilter   mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int plugType = intent.getIntExtra("plugged", 0);

                mLevel.setText("" + intent.getIntExtra("level", 0));
                mScale.setText("" + intent.getIntExtra("scale", 0));
                mVoltage.setText("" + intent.getIntExtra("voltage", 0) + " "
                        + getString(R.string.battery_info_voltage_units));
                mTemperature.setText("" + tenthsToFixedString(intent.getIntExtra("temperature", 0))
                        + getString(R.string.battery_info_temperature_units));
                mTechnology.setText("" + intent.getStringExtra("technology"));
                
                mStatus.setText(Utils.getBatteryStatus(getResources(), intent));

                switch (plugType) {
                    case 0:
                        mPower.setText(getString(R.string.battery_info_power_unplugged));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        mPower.setText(getString(R.string.battery_info_power_ac));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        mPower.setText(getString(R.string.battery_info_power_usb));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                        mPower.setText(getString(R.string.battery_info_power_wireless));
                        break;
                    case (BatteryManager.BATTERY_PLUGGED_AC|BatteryManager.BATTERY_PLUGGED_USB):
                        mPower.setText(getString(R.string.battery_info_power_ac_usb));
                        break;
                    default:
                        mPower.setText(getString(R.string.battery_info_power_unknown));
                        break;
                }
                
                int health = intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                String healthString;
                if (health == BatteryManager.BATTERY_HEALTH_GOOD) {
                    healthString = getString(R.string.battery_info_health_good);
                } else if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
                    healthString = getString(R.string.battery_info_health_overheat);
                } else if (health == BatteryManager.BATTERY_HEALTH_DEAD) {
                    healthString = getString(R.string.battery_info_health_dead);
                } else if (health == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE) {
                    healthString = getString(R.string.battery_info_health_over_voltage);
                } else if (health == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE) {
                    healthString = getString(R.string.battery_info_health_unspecified_failure);
                } else if (health == BatteryManager.BATTERY_HEALTH_COLD) {
                    healthString = getString(R.string.battery_info_health_cold);
                } else {
                    healthString = getString(R.string.battery_info_health_unknown);
                }
                mHealth.setText(healthString);
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.battery_info);

        // create the IntentFilter that will be used to listen
        // to battery status broadcasts
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    }

    @Override
    public void onResume() {
        super.onResume();

        mStatus = (TextView)findViewById(R.id.status);
        mPower = (TextView)findViewById(R.id.power);
        mLevel = (TextView)findViewById(R.id.level);
        mScale = (TextView)findViewById(R.id.scale);
        mHealth = (TextView)findViewById(R.id.health);
        mTechnology = (TextView)findViewById(R.id.technology);
        mVoltage = (TextView)findViewById(R.id.voltage);
        mTemperature = (TextView)findViewById(R.id.temperature);
        mUptime = (TextView) findViewById(R.id.uptime);
        
        // Get awake time plugged in and on battery
        mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batteryinfo"));
        mScreenStats = IPowerManager.Stub.asInterface(ServiceManager.getService(POWER_SERVICE));
        mHandler.sendEmptyMessageDelayed(EVENT_TICK, 1000);
        
        registerReceiver(mIntentReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeMessages(EVENT_TICK);
        
        // we are no longer on the screen stop the observers
        unregisterReceiver(mIntentReceiver);
    }

    private void updateBatteryStats() {
        long uptime = SystemClock.elapsedRealtime();
        mUptime.setText(DateUtils.formatElapsedTime(uptime / 1000));
    }
    
}
