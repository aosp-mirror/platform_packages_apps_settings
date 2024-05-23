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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datastore.ChangeReason;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Allow background usage fragment for each app */
public class PowerBackgroundUsageDetail extends DashboardFragment
        implements SelectorWithWidgetPreference.OnClickListener, OnCheckedChangeListener {
    private static final String TAG = "PowerBackgroundUsageDetail";

    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_POWER_USAGE_AMOUNT = "extra_power_usage_amount";
    public static final String EXTRA_ICON_ID = "extra_icon_id";
    private static final String KEY_PREF_HEADER = "header_view";
    private static final String KEY_PREF_UNRESTRICTED = "unrestricted_preference";
    private static final String KEY_PREF_OPTIMIZED = "optimized_preference";
    private static final String KEY_ALLOW_BACKGROUND_USAGE = "allow_background_usage";
    private static final String KEY_FOOTER_PREFERENCE = "app_usage_footer_preference";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @VisibleForTesting LayoutPreference mHeaderPreference;
    @VisibleForTesting ApplicationsState mState;
    @VisibleForTesting ApplicationsState.AppEntry mAppEntry;
    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;
    @VisibleForTesting SelectorWithWidgetPreference mOptimizePreference;
    @VisibleForTesting SelectorWithWidgetPreference mUnrestrictedPreference;
    @VisibleForTesting MainSwitchPreference mMainSwitchPreference;
    @VisibleForTesting FooterPreference mFooterPreference;
    @VisibleForTesting StringBuilder mLogStringBuilder;

    @VisibleForTesting @BatteryOptimizeUtils.OptimizationMode
    int mOptimizationMode = BatteryOptimizeUtils.MODE_UNKNOWN;

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

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference selected) {
        final String selectedKey = selected == null ? null : selected.getKey();
        updateSelectorPreferenceState(mUnrestrictedPreference, selectedKey);
        updateSelectorPreferenceState(mOptimizePreference, selectedKey);
        mBatteryOptimizeUtils.setAppUsageState(getSelectedPreference(), Action.APPLY);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mMainSwitchPreference.setChecked(isChecked);
        updateSelectorPreference(isChecked);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Bundle bundle = getArguments();
        final int uid = bundle.getInt(EXTRA_UID, 0);
        final String packageName = bundle.getString(EXTRA_PACKAGE_NAME);

        controllers.add(new AllowBackgroundPreferenceController(context, uid, packageName));
        controllers.add(new OptimizedPreferenceController(context, uid, packageName));
        controllers.add(new UnrestrictedPreferenceController(context, uid, packageName));

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

    @VisibleForTesting
    void updateSelectorPreference(boolean isEnabled) {
        mOptimizePreference.setEnabled(isEnabled);
        mUnrestrictedPreference.setEnabled(isEnabled);
        onRadioButtonClicked(isEnabled ? mOptimizePreference : null);
    }

    @VisibleForTesting
    void notifyBackupManager() {
        if (mOptimizationMode != mBatteryOptimizeUtils.getAppOptimizationMode()) {
            BatterySettingsStorage.get(getContext()).notifyChange(ChangeReason.UPDATE);
        }
    }

    @VisibleForTesting
    int getSelectedPreference() {
        if (!mMainSwitchPreference.isChecked()) {
            return BatteryOptimizeUtils.MODE_RESTRICTED;
        } else if (mUnrestrictedPreference.isChecked()) {
            return BatteryOptimizeUtils.MODE_UNRESTRICTED;
        } else if (mOptimizePreference.isChecked()) {
            return BatteryOptimizeUtils.MODE_OPTIMIZED;
        } else {
            return BatteryOptimizeUtils.MODE_UNKNOWN;
        }
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

        controller.done(true /* rebindActions */);
    }

    @VisibleForTesting
    void initFooter() {
        final String stateString;
        final String footerString;
        final Context context = getContext();

        if (mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()) {
            // Present optimized only string when the package name is invalid.
            stateString = context.getString(R.string.manager_battery_usage_optimized_only);
            footerString =
                    context.getString(R.string.manager_battery_usage_footer_limited, stateString);
        } else if (mBatteryOptimizeUtils.isSystemOrDefaultApp()) {
            // Present unrestricted only string when the package is system or default active app.
            stateString = context.getString(R.string.manager_battery_usage_unrestricted_only);
            footerString =
                    context.getString(R.string.manager_battery_usage_footer_limited, stateString);
        } else {
            // Present default string to normal app.
            footerString = context.getString(R.string.manager_battery_usage_footer);
        }
        mFooterPreference.setTitle(footerString);
        final Intent helpIntent =
                HelpUtils.getHelpIntent(
                        context,
                        context.getString(R.string.help_url_app_usage_settings),
                        /* backupContext= */ "");
        if (helpIntent != null) {
            mFooterPreference.setLearnMoreAction(
                    v -> startActivityForResult(helpIntent, /* requestCode= */ 0));
            mFooterPreference.setLearnMoreText(
                    context.getString(R.string.manager_battery_usage_link_a11y));
        }
    }

    private void onCreateBackgroundUsageState(String packageName) {
        mOptimizePreference = findPreference(KEY_PREF_OPTIMIZED);
        mUnrestrictedPreference = findPreference(KEY_PREF_UNRESTRICTED);
        mMainSwitchPreference = findPreference(KEY_ALLOW_BACKGROUND_USAGE);
        mFooterPreference = findPreference(KEY_FOOTER_PREFERENCE);

        mOptimizePreference.setOnClickListener(this);
        mUnrestrictedPreference.setOnClickListener(this);
        mMainSwitchPreference.addOnSwitchChangeListener(this);

        mBatteryOptimizeUtils =
                new BatteryOptimizeUtils(
                        getContext(), getArguments().getInt(EXTRA_UID), packageName);
    }

    private void updateSelectorPreferenceState(
            SelectorWithWidgetPreference preference, String selectedKey) {
        preference.setChecked(TextUtils.equals(selectedKey, preference.getKey()));
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
