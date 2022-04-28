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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsSpinnerAdapter;

/**
 * Dropdown activity to allow for the user to easily switch between the different TARE
 * policies in the developer options of settings. Depending on what is chosen, the fragment
 * containing that specific policies' factors will be generated.
 */
public class DropdownActivity extends Activity {

    private Fragment mAlarmManagerFragment;
    private Fragment mJobSchedulerFragment;
    private Spinner mSpinner;
    static final String EXTRA_POLICY = "policy";
    static final int POLICY_ALARM_MANAGER = 0;
    static final int POLICY_JOB_SCHEDULER = 1;
    private static final int DEFAULT_POLICY = POLICY_ALARM_MANAGER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tare_dropdown_page);

        // Determines what policy fragment to open up to
        Intent intent = getIntent();
        int policy = intent.getIntExtra(EXTRA_POLICY, DEFAULT_POLICY);

        mSpinner = findViewById(R.id.spinner);
        mAlarmManagerFragment = new AlarmManagerFragment();
        mJobSchedulerFragment = new JobSchedulerFragment();

        String[] policies = getResources().getStringArray(R.array.tare_policies);

        ArrayAdapter<String> arrayAdapter = new SettingsSpinnerAdapter<String>(this);
        arrayAdapter.addAll(policies);
        mSpinner.setAdapter(arrayAdapter);

        mSpinner.setSelection(policy);
        selectFragment(policy);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                    long id) {
                selectFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    /** Selects the correct policy fragment to display based on user selection */
    private void selectFragment(int policy) {
        switch (policy) {
            case POLICY_ALARM_MANAGER:
                openFragment(mAlarmManagerFragment);
                break;
            case POLICY_JOB_SCHEDULER:
                openFragment(mJobSchedulerFragment);
                break;
            default:
                openFragment(mAlarmManagerFragment);
        }
    }

    /** Opens the correct policy fragment */
    private void openFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}
