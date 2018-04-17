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

import android.app.Activity;
import android.app.LoaderManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.enterprise.DevicePolicyManagerWrapperImpl;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment;
import com.android.settings.fuelgauge.anomaly.AnomalyLoader;
import com.android.settings.fuelgauge.anomaly.AnomalySummaryPreferenceController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Power usage detail fragment for each app, this fragment contains
 *
 * 1. Detail battery usage information for app(i.e. usage time, usage amount)
 * 2. Battery related controls for app(i.e uninstall, force stop)
 */
public class AdvancedPowerUsageDetail extends DashboardFragment implements
        ButtonActionDialogFragment.AppButtonsDialogListener,
        AnomalyDialogFragment.AnomalyDialogListener,
        LoaderManager.LoaderCallbacks<List<Anomaly>> {

    public static final String TAG = "AdvancedPowerUsageDetail";
    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_FOREGROUND_TIME = "extra_foreground_time";
    public static final String EXTRA_BACKGROUND_TIME = "extra_background_time";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_ICON_ID = "extra_icon_id";
    public static final String EXTRA_POWER_USAGE_PERCENT = "extra_power_usage_percent";
    public static final String EXTRA_POWER_USAGE_AMOUNT = "extra_power_usage_amount";
    public static final String EXTRA_ANOMALY_LIST = "extra_anomaly_list";

    private static final String KEY_PREF_FOREGROUND = "app_usage_foreground";
    private static final String KEY_PREF_BACKGROUND = "app_usage_background";
    private static final String KEY_PREF_POWER_USAGE = "app_power_usage";
    private static final String KEY_PREF_HEADER = "header_view";

    private static final int REQUEST_UNINSTALL = 0;
    private static final int REQUEST_REMOVE_DEVICE_ADMIN = 1;

    private static final int ANOMALY_LOADER = 0;

    @VisibleForTesting
    LayoutPreference mHeaderPreference;
    @VisibleForTesting
    ApplicationsState mState;
    @VisibleForTesting
    ApplicationsState.AppEntry mAppEntry;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    @VisibleForTesting
    Preference mForegroundPreference;
    @VisibleForTesting
    Preference mBackgroundPreference;
    @VisibleForTesting
    Preference mPowerUsagePreference;
    @VisibleForTesting
    AnomalySummaryPreferenceController mAnomalySummaryPreferenceController;
    private AppButtonsPreferenceController mAppButtonsPreferenceController;

    private DevicePolicyManagerWrapper mDpm;
    private UserManager mUserManager;
    private PackageManager mPackageManager;
    private List<Anomaly> mAnomalies;
    private String mPackageName;

    @VisibleForTesting
    static void startBatteryDetailPage(SettingsActivity caller, BatteryUtils batteryUtils,
            PreferenceFragment fragment, BatteryStatsHelper helper, int which, BatteryEntry entry,
            String usagePercent, List<Anomaly> anomalies) {
        // Initialize mStats if necessary.
        helper.getStats();

        final Bundle args = new Bundle();
        final BatterySipper sipper = entry.sipper;
        final BatteryStats.Uid uid = sipper.uidObj;
        final boolean isTypeApp = sipper.drainType == BatterySipper.DrainType.APP;

        final long foregroundTimeMs = isTypeApp ? batteryUtils.getProcessTimeMs(
                BatteryUtils.StatusType.FOREGROUND, uid, which) : sipper.usageTimeMs;
        final long backgroundTimeMs = isTypeApp ? batteryUtils.getProcessTimeMs(
                BatteryUtils.StatusType.BACKGROUND, uid, which) : 0;

        if (ArrayUtils.isEmpty(sipper.mPackages)) {
            // populate data for system app
            args.putString(EXTRA_LABEL, entry.getLabel());
            args.putInt(EXTRA_ICON_ID, entry.iconId);
            args.putString(EXTRA_PACKAGE_NAME, null);
        } else {
            // populate data for normal app
            args.putString(EXTRA_PACKAGE_NAME, entry.defaultPackageName != null
                    ? entry.defaultPackageName
                    : sipper.mPackages[0]);
        }

        args.putInt(EXTRA_UID, sipper.getUid());
        args.putLong(EXTRA_BACKGROUND_TIME, backgroundTimeMs);
        args.putLong(EXTRA_FOREGROUND_TIME, foregroundTimeMs);
        args.putString(EXTRA_POWER_USAGE_PERCENT, usagePercent);
        args.putInt(EXTRA_POWER_USAGE_AMOUNT, (int) sipper.totalPowerMah);
        args.putParcelableList(EXTRA_ANOMALY_LIST, anomalies);

        caller.startPreferencePanelAsUser(fragment, AdvancedPowerUsageDetail.class.getName(), args,
                R.string.battery_details_title, null,
                new UserHandle(UserHandle.getUserId(sipper.getUid())));
    }

    public static void startBatteryDetailPage(SettingsActivity caller, PreferenceFragment fragment,
            BatteryStatsHelper helper, int which, BatteryEntry entry, String usagePercent,
            List<Anomaly> anomalies) {
        startBatteryDetailPage(caller, BatteryUtils.getInstance(caller), fragment, helper, which,
                entry, usagePercent, anomalies);
    }

    public static void startBatteryDetailPage(SettingsActivity caller, PreferenceFragment fragment,
            String packageName) {
        final Bundle args = new Bundle(3);
        final PackageManager packageManager = caller.getPackageManager();
        args.putString(EXTRA_PACKAGE_NAME, packageName);
        args.putString(EXTRA_POWER_USAGE_PERCENT, Utils.formatPercentage(0));
        try {
            args.putInt(EXTRA_UID, packageManager.getPackageUid(packageName, 0 /* no flag */));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        caller.startPreferencePanelAsUser(fragment, AdvancedPowerUsageDetail.class.getName(), args,
                R.string.battery_details_title, null, new UserHandle(UserHandle.myUserId()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mState = ApplicationsState.getInstance(getActivity().getApplication());
        mDpm = new DevicePolicyManagerWrapperImpl(
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE));
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPackageManager = activity.getPackageManager();
        mBatteryUtils = BatteryUtils.getInstance(getContext());
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPackageName = getArguments().getString(EXTRA_PACKAGE_NAME);
        mAnomalySummaryPreferenceController = new AnomalySummaryPreferenceController(
                (SettingsActivity) getActivity(), this, MetricsEvent.FUELGAUGE_POWER_USAGE_DETAIL);
        mForegroundPreference = findPreference(KEY_PREF_FOREGROUND);
        mBackgroundPreference = findPreference(KEY_PREF_BACKGROUND);
        mPowerUsagePreference = findPreference(KEY_PREF_POWER_USAGE);
        mHeaderPreference = (LayoutPreference) findPreference(KEY_PREF_HEADER);

        if (mPackageName != null) {
            mAppEntry = mState.getEntry(mPackageName, UserHandle.myUserId());
            initAnomalyInfo();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        initHeader();
        initPreference();
    }

    @VisibleForTesting
    void initAnomalyInfo() {
        mAnomalies = getArguments().getParcelableArrayList(EXTRA_ANOMALY_LIST);
        if (mAnomalies == null) {
            getLoaderManager().initLoader(ANOMALY_LOADER, Bundle.EMPTY, this);
        } else if (mAnomalies != null) {
            mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(mAnomalies);
        }
    }

    @VisibleForTesting
    void initHeader() {
        final View appSnippet = mHeaderPreference.findViewById(R.id.entity_header);
        final Activity context = getActivity();
        final Bundle bundle = getArguments();
        EntityHeaderController controller = EntityHeaderController
                .newInstance(context, this, appSnippet)
                .setRecyclerView(getListView(), getLifecycle())
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE);

        if (mAppEntry == null) {
            controller.setLabel(bundle.getString(EXTRA_LABEL));

            final int iconId = bundle.getInt(EXTRA_ICON_ID, 0);
            if (iconId == 0) {
                controller.setIcon(context.getPackageManager().getDefaultActivityIcon());
            } else {
                controller.setIcon(context.getDrawable(bundle.getInt(EXTRA_ICON_ID)));
            }
        } else {
            mState.ensureIcon(mAppEntry);
            controller.setLabel(mAppEntry);
            controller.setIcon(mAppEntry);
            boolean isInstantApp = AppUtils.isInstant(mAppEntry.info);
            CharSequence summary = isInstantApp
                    ? null : getString(Utils.getInstallationStatus(mAppEntry.info));
            controller.setIsInstantApp(AppUtils.isInstant(mAppEntry.info));
            controller.setSummary(summary);
        }

        controller.done(context, true /* rebindActions */);
    }

    @VisibleForTesting
    void initPreference() {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        final long foregroundTimeMs = bundle.getLong(EXTRA_FOREGROUND_TIME);
        final long backgroundTimeMs = bundle.getLong(EXTRA_BACKGROUND_TIME);
        final String usagePercent = bundle.getString(EXTRA_POWER_USAGE_PERCENT);
        final int powerMah = bundle.getInt(EXTRA_POWER_USAGE_AMOUNT);
        mForegroundPreference.setSummary(
                TextUtils.expandTemplate(getText(R.string.battery_used_for),
                        Utils.formatElapsedTime(context, foregroundTimeMs, false)));
        mBackgroundPreference.setSummary(
                TextUtils.expandTemplate(getText(R.string.battery_active_for),
                        Utils.formatElapsedTime(context, backgroundTimeMs, false)));
        mPowerUsagePreference.setSummary(
                getString(R.string.battery_detail_power_percentage, usagePercent, powerMah));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), AnomalySummaryPreferenceController.ANOMALY_KEY)) {
            mAnomalySummaryPreferenceController.onPreferenceTreeClick(preference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_DETAIL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_detail;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Bundle bundle = getArguments();
        final int uid = bundle.getInt(EXTRA_UID, 0);
        final String packageName = bundle.getString(EXTRA_PACKAGE_NAME);

        controllers.add(new BackgroundActivityPreferenceController(context, uid));
        controllers.add(new BatteryOptimizationPreferenceController(
                (SettingsActivity) getActivity(), this, packageName));
        mAppButtonsPreferenceController = new AppButtonsPreferenceController(
                (SettingsActivity) getActivity(), this, getLifecycle(), packageName, mState, mDpm,
                mUserManager, mPackageManager, REQUEST_UNINSTALL, REQUEST_REMOVE_DEVICE_ADMIN);
        controllers.add(mAppButtonsPreferenceController);

        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mAppButtonsPreferenceController != null) {
            mAppButtonsPreferenceController.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void handleDialogClick(int id) {
        if (mAppButtonsPreferenceController != null) {
            mAppButtonsPreferenceController.handleDialogClick(id);
        }
    }

    @Override
    public void onAnomalyHandled(Anomaly anomaly) {
        mAnomalySummaryPreferenceController.hideHighUsagePreference();
    }

    @Override
    public Loader<List<Anomaly>> onCreateLoader(int id, Bundle args) {
        return new AnomalyLoader(getContext(), mPackageName);
    }

    @Override
    public void onLoadFinished(Loader<List<Anomaly>> loader, List<Anomaly> data) {
        final AnomalyUtils anomalyUtils = AnomalyUtils.getInstance(getContext());
        anomalyUtils.logAnomalies(mMetricsFeatureProvider, data,
                MetricsEvent.FUELGAUGE_POWER_USAGE_DETAIL);
        mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Anomaly>> loader) {

    }
}
