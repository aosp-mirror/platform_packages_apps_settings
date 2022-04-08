/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.SparseLongArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.BatteryTipDialogFragment;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settings.widget.AppCheckBoxPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;

import java.util.List;

/**
 * Fragment to show a list of anomaly apps, where user could handle these anomalies
 */
public class RestrictedAppDetails extends DashboardFragment implements
        BatteryTipPreferenceController.BatteryTipListener {

    public static final String TAG = "RestrictedAppDetails";

    @VisibleForTesting
    static final String EXTRA_APP_INFO_LIST = "app_info_list";
    private static final String KEY_PREF_RESTRICTED_APP_LIST = "restrict_app_list";
    private static final long TIME_NULL = -1;

    @VisibleForTesting
    List<AppInfo> mAppInfos;
    @VisibleForTesting
    IconDrawableFactory mIconDrawableFactory;
    @VisibleForTesting
    PreferenceGroup mRestrictedAppListGroup;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    PackageManager mPackageManager;
    @VisibleForTesting
    BatteryDatabaseManager mBatteryDatabaseManager;

    public static void startRestrictedAppDetails(InstrumentedPreferenceFragment fragment,
            List<AppInfo> appInfos) {
        final Bundle args = new Bundle();
        args.putParcelableList(EXTRA_APP_INFO_LIST, appInfos);

        new SubSettingLauncher(fragment.getContext())
                .setDestination(RestrictedAppDetails.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.restricted_app_title)
                .setSourceMetricsCategory(fragment.getMetricsCategory())
                .launch();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getContext();

        mRestrictedAppListGroup = (PreferenceGroup) findPreference(KEY_PREF_RESTRICTED_APP_LIST);
        mAppInfos = getArguments().getParcelableArrayList(EXTRA_APP_INFO_LIST);
        mPackageManager = context.getPackageManager();
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        mBatteryUtils = BatteryUtils.getInstance(context);
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);

        refreshUi();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.restricted_apps_detail;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_RESTRICTED_APP_DETAILS;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_restricted_apps;
    }

    @VisibleForTesting
    void refreshUi() {
        mRestrictedAppListGroup.removeAll();
        final Context context = getPrefContext();
        final SparseLongArray timestampArray = mBatteryDatabaseManager
                .queryActionTime(AnomalyDatabaseHelper.ActionType.RESTRICTION);
        final long now = System.currentTimeMillis();

        for (int i = 0, size = mAppInfos.size(); i < size; i++) {
            final CheckBoxPreference checkBoxPreference = new AppCheckBoxPreference(context);
            final AppInfo appInfo = mAppInfos.get(i);
            try {
                final ApplicationInfo applicationInfo = mPackageManager.getApplicationInfoAsUser(
                        appInfo.packageName, 0 /* flags */, UserHandle.getUserId(appInfo.uid));
                checkBoxPreference.setChecked(
                        mBatteryUtils.isForceAppStandbyEnabled(appInfo.uid, appInfo.packageName));
                checkBoxPreference.setTitle(mPackageManager.getApplicationLabel(applicationInfo));
                checkBoxPreference.setIcon(
                        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager,
                                appInfo.packageName,
                                UserHandle.getUserId(appInfo.uid)));
                checkBoxPreference.setKey(getKeyFromAppInfo(appInfo));
                checkBoxPreference.setOnPreferenceChangeListener((pref, value) -> {
                    final BatteryTipDialogFragment fragment = createDialogFragment(appInfo,
                            (Boolean) value);
                    fragment.setTargetFragment(this, 0 /* requestCode */);
                    fragment.show(getFragmentManager(), TAG);

                    return false;
                });

                final long timestamp = timestampArray.get(appInfo.uid, TIME_NULL);
                if (timestamp != TIME_NULL) {
                    checkBoxPreference.setSummary(getString(R.string.restricted_app_time_summary,
                            StringUtil.formatRelativeTime(context, now - timestamp, false)));
                }
                final CharSequence test = checkBoxPreference.getSummaryOn();
                mRestrictedAppListGroup.addPreference(checkBoxPreference);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Can't find package: " + appInfo.packageName);
            }
        }
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        final AppInfo appInfo;
        final boolean isRestricted = batteryTip instanceof RestrictAppTip;
        if (isRestricted) {
            appInfo = ((RestrictAppTip) batteryTip).getRestrictAppList().get(0);
        } else {
            appInfo = ((UnrestrictAppTip) batteryTip).getUnrestrictAppInfo();
        }

        CheckBoxPreference preference = (CheckBoxPreference) mRestrictedAppListGroup
                .findPreference(getKeyFromAppInfo(appInfo));
        if (preference != null) {
            preference.setChecked(isRestricted);
        }
    }

    @VisibleForTesting
    BatteryTipDialogFragment createDialogFragment(AppInfo appInfo, boolean toRestrict) {
        final BatteryTip batteryTip = toRestrict
                ? new RestrictAppTip(BatteryTip.StateType.NEW, appInfo)
                : new UnrestrictAppTip(BatteryTip.StateType.NEW, appInfo);

        return BatteryTipDialogFragment.newInstance(
                batteryTip, getMetricsCategory());
    }

    @VisibleForTesting
    String getKeyFromAppInfo(AppInfo appInfo) {
        return appInfo.uid + "," + appInfo.packageName;
    }
}
