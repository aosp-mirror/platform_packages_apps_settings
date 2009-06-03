/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;

public class PowerUsageDetail extends Activity {

    public static final int USAGE_SINCE_UNPLUGGED = 1;
    public static final int USAGE_SINCE_RESET = 2;

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_USAGE_SINCE = "since";
    public static final String EXTRA_USAGE_DURATION = "duration";
    public static final String EXTRA_DETAIL_TYPES = "types";
    public static final String EXTRA_DETAIL_VALUES = "values";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private static final boolean DEBUG = true;
    private String mTitle;
    private double mPercentage;
    private int mUsageSince;
    private int[] mTypes;
    private double[] mValues;
    private TextView mTitleView;
    private ViewGroup mDetailsParent;
    private long mStartTime;

    private static final String TAG = "PowerUsageDetail";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.power_usage_details);
        createDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStartTime = android.os.Process.getElapsedCpuTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void createDetails() {
        final Intent intent = getIntent();
        mTitle = intent.getStringExtra(EXTRA_TITLE);
        mPercentage = intent.getDoubleExtra(EXTRA_PERCENT, -1);
        mUsageSince = intent.getIntExtra(EXTRA_USAGE_SINCE, USAGE_SINCE_UNPLUGGED);

        mTypes = intent.getIntArrayExtra(EXTRA_DETAIL_TYPES);
        mValues = intent.getDoubleArrayExtra(EXTRA_DETAIL_VALUES);

        mTitleView = (TextView) findViewById(R.id.name);
        mTitleView.setText(mTitle);
        // TODO: I18N
        ((TextView)findViewById(R.id.battery_percentage))
            .setText(String.format("%3.2f%% of battery usage since last unplugged", mPercentage));

        mDetailsParent = (ViewGroup) findViewById(R.id.details);
        LayoutInflater inflater = getLayoutInflater();
        if (mTypes != null && mValues != null) {
            for (int i = 0; i < mTypes.length; i++) {
                // Only add an item if the time is greater than zero
                if (mValues[i] <= 0) continue;
                final String label = getString(mTypes[i]);
                String value = null;
                switch (mTypes[i]) {
                    case R.string.usage_type_data_recv:
                    case R.string.usage_type_data_send:
                        value = formatBytes(mValues[i]);
                        break;
                    default:
                        value = formatTime(mValues[i]);
                }
                ViewGroup item = (ViewGroup) inflater.inflate(R.layout.power_usage_detail_item_text,
                        null);
                mDetailsParent.addView(item);
                TextView labelView = (TextView) item.findViewById(R.id.label);
                TextView valueView = (TextView) item.findViewById(R.id.value);
                labelView.setText(label);
                valueView.setText(value);
            }
        }
    }

    private String formatTime(double millis) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000);

        int days = 0, hours = 0, minutes = 0;
        if (seconds > SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds > SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds > SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        if (days > 0) {
            sb.append(getString(R.string.battery_history_days, days, hours, minutes, seconds));
        } else if (hours > 0) {
            sb.append(getString(R.string.battery_history_hours, hours, minutes, seconds));
        } else if (minutes > 0) {
            sb.append(getString(R.string.battery_history_minutes, minutes, seconds));
        } else {
            sb.append(getString(R.string.battery_history_seconds, seconds));
        }
        return sb.toString();
    }

    private String formatBytes(double bytes) {
        // TODO: I18N
        if (bytes > 1000 * 1000) {
            return String.format("%.2f MB", ((int) (bytes / 1000)) / 1000f);
        } else if (bytes > 1024) {
            return String.format("%.2f KB", ((int) (bytes / 10)) / 100f);
        } else {
            return String.format("%d bytes", (int) bytes);
        }
    }
}
