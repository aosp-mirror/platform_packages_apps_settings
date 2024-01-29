/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.development.tare;

import static com.android.settings.development.tare.DropdownActivity.EXTRA_POLICY;
import static com.android.settings.development.tare.DropdownActivity.POLICY_ALARM_MANAGER;
import static com.android.settings.development.tare.DropdownActivity.POLICY_JOB_SCHEDULER;

import android.app.Activity;
import android.app.tare.EconomyManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

/** Class for creating the TARE homepage in settings */
public class TareHomePage extends Activity {
    private static final String TAG = "TareHomePage";

    private CompoundButton mOnSwitch;
    private Button mRevButton;
    private TextView mAlarmManagerView;
    private TextView mJobSchedulerView;
    private ConfigObserver mConfigObserver;

    private static final int SETTING_VALUE_DEFAULT = -1;
    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tare_homepage);

        mOnSwitch = findViewById(R.id.on_switch);
        mRevButton = findViewById(R.id.revert_button);
        mAlarmManagerView = findViewById(R.id.alarmmanager);
        mJobSchedulerView = findViewById(R.id.jobscheduler);

        mConfigObserver = new ConfigObserver(new Handler(Looper.getMainLooper()));

        mOnSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mConfigObserver.mEnableTareSetting == SETTING_VALUE_DEFAULT
                        && isChecked == mConfigObserver.getDefaultEnabledStatus()) {
                    // Don't bother writing something that's not new information. It would make
                    // it hard to use DeviceConfig if we did.
                    return;
                }
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.ENABLE_TARE,
                        isChecked ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConfigObserver.start();
    }

    @Override
    protected void onPause() {
        mConfigObserver.stop();
        super.onPause();
    }

    /** Reverts the TARE settings to the original default settings */
    public void revertSettings(View v) {
        Toast.makeText(this, R.string.tare_settings_reverted_toast, Toast.LENGTH_LONG).show();
        final boolean wasSettingsDefault =
                mConfigObserver.mEnableTareSetting == SETTING_VALUE_DEFAULT;
        Settings.Global.putString(getApplicationContext().getContentResolver(),
                Settings.Global.ENABLE_TARE, null);
        Settings.Global.putString(getApplicationContext().getContentResolver(),
                Settings.Global.TARE_ALARM_MANAGER_CONSTANTS, null);
        Settings.Global.putString(getApplicationContext().getContentResolver(),
                Settings.Global.TARE_JOB_SCHEDULER_CONSTANTS, null);
        if (wasSettingsDefault) {
            // Only do this manually here to force a DeviceConfig check if the settings value isn't
            // actually changing.
            setEnabled(mConfigObserver.getDefaultEnabledStatus());
        }
    }

    /** Opens up the AlarmManager TARE policy page with its factors to view and edit */
    public void launchAlarmManagerPage(View v) {
        Intent i = new Intent(getApplicationContext(), DropdownActivity.class);
        i.putExtra(EXTRA_POLICY, POLICY_ALARM_MANAGER);
        startActivity(i);
    }

    /** Opens up the JobScheduler TARE policy page with its factors to view and edit */
    public void launchJobSchedulerPage(View v) {
        Intent i = new Intent(getApplicationContext(), DropdownActivity.class);
        i.putExtra(EXTRA_POLICY, POLICY_JOB_SCHEDULER);
        startActivity(i);
    }

    /** Changes the enabled state of the TARE homepage buttons based on global toggle */
    private void setEnabled(boolean tareStatus) {
        mAlarmManagerView.setEnabled(tareStatus);
        mJobSchedulerView.setEnabled(tareStatus);
        mOnSwitch.setChecked(tareStatus);
    }

    private class ConfigObserver extends ContentObserver {
        private int mEnableTareSetting;

        ConfigObserver(Handler handler) {
            super(handler);
        }

        public void start() {
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ENABLE_TARE), false, this);
            processEnableTareChange();
        }

        public void stop() {
            getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            processEnableTareChange();
        }

        private void processEnableTareChange() {
            final String setting =
                    Settings.Global.getString(getContentResolver(), Settings.Global.ENABLE_TARE);
            if (setting == null) {
                mEnableTareSetting = SETTING_VALUE_DEFAULT;
            } else {
                try {
                    mEnableTareSetting = Integer.parseInt(setting);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid setting value", e);
                    mEnableTareSetting = EconomyManager.DEFAULT_ENABLE_TARE_MODE;
                }
            }
            final boolean enabled;
            if (mEnableTareSetting == SETTING_VALUE_ON) {
                enabled = true;
            } else if (mEnableTareSetting == SETTING_VALUE_OFF) {
                enabled = false;
            } else {
                enabled = getDefaultEnabledStatus();
            }
            setEnabled(enabled);
        }

        private boolean getDefaultEnabledStatus() {
            // Show Shadow Mode as "off" in the UI since it won't be affecting device behavior.
            return DeviceConfig.getInt(DeviceConfig.NAMESPACE_TARE,
                    EconomyManager.KEY_ENABLE_TARE_MODE,
                    EconomyManager.DEFAULT_ENABLE_TARE_MODE) == EconomyManager.ENABLED_MODE_ON;
        }
    }
}
