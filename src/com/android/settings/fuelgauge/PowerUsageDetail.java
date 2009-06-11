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
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.InstalledAppDetails;
import com.android.settings.R;

public class PowerUsageDetail extends Activity implements Button.OnClickListener {

    enum DrainType {
        IDLE,
        CELL,
        PHONE,
        WIFI,
        BLUETOOTH,
        SCREEN,
        APP
    }

    public static final int ACTION_DISPLAY_SETTINGS = 1;
    public static final int ACTION_WIFI_SETTINGS = 2;
    public static final int ACTION_BLUETOOTH_SETTINGS = 3;
    public static final int ACTION_FORCE_STOP = 4;
    public static final int ACTION_UNINSTALL = 5;

    public static final int USAGE_SINCE_UNPLUGGED = 1;
    public static final int USAGE_SINCE_RESET = 2;

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_USAGE_SINCE = "since";
    public static final String EXTRA_USAGE_DURATION = "duration";
    public static final String EXTRA_DETAIL_TYPES = "types";
    public static final String EXTRA_DETAIL_VALUES = "values";
    public static final String EXTRA_DRAIN_TYPE = "drainType";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private static final boolean DEBUG = true;
    private String mTitle;
    private double mPercentage;
    private int mUsageSince;
    private int[] mTypes;
    private int mUid;
    private double[] mValues;
    private TextView mTitleView;
    private ViewGroup mDetailsParent;
    private long mStartTime;
    private DrainType mDrainType;
    private int mAction1;
    private int mAction2;

    private static final String TAG = "PowerUsageDetail";
    private Button mButton1;
    private Button mButton2;
    private String[] mPackages;

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
        mUid = intent.getIntExtra(EXTRA_UID, 0);
        mDrainType = (DrainType) intent.getSerializableExtra(EXTRA_DRAIN_TYPE);

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

        fillPackagesSection(mUid);
        fillControlsSection(mUid);
    }

    public void onClick(View v) {
        int action = v == mButton1 ? mAction1 : mAction2;
        doAction(action);
    }

    private void doAction(int action) {
        switch (action) {
            case ACTION_DISPLAY_SETTINGS:
                startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
                break;
            case ACTION_WIFI_SETTINGS:
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
            case ACTION_BLUETOOTH_SETTINGS:
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                break;
            case ACTION_FORCE_STOP:
                killProcesses();
                break;
            case ACTION_UNINSTALL:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(this, InstalledAppDetails.class);
                intent.putExtra("com.android.settings.ApplicationPkgName", mPackages[0]);
                startActivity(intent);
                break;
        }
    }

    private void fillControlsSection(int uid) {
        String label1 = null;
        String label2 = null;
        mAction1 = 0;
        mAction2 = 0;
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(mUid);
        PackageInfo pi = null;
        try {
            pi = packages != null ? pm.getPackageInfo(packages[0], 0) : null;
        } catch (NameNotFoundException nnfe) { /* Nothing */ }
        ApplicationInfo ai = pi != null? pi.applicationInfo : null;
        boolean isSystem = ai != null? (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 : false;

        if (uid == 0 || !isSystem) { 
            switch (mDrainType) {
                case APP:
                    label1 = getString(R.string.battery_action_stop);
                    label2 = getString(R.string.battery_action_app_details);
                    mAction1 = ACTION_FORCE_STOP;
                    mAction2 = ACTION_UNINSTALL;
                    break;
                case SCREEN:
                    //label2 = getString(R.string.battery_action_display);
                    //mAction2 = ACTION_DISPLAY_SETTINGS;
                    break;
                case WIFI:
                    label2 = getString(R.string.battery_action_wifi);
                    mAction2 = ACTION_WIFI_SETTINGS;
                    break;
                case BLUETOOTH:
                    //label2 = getString(R.string.battery_action_bluetooth);
                    //mAction2 = ACTION_BLUETOOTH_SETTINGS;
                    break;
            }
        }
        mButton1 = (Button) findViewById(R.id.action_button1);
        mButton2 = (Button) findViewById(R.id.action_button2);
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        if (label1 == null) {
            mButton1.setVisibility(View.GONE);
        } else {
            mButton1.setText(label1);
        }
        if (label2 == null) {
            findViewById(R.id.controls_section).setVisibility(View.GONE);
        } else {
            mButton2.setText(label2);
        }
    }

    private void removePackagesSection() {
        View view;
        if ((view = findViewById(R.id.packages_section_title)) != null) {
            view.setVisibility(View.GONE);
        }
        if ((view = findViewById(R.id.packages_section)) != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void killProcesses() {
        if (mPackages == null) return;
        ActivityManager am = (ActivityManager)getSystemService(
                Context.ACTIVITY_SERVICE);
        for (int i = 0; i < mPackages.length; i++) {
            am.restartPackage(mPackages[i]);
        }
    }

    private void fillPackagesSection(int uid) {
        if (uid == 0) {
            removePackagesSection();
            return;
        }
        ViewGroup packagesParent = (ViewGroup) findViewById(R.id.packages_section);
        if (packagesParent == null) return;
        LayoutInflater inflater = getLayoutInflater();
        
        PackageManager pm = getPackageManager();
        final Drawable defaultActivityIcon = pm.getDefaultActivityIcon();
        mPackages = pm.getPackagesForUid(uid);
        if (mPackages == null || mPackages.length < 2) {
            removePackagesSection();
            return;
        }

        // Convert package names to user-facing labels where possible
        for (int i = 0; i < mPackages.length; i++) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(mPackages[i], 0);
                CharSequence label = ai.loadLabel(pm);
                Drawable icon = defaultActivityIcon;
                if (label != null) {
                    mPackages[i] = label.toString();
                }
                if (ai.icon != 0) {
                    icon = ai.loadIcon(pm);
                }
                ViewGroup item = (ViewGroup) inflater.inflate(R.layout.power_usage_package_item,
                        null);
                packagesParent.addView(item);
                TextView labelView = (TextView) item.findViewById(R.id.label);
                labelView.setText(mPackages[i]);
            } catch (NameNotFoundException e) {
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
