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

import static com.android.settings.Utils.prepareCustomPreferencesList;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.wifi.WifiSettings;

public class PowerUsageDetail extends Fragment implements Button.OnClickListener {

    enum DrainType {
        IDLE,
        CELL,
        PHONE,
        WIFI,
        BLUETOOTH,
        SCREEN,
        APP,
        USER
    }

    // Note: Must match the sequence of the DrainType
    private static int[] sDrainTypeDesciptions = new int[] {
        R.string.battery_desc_standby,
        R.string.battery_desc_radio,
        R.string.battery_desc_voice,
        R.string.battery_desc_wifi,
        R.string.battery_desc_bluetooth,
        R.string.battery_desc_display,
        R.string.battery_desc_apps,
        R.string.battery_desc_users,
    };

    public static final int ACTION_DISPLAY_SETTINGS = 1;
    public static final int ACTION_WIFI_SETTINGS = 2;
    public static final int ACTION_BLUETOOTH_SETTINGS = 3;
    public static final int ACTION_WIRELESS_SETTINGS = 4;
    public static final int ACTION_APP_DETAILS = 5;
    public static final int ACTION_LOCATION_SETTINGS = 6;
    public static final int ACTION_FORCE_STOP = 7;
    public static final int ACTION_REPORT = 8;

    public static final int USAGE_SINCE_UNPLUGGED = 1;
    public static final int USAGE_SINCE_RESET = 2;

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_GAUGE = "gauge";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_USAGE_SINCE = "since";
    public static final String EXTRA_USAGE_DURATION = "duration";
    public static final String EXTRA_REPORT_DETAILS = "report_details";
    public static final String EXTRA_REPORT_CHECKIN_DETAILS = "report_checkin_details";
    public static final String EXTRA_DETAIL_TYPES = "types"; // Array of usage types (cpu, gps, etc)
    public static final String EXTRA_DETAIL_VALUES = "values"; // Array of doubles
    public static final String EXTRA_DRAIN_TYPE = "drainType"; // DrainType
    public static final String EXTRA_ICON_PACKAGE = "iconPackage"; // String
    public static final String EXTRA_NO_COVERAGE = "noCoverage";
    public static final String EXTRA_ICON_ID = "iconId"; // Int
    public static final String EXTRA_SHOW_LOCATION_BUTTON = "showLocationButton";  // Boolean

    private PackageManager mPm;
    private DevicePolicyManager mDpm;
    private String mTitle;
    private int mUsageSince;
    private int[] mTypes;
    private int mUid;
    private double[] mValues;
    private View mRootView;
    private TextView mTitleView;
    private ViewGroup mTwoButtonsPanel;
    private Button mForceStopButton;
    private Button mReportButton;
    private ViewGroup mDetailsParent;
    private ViewGroup mControlsParent;
    private long mStartTime;
    private DrainType mDrainType;
    private Drawable mAppIcon;
    private double mNoCoverage; // Percentage of time that there was no coverage

    private boolean mUsesGps;
    private boolean mShowLocationButton;

    private static final String TAG = "PowerUsageDetail";
    private String[] mPackages;

