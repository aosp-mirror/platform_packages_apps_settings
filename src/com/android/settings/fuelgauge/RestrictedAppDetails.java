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

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.IconDrawableFactory;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment;
import com.android.settings.fuelgauge.anomaly.AnomalyPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * Fragment to show a list of anomaly apps, where user could handle these anomalies
 */
public class RestrictedAppDetails extends DashboardFragment {

    public static final String TAG = "RestrictedAppDetails";

    private static final String EXTRA_PACKAGE_OPS_LIST = "package_ops_list";
    private static final String KEY_PREF_RESTRICTED_APP_LIST = "restrict_app_list";

    @VisibleForTesting
    List<AppOpsManager.PackageOps> mPackageOpsList;
    @VisibleForTesting
    IconDrawableFactory mIconDrawableFactory;
    @VisibleForTesting
    PreferenceGroup mRestrictedAppListGroup;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    PackageManager mPackageManager;

    public static void startRestrictedAppDetails(SettingsActivity caller,
            PreferenceFragment fragment, List<AppOpsManager.PackageOps> packageOpsList) {
        Bundle args = new Bundle();
        args.putParcelableList(EXTRA_PACKAGE_OPS_LIST, packageOpsList);

        caller.startPreferencePanelAsUser(fragment, RestrictedAppDetails.class.getName(), args,
                R.string.restricted_app_title, null /* titleText */,
                new UserHandle(UserHandle.myUserId()));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getContext();

        mRestrictedAppListGroup = (PreferenceGroup) findPreference(KEY_PREF_RESTRICTED_APP_LIST);
        mPackageOpsList = getArguments().getParcelableArrayList(EXTRA_PACKAGE_OPS_LIST);
        mPackageManager = context.getPackageManager();
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        mBatteryUtils = BatteryUtils.getInstance(context);

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
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FUELGAUGE_RESTRICTED_APP_DETAILS;
    }

    @VisibleForTesting
    void refreshUi() {
        mRestrictedAppListGroup.removeAll();
        final Context context = getPrefContext();

        for (int i = 0, size = mPackageOpsList.size(); i < size; i++) {
            final CheckBoxPreference checkBoxPreference = new CheckBoxPreference(context);
            final AppOpsManager.PackageOps packageOps = mPackageOpsList.get(i);
            try {
                final ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(
                        packageOps.getPackageName(), 0 /* flags */);
                checkBoxPreference.setChecked(true);
                checkBoxPreference.setTitle(mPackageManager.getApplicationLabel(applicationInfo));
                checkBoxPreference.setKey(packageOps.getPackageName());
                checkBoxPreference.setOnPreferenceChangeListener((pref, value) -> {
                    // change the toggle
                    final int mode = (Boolean) value ? AppOpsManager.MODE_IGNORED
                            : AppOpsManager.MODE_ALLOWED;
                    final String packageName = pref.getKey();
                    final int uid = mBatteryUtils.getPackageUid(packageName);
                    mBatteryUtils.setForceAppStandby(uid, packageName, mode);
                    return true;
                });
                mRestrictedAppListGroup.addPreference(checkBoxPreference);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
