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

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isUserConsumer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.appinfo.AppButtonsPreferenceController;
import com.android.settings.applications.appinfo.ButtonActionDialogFragment;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.datastore.ChangeReason;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Power usage detail fragment for each app, this fragment contains <br>
 * <br>
 * 1. Detail battery usage information for app(i.e. usage time, usage amount) <br>
 * 2. Battery related controls for app(i.e uninstall, force stop)
 */
public class AdvancedPowerUsageDetail extends DashboardFragment
        implements ButtonActionDialogFragment.AppButtonsDialogListener,
                Preference.OnPreferenceClickListener,
                Preference.OnPreferenceChangeListener {
    public static final String TAG = "AdvancedPowerDetail";
    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_FOREGROUND_TIME = "extra_foreground_time";
    public static final String EXTRA_BACKGROUND_TIME = "extra_background_time";
    public static final String EXTRA_SCREEN_ON_TIME = "extra_screen_on_time";
    public static final String EXTRA_ANOMALY_HINT_PREF_KEY = "extra_anomaly_hint_pref_key";
    public static final String EXTRA_ANOMALY_HINT_TEXT = "extra_anomaly_hint_text";
    public static final String EXTRA_SHOW_TIME_INFO = "extra_show_time_info";
    public static final String EXTRA_SLOT_TIME = "extra_slot_time";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_ICON_ID = "extra_icon_id";
    public static final String EXTRA_POWER_USAGE_PERCENT = "extra_power_usage_percent";
    public static final String EXTRA_POWER_USAGE_AMOUNT = "extra_power_usage_amount";

    private static final String KEY_PREF_HEADER = "header_view";
    private static final String KEY_ALLOW_BACKGROUND_USAGE = "allow_background_usage";

    private static final int REQUEST_UNINSTALL = 0;
    private static final int REQUEST_REMOVE_DEVICE_ADMIN = 1;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private AppButtonsPreferenceController mAppButtonsPreferenceController;
    private PowerUsageTimeController mPowerUsageTimeController;

    @VisibleForTesting LayoutPreference mHeaderPreference;
    @VisibleForTesting ApplicationsState mState;
    @VisibleForTesting ApplicationsState.AppEntry mAppEntry;
    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;
    @VisibleForTesting PrimarySwitchPreference mAllowBackgroundUsagePreference;

    @VisibleForTesting @BatteryOptimizeUtils.OptimizationMode
    int mOptimizationMode = BatteryOptimizeUtils.MODE_UNKNOWN;

    @VisibleForTesting StringBuilder mLogStringBuilder;

    // A wrapper class to carry LaunchBatteryDetailPage required arguments.
    private static final class LaunchBatteryDetailPageArgs {
        private String mUsagePercent;
        private String mPackageName;
        private String mAppLabel;
        private String mSlotInformation;
        private String mAnomalyHintText;
        private String mAnomalyHintPrefKey;
        private int mUid;
        private int mIconId;
        private int mConsumedPower;
        private long mForegroundTimeMs;
        private long mBackgroundTimeMs;
        private long mScreenOnTimeMs;
        private boolean mShowTimeInformation;
        private boolean mIsUserEntry;
    }

    /** Launches battery details page for an individual battery consumer fragment. */
    public static void startBatteryDetailPage(
            Context context,
            int sourceMetricsCategory,
            BatteryDiffEntry diffEntry,
            String usagePercent,
            String slotInformation,
            boolean showTimeInformation,
            String anomalyHintPrefKey,
            String anomalyHintText) {
        final LaunchBatteryDetailPageArgs launchArgs = new LaunchBatteryDetailPageArgs();
        // configure the launch argument.
        launchArgs.mUsagePercent = usagePercent;
        launchArgs.mPackageName = diffEntry.getPackageName();
        launchArgs.mAppLabel = diffEntry.getAppLabel();
        launchArgs.mSlotInformation = slotInformation;
        launchArgs.mUid = (int) diffEntry.mUid;
        launchArgs.mIconId = diffEntry.getAppIconId();
        launchArgs.mConsumedPower = (int) diffEntry.mConsumePower;
        launchArgs.mShowTimeInformation = showTimeInformation;
        if (launchArgs.mShowTimeInformation) {
            launchArgs.mForegroundTimeMs = diffEntry.mForegroundUsageTimeInMs;
            launchArgs.mBackgroundTimeMs =
                    diffEntry.mBackgroundUsageTimeInMs + diffEntry.mForegroundServiceUsageTimeInMs;
            launchArgs.mScreenOnTimeMs = diffEntry.mScreenOnTimeInMs;
            launchArgs.mAnomalyHintPrefKey = anomalyHintPrefKey;
            launchArgs.mAnomalyHintText = anomalyHintText;
        }
        launchArgs.mIsUserEntry = isUserConsumer(diffEntry.mConsumerType);
        startBatteryDetailPage(context, sourceMetricsCategory, launchArgs);
    }

    /** Launches battery details page for an individual battery consumer. */
    public static void startBatteryDetailPage(
            Activity caller,
            InstrumentedPreferenceFragment fragment,
            BatteryEntry entry,
            String usagePercent) {
        final LaunchBatteryDetailPageArgs launchArgs = new LaunchBatteryDetailPageArgs();
        // configure the launch argument.
        launchArgs.mUsagePercent = usagePercent;
        launchArgs.mPackageName = entry.getDefaultPackageName();
        launchArgs.mAppLabel = entry.getLabel();
        launchArgs.mUid = entry.getUid();
        launchArgs.mIconId = entry.mIconId;
        launchArgs.mConsumedPower = (int) entry.getConsumedPower();
        launchArgs.mIsUserEntry = entry.isUserEntry();
        launchArgs.mShowTimeInformation = false;
        startBatteryDetailPage(caller, fragment.getMetricsCategory(), launchArgs);
    }

    private static void startBatteryDetailPage(
            Context context, int sourceMetricsCategory, LaunchBatteryDetailPageArgs launchArgs) {
        final Bundle args = new Bundle();
        if (launchArgs.mPackageName == null) {
            // populate data for system app
            args.putString(EXTRA_LABEL, launchArgs.mAppLabel);
            args.putInt(EXTRA_ICON_ID, launchArgs.mIconId);
            args.putString(EXTRA_PACKAGE_NAME, null);
        } else {
            // populate data for normal app
            args.putString(EXTRA_PACKAGE_NAME, launchArgs.mPackageName);
        }

        args.putInt(EXTRA_UID, launchArgs.mUid);
        args.putLong(EXTRA_BACKGROUND_TIME, launchArgs.mBackgroundTimeMs);
        args.putLong(EXTRA_FOREGROUND_TIME, launchArgs.mForegroundTimeMs);
        args.putLong(EXTRA_SCREEN_ON_TIME, launchArgs.mScreenOnTimeMs);
        args.putString(EXTRA_SLOT_TIME, launchArgs.mSlotInformation);
        args.putString(EXTRA_POWER_USAGE_PERCENT, launchArgs.mUsagePercent);
        args.putInt(EXTRA_POWER_USAGE_AMOUNT, launchArgs.mConsumedPower);
        args.putBoolean(EXTRA_SHOW_TIME_INFO, launchArgs.mShowTimeInformation);
        args.putString(EXTRA_ANOMALY_HINT_PREF_KEY, launchArgs.mAnomalyHintPrefKey);
        args.putString(EXTRA_ANOMALY_HINT_TEXT, launchArgs.mAnomalyHintText);
        final int userId =
                launchArgs.mIsUserEntry
                        ? ActivityManager.getCurrentUser()
                        : UserHandle.getUserId(launchArgs.mUid);

        new SubSettingLauncher(context)
                .setDestination(AdvancedPowerUsageDetail.class.getName())
                .setTitleRes(R.string.battery_details_title)
                .setArguments(args)
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setUserHandle(new UserHandle(userId))
                .launch();
    }

    /** Start packageName's battery detail page. */
    public static void startBatteryDetailPage(
            Activity caller,
            Instrumentable instrumentable,
            String packageName,
            UserHandle userHandle) {
        final Bundle args = new Bundle(3);
        final PackageManager packageManager = caller.getPackageManager();
        args.putString(EXTRA_PACKAGE_NAME, packageName);
        args.putString(EXTRA_POWER_USAGE_PERCENT, Utils.formatPercentage(0));
        try {
            args.putInt(EXTRA_UID, packageManager.getPackageUid(packageName, 0 /* no flag */));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot find package: " + packageName, e);
        }

        new SubSettingLauncher(caller)
                .setDestination(AdvancedPowerUsageDetail.class.getName())
                .setTitleRes(R.string.battery_details_title)
                .setArguments(args)
                .setSourceMetricsCategory(instrumentable.getMetricsCategory())
                .setUserHandle(userHandle)
                .launch();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mState = ApplicationsState.getInstance(getActivity().getApplication());
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final String packageName = getArguments().getString(EXTRA_PACKAGE_NAME);
        onCreateBackgroundUsageState(packageName);
        mHeaderPreference = findPreference(KEY_PREF_HEADER);

        if (packageName != null) {
            mAppEntry = mState.getEntry(packageName, UserHandle.myUserId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        initHeader();
        mOptimizationMode = mBatteryOptimizeUtils.getAppOptimizationMode();
        initFooter();
        mLogStringBuilder = new StringBuilder("onResume mode = ").append(mOptimizationMode);
    }

    @Override
    public void onPause() {
        super.onPause();

        notifyBackupManager();
        final int currentOptimizeMode = mBatteryOptimizeUtils.getAppOptimizationMode();
        mLogStringBuilder.append(", onPause mode = ").append(currentOptimizeMode);
        logMetricCategory(currentOptimizeMode);

        mExecutor.execute(
                () -> {
                    BatteryOptimizeLogUtils.writeLog(
                            getContext().getApplicationContext(),
                            Action.LEAVE,
                            BatteryOptimizeLogUtils.getPackageNameWithUserId(
                                    mBatteryOptimizeUtils.getPackageName(), UserHandle.myUserId()),
                            mLogStringBuilder.toString());
                });
        Log.d(TAG, "Leave with mode: " + currentOptimizeMode);
    }

    @VisibleForTesting
    void notifyBackupManager() {
        if (mOptimizationMode != mBatteryOptimizeUtils.getAppOptimizationMode()) {
            BatterySettingsStorage.get(getContext()).notifyChange(ChangeReason.UPDATE);
        }
    }

    @VisibleForTesting
    void initHeader() {
        final View appSnippet = mHeaderPreference.findViewById(R.id.entity_header);
        final Activity context = getActivity();
        final Bundle bundle = getArguments();
        EntityHeaderController controller =
                EntityHeaderController.newInstance(context, this, appSnippet)
                        .setButtonActions(
                                EntityHeaderController.ActionType.ACTION_NONE,
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
            controller.setIsInstantApp(AppUtils.isInstant(mAppEntry.info));
        }

        if (mPowerUsageTimeController != null) {
            final String slotTime = bundle.getString(EXTRA_SLOT_TIME);
            final long screenOnTimeInMs = bundle.getLong(EXTRA_SCREEN_ON_TIME);
            final long backgroundTimeMs = bundle.getLong(EXTRA_BACKGROUND_TIME);
            final String anomalyHintPrefKey = bundle.getString(EXTRA_ANOMALY_HINT_PREF_KEY);
            final String anomalyHintText = bundle.getString(EXTRA_ANOMALY_HINT_TEXT);
            mPowerUsageTimeController.handleScreenTimeUpdated(
                    slotTime,
                    screenOnTimeInMs,
                    backgroundTimeMs,
                    anomalyHintPrefKey,
                    anomalyHintText);
        }
        controller.done(true /* rebindActions */);
    }

    @VisibleForTesting
    void initFooter() {
        final String stateString;
        final String detailInfoString;
        final Context context = getContext();

        if (mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()) {
            // Present optimized only string when the package name is invalid.
            stateString = context.getString(R.string.manager_battery_usage_optimized_only);
            detailInfoString =
                    context.getString(R.string.manager_battery_usage_footer_limited, stateString);
        } else if (mBatteryOptimizeUtils.isSystemOrDefaultApp()) {
            // Present unrestricted only string when the package is system or default active app.
            stateString = context.getString(R.string.manager_battery_usage_unrestricted_only);
            detailInfoString =
                    context.getString(R.string.manager_battery_usage_footer_limited, stateString);
        } else {
            // Present default string to normal app.
            detailInfoString =
                    context.getString(
                            R.string.manager_battery_usage_allow_background_usage_summary);
        }
        mAllowBackgroundUsagePreference.setSummary(detailInfoString);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_POWER_USAGE_DETAIL;
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Bundle bundle = getArguments();
        final int uid = bundle.getInt(EXTRA_UID, 0);
        final String packageName = bundle.getString(EXTRA_PACKAGE_NAME);

        mAppButtonsPreferenceController =
                new AppButtonsPreferenceController(
                        (SettingsActivity) getActivity(),
                        this,
                        getSettingsLifecycle(),
                        packageName,
                        mState,
                        REQUEST_UNINSTALL,
                        REQUEST_REMOVE_DEVICE_ADMIN);
        if (bundle.getBoolean(EXTRA_SHOW_TIME_INFO, false)) {
            mPowerUsageTimeController = new PowerUsageTimeController(getContext());
            controllers.add(mPowerUsageTimeController);
        }
        controllers.add(mAppButtonsPreferenceController);
        controllers.add(new AllowBackgroundPreferenceController(context, uid, packageName));

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
    public boolean onPreferenceClick(Preference preference) {
        if (!(preference instanceof PrimarySwitchPreference)
                || !TextUtils.equals(preference.getKey(), KEY_ALLOW_BACKGROUND_USAGE)) {
            return false;
        }
        PowerBackgroundUsageDetail.startPowerBackgroundUsageDetailPage(
                getContext(), getArguments());
        return true;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (!(preference instanceof PrimarySwitchPreference)
                || !TextUtils.equals(preference.getKey(), KEY_ALLOW_BACKGROUND_USAGE)) {
            return false;
        }
        if (newValue instanceof Boolean) {
            final boolean isAllowBackgroundUsage = (boolean) newValue;
            mBatteryOptimizeUtils.setAppUsageState(
                    isAllowBackgroundUsage
                            ? BatteryOptimizeUtils.MODE_OPTIMIZED
                            : BatteryOptimizeUtils.MODE_RESTRICTED,
                    Action.APPLY);
        }
        return true;
    }

    private void logMetricCategory(int currentOptimizeMode) {
        if (currentOptimizeMode == mOptimizationMode) {
            return;
        }
        int metricCategory = 0;
        switch (currentOptimizeMode) {
            case BatteryOptimizeUtils.MODE_UNRESTRICTED:
            case BatteryOptimizeUtils.MODE_OPTIMIZED:
                metricCategory = SettingsEnums.ACTION_APP_BATTERY_USAGE_ALLOW_BACKGROUND;
                break;
            case BatteryOptimizeUtils.MODE_RESTRICTED:
                metricCategory = SettingsEnums.ACTION_APP_BATTERY_USAGE_DISABLE_BACKGROUND;
                break;
        }
        if (metricCategory == 0) {
            return;
        }
        int finalMetricCategory = metricCategory;
        mExecutor.execute(
                () -> {
                    String packageName =
                            BatteryUtils.getLoggingPackageName(
                                    getContext(), mBatteryOptimizeUtils.getPackageName());
                    FeatureFactory.getFeatureFactory()
                            .getMetricsFeatureProvider()
                            .action(
                                    /* attribution */ SettingsEnums.LEAVE_APP_BATTERY_USAGE,
                                    /* action */ finalMetricCategory,
                                    /* pageId */ SettingsEnums.FUELGAUGE_POWER_USAGE_DETAIL,
                                    packageName,
                                    getArguments().getInt(EXTRA_POWER_USAGE_AMOUNT));
                });
    }

    private void onCreateBackgroundUsageState(String packageName) {
        mAllowBackgroundUsagePreference = findPreference(KEY_ALLOW_BACKGROUND_USAGE);
        if (mAllowBackgroundUsagePreference != null) {
            mAllowBackgroundUsagePreference.setOnPreferenceClickListener(this);
            mAllowBackgroundUsagePreference.setOnPreferenceChangeListener(this);
        }

        mBatteryOptimizeUtils =
                new BatteryOptimizeUtils(
                        getContext(), getArguments().getInt(EXTRA_UID), packageName);
    }
}