    ApplicationInfo mApp;
    ComponentName mInstaller;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPm = getActivity().getPackageManager();
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.power_usage_details, container, false);
        prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        createDetails();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mStartTime = android.os.Process.getElapsedCpuTime();
        checkForceStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void createDetails() {
        final Bundle args = getArguments();
        mTitle = args.getString(EXTRA_TITLE);
        final int percentage = args.getInt(EXTRA_PERCENT, 1);
        final int gaugeValue = args.getInt(EXTRA_GAUGE, 1);
        mUsageSince = args.getInt(EXTRA_USAGE_SINCE, USAGE_SINCE_UNPLUGGED);
        mUid = args.getInt(EXTRA_UID, 0);
        mDrainType = (DrainType) args.getSerializable(EXTRA_DRAIN_TYPE);
        mNoCoverage = args.getDouble(EXTRA_NO_COVERAGE, 0);
        String iconPackage = args.getString(EXTRA_ICON_PACKAGE);
        int iconId = args.getInt(EXTRA_ICON_ID, 0);
        mShowLocationButton = args.getBoolean(EXTRA_SHOW_LOCATION_BUTTON);
        if (!TextUtils.isEmpty(iconPackage)) {
            try {
                final PackageManager pm = getActivity().getPackageManager();
                ApplicationInfo ai = pm.getPackageInfo(iconPackage, 0).applicationInfo;
                if (ai != null) {
                    mAppIcon = ai.loadIcon(pm);
                }
            } catch (NameNotFoundException nnfe) {
                // Use default icon
            }
        } else if (iconId != 0) {
            mAppIcon = getActivity().getResources().getDrawable(iconId);
        }
        if (mAppIcon == null) {
            mAppIcon = getActivity().getPackageManager().getDefaultActivityIcon();
        }

        // Set the description
        final TextView summary = (TextView) mRootView.findViewById(android.R.id.summary);
        summary.setText(getDescriptionForDrainType());
        summary.setVisibility(View.VISIBLE);

        mTypes = args.getIntArray(EXTRA_DETAIL_TYPES);
        mValues = args.getDoubleArray(EXTRA_DETAIL_VALUES);

        mTitleView = (TextView) mRootView.findViewById(android.R.id.title);
        mTitleView.setText(mTitle);

        final TextView text1 = (TextView)mRootView.findViewById(android.R.id.text1);
        text1.setText(getString(R.string.percentage, percentage));

        mTwoButtonsPanel = (ViewGroup)mRootView.findViewById(R.id.two_buttons_panel);
        mForceStopButton = (Button)mRootView.findViewById(R.id.left_button);
        mReportButton = (Button)mRootView.findViewById(R.id.right_button);
        mForceStopButton.setEnabled(false);

        final ProgressBar progress = (ProgressBar) mRootView.findViewById(android.R.id.progress);
        progress.setProgress(gaugeValue);

        final ImageView icon = (ImageView) mRootView.findViewById(android.R.id.icon);
        icon.setImageDrawable(mAppIcon);

        mDetailsParent = (ViewGroup)mRootView.findViewById(R.id.details);
        mControlsParent = (ViewGroup)mRootView.findViewById(R.id.controls);

        fillDetailsSection();
        fillPackagesSection(mUid);
        fillControlsSection(mUid);
        
        if (mUid >= Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setText(R.string.force_stop);
            mForceStopButton.setTag(ACTION_FORCE_STOP);
            mForceStopButton.setOnClickListener(this);
            mReportButton.setText(com.android.internal.R.string.report);
            mReportButton.setTag(ACTION_REPORT);
            mReportButton.setOnClickListener(this);
            
            // check if error reporting is enabled in secure settings
            int enabled = Settings.Global.getInt(getActivity().getContentResolver(),
                    Settings.Global.SEND_ACTION_APP_ERROR, 0);
            if (enabled != 0) {
                if (mPackages != null && mPackages.length > 0) {
                    try {
                        mApp = getActivity().getPackageManager().getApplicationInfo(
                                mPackages[0], 0);
                        mInstaller = ApplicationErrorReport.getErrorReportReceiver(
                                getActivity(), mPackages[0], mApp.flags);
                    } catch (NameNotFoundException e) {
                    }
                }
                mReportButton.setEnabled(mInstaller != null);
            } else {
                mTwoButtonsPanel.setVisibility(View.GONE);
            }
        } else {
            mTwoButtonsPanel.setVisibility(View.GONE);
        }
    }

    public void onClick(View v) {
        doAction((Integer) v.getTag());
    }

    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        // start new fragment to display extended information
        Bundle args = new Bundle();
        args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mPackages[0]);

        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.startPreferencePanel(InstalledAppDetails.class.getName(), args,
                R.string.application_info_label, null, null, 0);
    }

    private void doAction(int action) {
        PreferenceActivity pa = (PreferenceActivity)getActivity();
        switch (action) {
            case ACTION_DISPLAY_SETTINGS:
                pa.startPreferencePanel(DisplaySettings.class.getName(), null,
                        R.string.display_settings_title, null, null, 0);
                break;
            case ACTION_WIFI_SETTINGS:
                pa.startPreferencePanel(WifiSettings.class.getName(), null,
                        R.string.wifi_settings, null, null, 0);
                break;
            case ACTION_BLUETOOTH_SETTINGS:
                pa.startPreferencePanel(BluetoothSettings.class.getName(), null,
                        R.string.bluetooth_settings, null, null, 0);
                break;
            case ACTION_WIRELESS_SETTINGS:
                pa.startPreferencePanel(WirelessSettings.class.getName(), null,
                        R.string.radio_controls_title, null, null, 0);
                break;
            case ACTION_APP_DETAILS:
                startApplicationDetailsActivity();
                break;
            case ACTION_LOCATION_SETTINGS:
                pa.startPreferencePanel(LocationSettings.class.getName(), null,
                        R.string.location_settings_title, null, null, 0);
                break;
            case ACTION_FORCE_STOP:
                killProcesses();
                break;
            case ACTION_REPORT:
                reportBatteryUse();
                break;
        }
    }

    private void fillDetailsSection() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if (mTypes != null && mValues != null) {
            for (int i = 0; i < mTypes.length; i++) {
                // Only add an item if the time is greater than zero
                if (mValues[i] <= 0) continue;
                final String label = getString(mTypes[i]);
                String value = null;
                switch (mTypes[i]) {
                    case R.string.usage_type_data_recv:
                    case R.string.usage_type_data_send:
                    case R.string.usage_type_data_wifi_recv:
                    case R.string.usage_type_data_wifi_send:
                        final long bytes = (long) (mValues[i]);
                        value = Formatter.formatFileSize(getActivity(), bytes);
                        break;
                    case R.string.usage_type_no_coverage:
                        final int percentage = (int) Math.floor(mValues[i]);
                        value = getActivity().getString(R.string.percentage, percentage);
                        break;
                    case R.string.usage_type_gps:
                        mUsesGps = true;
                        // Fall through
                    default:
                        value = Utils.formatElapsedTime(getActivity(), mValues[i], true);
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
        PackageManager pm = getActivity().getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        PackageInfo pi = null;
        try {
            pi = packages != null ? pm.getPackageInfo(packages[0], 0) : null;
        } catch (NameNotFoundException nnfe) { /* Nothing */ }
        ApplicationInfo ai = pi != null? pi.applicationInfo : null;

        boolean removeHeader = true;
        switch (mDrainType) {
            case APP:
                // If it is a Java application and only one package is associated with the Uid
                if (packages != null && packages.length == 1) {
                    addControl(R.string.battery_action_app_details,
                            R.string.battery_sugg_apps_info, ACTION_APP_DETAILS);
                    removeHeader = false;
                    // If the application has a settings screen, jump to  that
                    // TODO:
                }
                // If power usage detail page is launched from location page, suppress "Location"
                // button to prevent circular loops.
                if (mUsesGps && mShowLocationButton) {
                    addControl(R.string.location_settings_title,
                            R.string.battery_sugg_apps_gps, ACTION_LOCATION_SETTINGS);
                    removeHeader = false;
                }
                break;
            case SCREEN:
                addControl(R.string.display_settings,
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
        LayoutInflater inflater = getActivity().getLayoutInflater();
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
        if ((view = mRootView.findViewById(R.id.packages_section_title)) != null) {
            view.setVisibility(View.GONE);
        }
        if ((view = mRootView.findViewById(R.id.packages_section)) != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void killProcesses() {
        if (mPackages == null) return;
        ActivityManager am = (ActivityManager)getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        for (int i = 0; i < mPackages.length; i++) {
            am.forceStopPackage(mPackages[i]);
        }
        checkForceStop();
    }

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mForceStopButton.setEnabled(getResultCode() != Activity.RESULT_CANCELED);
        }
    };
    
    private void checkForceStop() {
        if (mPackages == null || mUid < Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setEnabled(false);
            return;
        }
        for (int i = 0; i < mPackages.length; i++) {
            if (mDpm.packageHasActiveAdmins(mPackages[i])) {
                mForceStopButton.setEnabled(false);
                return;
            }
        }
        for (int i = 0; i < mPackages.length; i++) {
            try {
                ApplicationInfo info = mPm.getApplicationInfo(mPackages[i], 0);
                if ((info.flags&ApplicationInfo.FLAG_STOPPED) == 0) {
                    mForceStopButton.setEnabled(true);
                    break;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                Uri.fromParts("package", mPackages[0], null));
        intent.putExtra(Intent.EXTRA_PACKAGES, mPackages);
        intent.putExtra(Intent.EXTRA_UID, mUid);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mUid));
        getActivity().sendOrderedBroadcast(intent, null, mCheckKillProcessesReceiver, null,
                Activity.RESULT_CANCELED, null, null);
    }
    
    private void reportBatteryUse() {
        if (mPackages == null) return;
        
        ApplicationErrorReport report = new ApplicationErrorReport();
        report.type = ApplicationErrorReport.TYPE_BATTERY;
        report.packageName = mPackages[0];
        report.installerPackageName = mInstaller.getPackageName();
        report.processName = mPackages[0];
        report.time = System.currentTimeMillis();
        report.systemApp = (mApp.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

        final Bundle args = getArguments();
        ApplicationErrorReport.BatteryInfo batteryInfo = new ApplicationErrorReport.BatteryInfo();
        batteryInfo.usagePercent = args.getInt(EXTRA_PERCENT, 1);
        batteryInfo.durationMicros = args.getLong(EXTRA_USAGE_DURATION, 0);
        batteryInfo.usageDetails = args.getString(EXTRA_REPORT_DETAILS);
        batteryInfo.checkinDetails = args.getString(EXTRA_REPORT_CHECKIN_DETAILS);
        report.batteryInfo = batteryInfo;

        Intent result = new Intent(Intent.ACTION_APP_ERROR);
        result.setComponent(mInstaller);
        result.putExtra(Intent.EXTRA_BUG_REPORT, report);
        result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(result);
    }
    
    private void fillPackagesSection(int uid) {
        if (uid < 1) {
            removePackagesSection();
            return;
        }
        ViewGroup packagesParent = (ViewGroup)mRootView.findViewById(R.id.packages_section);
        if (packagesParent == null) return;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        
        PackageManager pm = getActivity().getPackageManager();
        //final Drawable defaultActivityIcon = pm.getDefaultActivityIcon();
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
                //Drawable icon = defaultActivityIcon;
                if (label != null) {
                    mPackages[i] = label.toString();
                }
                //if (ai.icon != 0) {
                //    icon = ai.loadIcon(pm);
                //}
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
