/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.anomaly.tester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.settings.anomaly.tester.service.AnomalyService;
import com.android.settings.anomaly.tester.utils.AnomalyActions;
import com.android.settings.anomaly.tester.utils.AnomalyPolicyBuilder;

/**
 * Main activity to control and start anomaly
 */
public class AnomalyActivity extends Activity {
    private static final String TAG = AnomalyActivity.class.getSimpleName();

    public static final String KEY_TARGET_BUTTON = "target_button";

    private AnomalyResultReceiver mResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultReceiver = new AnomalyResultReceiver(new Handler());
    }

    public void startBluetoothAnomaly(View view) {
        try {
            // Enable anomaly detection and change the threshold
            final String config = new AnomalyPolicyBuilder()
                    .addPolicy(AnomalyPolicyBuilder.KEY_ANOMALY_DETECTION_ENABLED, true)
                    .addPolicy(AnomalyPolicyBuilder.KEY_BLUETOOTH_SCAN_DETECTION_ENABLED, true)
                    .addPolicy(AnomalyPolicyBuilder.KEY_BLUETOOTH_SCAN_THRESHOLD,
                            getValueFromEditText(R.id.bluetooth_threshold))
                    .build();
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.ANOMALY_DETECTION_CONSTANTS, config);

            // Start the anomaly service
            Intent intent = new Intent(this, AnomalyService.class);
            intent.putExtra(AnomalyActions.KEY_ACTION, AnomalyActions.ACTION_BLE_SCAN_UNOPTIMIZED);
            intent.putExtra(AnomalyActions.KEY_DURATION_MS,
                    getValueFromEditText(R.id.bluetooth_run_time));
            intent.putExtra(AnomalyActions.KEY_RESULT_RECEIVER, mResultReceiver);
            intent.putExtra(KEY_TARGET_BUTTON, view.getId());
            startService(intent);

            view.setEnabled(false);
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void startWakelockAnomaly(View view) {
        try {
            // Enable anomaly detection and change the threshold
            final String config = new AnomalyPolicyBuilder()
                    .addPolicy(AnomalyPolicyBuilder.KEY_ANOMALY_DETECTION_ENABLED, true)
                    .addPolicy(AnomalyPolicyBuilder.KEY_WAKELOCK_DETECTION_ENABLED, true)
                    .addPolicy(AnomalyPolicyBuilder.KEY_WAKELOCK_THRESHOLD,
                            getValueFromEditText(R.id.wakelock_threshold))
                    .build();
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.ANOMALY_DETECTION_CONSTANTS,
                    config);

            // Start the anomaly service
            Intent intent = new Intent(this, AnomalyService.class);
            intent.putExtra(AnomalyActions.KEY_ACTION, AnomalyActions.ACTION_WAKE_LOCK);
            intent.putExtra(AnomalyActions.KEY_DURATION_MS,
                    getValueFromEditText(R.id.wakelock_run_time));
            intent.putExtra(AnomalyActions.KEY_RESULT_RECEIVER, mResultReceiver);
            intent.putExtra(KEY_TARGET_BUTTON, view.getId());
            startService(intent);

            view.setEnabled(false);
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private long getValueFromEditText(final int id) throws NumberFormatException {
        final EditText editText = findViewById(id);
        if (editText != null) {
            final long value = Long.parseLong(editText.getText().toString());
            if (value > 0) {
                return value;
            }
        }

        throw new NumberFormatException("Number should be positive");
    }

    private class AnomalyResultReceiver extends ResultReceiver {

        public AnomalyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            final Button button = findViewById(resultData.getInt(KEY_TARGET_BUTTON));
            if (button != null) {
                button.setEnabled(true);
            }

        }
    }
}
