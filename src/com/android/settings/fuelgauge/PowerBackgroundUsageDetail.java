/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.batteryusage.AppOptModeSharedPreferencesUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IntroPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Allow background usage fragment for each app */
public class PowerBackgroundUsageDetail extends DashboardFragment {
    private static final String TAG = "PowerBackgroundUsageDetail";

    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_POWER_USAGE_AMOUNT = "extra_power_usage_amount";
    public static final String EXTRA_ICON_ID = "extra_icon_id";
    private static final String KEY_PREF_HEADER = "header_view";
    private static final String KEY_FOOTER_PREFERENCE = "app_usage_footer_preference";
    private static final String KEY_BATTERY_OPTIMIZATION_MODE_CATEGORY =
            "battery_optimization_mode_category";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @VisibleForTesting ApplicationsState mState;
    @VisibleForTesting ApplicationsState.AppEntry mAppEntry;
    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;
    @VisibleForTesting StringBuilder mLogStringBuilder;

    @VisibleForTesting @BatteryOptimizeUtils.OptimizationMode
    int mOptimizationMode = BatteryOptimizeUtils.MODE_UNKNOWN;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        final Bundle bundle = getArguments();
        final int uid = bundle.getInt(EXTRA_UID, 0);
        final String packageName = bundle.getString(EXTRA_PACKAGE_NAME);
        mBatteryOptimizeUtils = new BatteryOptimizeUtils(getContext(), uid, packageName);
        mState = ApplicationsState.getInstance(getActivity().getApplication());
        if (packageName != null) {
            mAppEntry = mState.getEntry(packageName, UserHandle.myUserId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initHeader();
        initFooter();
        mOptimizationMode = mBatteryOptimizeUtils.getAppOptimizationMode();
        mLogStringBuilder = new StringBuilder("onResume mode = ").append(mOptimizationMode);
    }

    @Override
    public void onPause() {
        super.onPause();

        final int currentOptimizeMode = mBatteryOptimizeUtils.getAppOptimizationMode();
        final Context applicationContext = requireContext().getApplicationContext();
        mLogStringBuilder.append(", onPause mode = ").append(currentOptimizeMode);
        logMetricCategory(currentOptimizeMode);
        mExecutor.execute(
                () -> {
                    if (currentOptimizeMode != mOptimizationMode) {
                        AppOptModeSharedPreferencesUtils.deleteAppOptimizationModeEventByUid(
                                applicationContext, mBatteryOptimizeUtils.getUid());
                    }
                    BatteryOptimizeLogUtils.writeLog(
                            applicationContext,
                            Action.LEAVE,
                            BatteryOptimizeLogUtils.getPackageNameWithUserId(
                                    mBatteryOptimizeUtils.getPackageName(), UserHandle.myUserId()),
                            mLogStringBuilder.toString());
                });
        Log.d(TAG, "Leave with mode: " + currentOptimizeMode);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>(1);
        controllers.add(
                new BatteryOptimizationModePreferenceController(
                        context, KEY_BATTERY_OPTIMIZATION_MODE_CATEGORY, mBatteryOptimizeUtils));

        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_background_usage_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    static void startPowerBackgroundUsageDetailPage(Context context, Bundle args) {
        new SubSettingLauncher(context)
                .setDestination(PowerBackgroundUsageDetail.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(SettingsEnums.FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND)
                .launch();
    }

    @VisibleForTesting
    void initHeader() {
        final IntroPreference introPreference = findPreference(KEY_PREF_HEADER);
        if (introPreference == null) {
            return;
        }
        final Activity context = getActivity();
        final Bundle bundle = getArguments();

        if (mAppEntry == null) {
            introPreference.setTitle(bundle.getString(EXTRA_LABEL));

            final int iconId = bundle.getInt(EXTRA_ICON_ID, 0);
            if (iconId == 0) {
                introPreference.setIcon(context.getPackageManager().getDefaultActivityIcon());
            } else {
                introPreference.setIcon(context.getDrawable(bundle.getInt(EXTRA_ICON_ID)));
            }
        } else {
            mState.ensureIcon(mAppEntry);
            introPreference.setTitle(mAppEntry.label);
            introPreference.setIcon(Utils.getBadgedIcon(context, mAppEntry.info));
        }
    }

    @VisibleForTesting
    void initFooter() {
        final FooterPreference footerPreference = findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference == null) {
            return;
        }
        final Context context = getContext();
        footerPreference.setTitle(context.getString(R.string.manager_battery_usage_footer));
        final Intent helpIntent =
                HelpUtils.getHelpIntent(
                        context,
                        context.getString(R.string.help_url_app_usage_settings),
                        /* backupContext= */ "");
        if (helpIntent != null) {
            footerPreference.setLearnMoreAction(
                    v -> startActivityForResult(helpIntent, /* requestCode= */ 0));
            footerPreference.setLearnMoreText(
                    context.getString(R.string.manager_battery_usage_link_a11y));
        }
    }

    private void logMetricCategory(int currentOptimizeMode) {
        if (currentOptimizeMode == mOptimizationMode) {
            return;
        }
        int metricCategory = 0;
        switch (currentOptimizeMode) {
            case BatteryOptimizeUtils.MODE_UNRESTRICTED:
                metricCategory = SettingsEnums.ACTION_APP_BATTERY_USAGE_UNRESTRICTED;
                break;
            case BatteryOptimizeUtils.MODE_OPTIMIZED:
                metricCategory = SettingsEnums.ACTION_APP_BATTERY_USAGE_OPTIMIZED;
                break;
            case BatteryOptimizeUtils.MODE_RESTRICTED:
                metricCategory = SettingsEnums.ACTION_APP_BATTERY_USAGE_RESTRICTED;
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
                                    /* attribution */ SettingsEnums
                                            .LEAVE_POWER_USAGE_MANAGE_BACKGROUND,
                                    /* action */ finalMetricCategory,
                                    /* pageId */ SettingsEnums
                                            .FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND,
                                    packageName,
                                    getArguments().getInt(EXTRA_POWER_USAGE_AMOUNT));
                });
    }
}
