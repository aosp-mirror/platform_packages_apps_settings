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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    // Note: Must match the sequence of the DrainType
    private static int[] sDrainTypeDesciptions = new int[] {
        R.string.battery_desc_standby,
        R.string.battery_desc_radio,
        R.string.battery_desc_voice,
        R.string.battery_desc_wifi,
        R.string.battery_desc_bluetooth,
        R.string.battery_desc_display,
        R.string.battery_desc_apps
    };

    public static final int ACTION_DISPLAY_SETTINGS = 1;
    public static final int ACTION_WIFI_SETTINGS = 2;
    public static final int ACTION_BLUETOOTH_SETTINGS = 3;
    public static final int ACTION_WIRELESS_SETTINGS = 4;
    public static final int ACTION_APP_DETAILS = 5;
    public static final int ACTION_SECURITY_SETTINGS = 6;

    public static final int USAGE_SINCE_UNPLUGGED = 1;
    public static final int USAGE_SINCE_RESET = 2;

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_GAUGE = "gauge";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_USAGE_SINCE = "since";
    public static final String EXTRA_USAGE_DURATION = "duration";
    public static final String EXTRA_DETAIL_TYPES = "types"; // Array of usage types (cpu, gps, etc)
    public static final String EXTRA_DETAIL_VALUES = "values"; // Array of doubles
    public static final String EXTRA_DRAIN_TYPE = "drainType"; // DrainType
    public static final String EXTRA_ICON_PACKAGE = "iconPackage"; // String
    public static final String EXTRA_NO_COVERAGE = "noCoverage";
    public static final String EXTRA_ICON_ID = "iconId"; // Int

    private static final boolean DEBUG = true;
    private String mTitle;
    private int mUsageSince;
    private int[] mTypes;
    private int mUid;
    private double[] mValues;
    private TextView mTitleView;
    private ViewGroup mDetailsParent;
    private ViewGroup mControlsParent;
    private long mStartTime;
    private DrainType mDrainType;
    private PercentageBar mGauge;
    private Drawable mAppIcon;
    private double mNoCoverage; // Percentage of time that there was no coverage

    private boolean mUsesGps;

    private static final String TAG = "PowerUsageDetail";
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
        final int percentage = intent.getIntExtra(EXTRA_PERCENT, 1);
        final int gaugeValue = intent.getIntExtra(EXTRA_GAUGE, 1);
        mUsageSince = intent.getIntExtra(EXTRA_USAGE_SINCE, USAGE_SINCE_UNPLUGGED);
        mUid = intent.getIntExtra(EXTRA_UID, 0);
        mDrainType = (DrainType) intent.getSerializableExtra(EXTRA_DRAIN_TYPE);
        mNoCoverage = intent.getDoubleExtra(EXTRA_NO_COVERAGE, 0);
        String iconPackage = intent.getStringExtra(EXTRA_ICON_PACKAGE);
        int iconId = intent.getIntExtra(EXTRA_ICON_ID, 0);
        if (!TextUtils.isEmpty(iconPackage)) {
            try {
                final PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getPackageInfo(iconPackage, 0).applicationInfo;
                if (ai != null) {
                    mAppIcon = ai.loadIcon(pm);
                }
            } catch (NameNotFoundException nnfe) {
                // Use default icon
            }
        } else if (iconId != 0) {
            mAppIcon = getResources().getDrawable(iconId);
        }
        if (mAppIcon == null) {
            mAppIcon = getPackageManager().getDefaultActivityIcon();
        }

        // Set the description
        String summary = getDescriptionForDrainType();
        ((TextView)findViewById(R.id.summary)).setText(summary);
        
        mTypes = intent.getIntArrayExtra(EXTRA_DETAIL_TYPES);
        mValues = intent.getDoubleArrayExtra(EXTRA_DETAIL_VALUES);

        mTitleView = (TextView) findViewById(R.id.name);
        mTitleView.setText(mTitle);
        ((TextView)findViewById(R.id.battery_percentage))
            .setText(String.format("%d%%", percentage));

        ImageView gaugeImage = (ImageView) findViewById(R.id.gauge);
        mGauge = new PercentageBar();
        mGauge.percent = gaugeValue;
        mGauge.bar = getResources().getDrawable(R.drawable.app_gauge);
        gaugeImage.setImageDrawable(mGauge);

        ImageView iconImage = (ImageView) findViewById(R.id.icon);
        iconImage.setImageDrawable(mAppIcon);

        mDetailsParent = (ViewGroup) findViewById(R.id.details);
        mControlsParent = (ViewGroup) findViewById(R.id.controls);

        fillDetailsSection();
        fillPackagesSection(mUid);
        fillControlsSection(mUid);
    }

    public void onClick(View v) {
        doAction((Integer) v.getTag());
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
            case ACTION_WIRELESS_SETTINGS:
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                break;
            case ACTION_APP_DETAILS:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(this, InstalledAppDetails.class);
                intent.putExtra("com.android.settings.ApplicationPkgName", mPackages[0]);
                startActivity(intent);
                break;
            case ACTION_SECURITY_SETTINGS:
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                break;
        }
    }

    private void fillDetailsSection() {
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
                        value = Utils.formatBytes(this, mValues[i]);
                        break;
                    case R.string.usage_type_no_coverage:
                        value = String.format("%d%%", (int) Math.floor(mValues[i]));
                        break;
                    case R.string.usage_type_gps:
                        mUsesGps = true;
                        // Fall through
                    default:
                        value = Utils.formatElapsedTime(this, mValues[i]);
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

    private void fillControlsSection(int uid) {
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        PackageInfo pi = null;
        try {
            pi = packages != null ? pm.getPackageInfo(packages[0], 0) : null;
        } catch (NameNotFoundException nnfe) { /* Nothing */ }
        ApplicationInfo ai = pi != null? pi.applicationInfo : null;
        boolean isSystem = ai != null? (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 : false;

        boolean removeHeader = true;
        switch (mDrainType) {
            case APP:
                // If it is a Java application and it's not a system application
                if (packages != null && !isSystem) {
                    addControl(R.string.battery_action_app_details,
                            R.string.battery_sugg_apps_info, ACTION_APP_DETAILS);
                    removeHeader = false;
                    // If the application has a settings screen, jump to  that
                    // TODO:
                }
                if (mUsesGps) {
                    addControl(R.string.security_settings_title,
                            R.string.battery_sugg_apps_gps, ACTION_SECURITY_SETTINGS);
                    removeHeader = false;
                }
                break;
            case SCREEN:
                addControl(R.string.sound_and_display_settings,
                        R.string.battery_sugg_display,
                        ACTION_DISPLAY_SETTINGS);
                removeHeader = false;
                break;
            case WIFI:
                addControl(R.string.wifi_settings,
                        R.string.battery_sugg_wifi,
                        ACTION_WIFI_SETTINGS);
                removeHeader = false;
                break;
            case BLUETOOTH:
                addControl(R.string.bluetooth_settings,
                        R.string.battery_sugg_bluetooth_basic,
                        ACTION_BLUETOOTH_SETTINGS);
                removeHeader = false;
                break;
            case CELL:
                if (mNoCoverage > 10) {
                    addControl(R.string.radio_controls_title,
                            R.string.battery_sugg_radio,
                            ACTION_WIRELESS_SETTINGS);
                    removeHeader = false;
                }
                break;
        }
        if (removeHeader) {
            mControlsParent.setVisibility(View.GONE);
        }
    }

    private void addControl(int title, int summary, int action) {
        final Resources res = getResources();
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.power_usage_action_item,null);
        mControlsParent.addView(item);
        Button actionButton = (Button) item.findViewById(R.id.action_button);
        TextView summaryView = (TextView) item.findViewById(R.id.summary);
        actionButton.setText(res.getString(title));
        summaryView.setText(res.getString(summary));
        actionButton.setOnClickListener(this);
        actionButton.setTag(new Integer(action));
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
        if (uid < 1) {
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
    
    private String getDescriptionForDrainType() {
        return getResources().getString(sDrainTypeDesciptions[mDrainType.ordinal()]);
    }
}
