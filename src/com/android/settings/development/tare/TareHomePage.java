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
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

/** Class for creating the TARE homepage in settings */
public class TareHomePage extends Activity {
    private Switch mOnSwitch;
    private Button mRevButton;
    private TextView mAlarmManagerView;
    private TextView mJobSchedulerView;
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

        final boolean isTareEnabled = Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_TARE, Settings.Global.DEFAULT_ENABLE_TARE) == 1;
        setEnabled(isTareEnabled);

        mOnSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setEnabled(isChecked);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.ENABLE_TARE,
                        isChecked ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
            }
        });
    }

    /** Reverts the TARE settings to the original default settings */
    // TODO: Establish default TARE values and make this method revert all settings back to default.
    public void revertSettings(View v) {
        Toast.makeText(this, R.string.tare_settings_reverted_toast, Toast.LENGTH_LONG).show();
        Settings.Global.putString(getApplicationContext().getContentResolver(),
                Settings.Global.ENABLE_TARE, null);
        setEnabled(Settings.Global.DEFAULT_ENABLE_TARE == SETTING_VALUE_ON);
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
        mRevButton.setEnabled(tareStatus);
        mAlarmManagerView.setEnabled(tareStatus);
        mJobSchedulerView.setEnabled(tareStatus);
        mOnSwitch.setChecked(tareStatus);
    }
}
