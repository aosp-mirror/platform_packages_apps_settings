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
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.IconDrawableFactory;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.widget.AppCheckBoxPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreferenceMixin;

import java.util.List;

/**
 * Fragment to show a list of anomaly apps, where user could handle these anomalies
 */
public class RestrictedAppDetails extends DashboardFragment {

    public static final String TAG = "RestrictedAppDetails";

    @VisibleForTesting
    static final String EXTRA_APP_INFO_LIST = "app_info_list";
    private static final String KEY_PREF_RESTRICTED_APP_LIST = "restrict_app_list";

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
    private final FooterPreferenceMixin mFooterPreferenceMixin =
            new FooterPreferenceMixin(this, getLifecycle());

    public static void startRestrictedAppDetails(SettingsActivity caller,
            InstrumentedPreferenceFragment fragment, List<AppInfo> appInfos) {
        final Bundle args = new Bundle();
        args.putParcelableList(EXTRA_APP_INFO_LIST, appInfos);

        new SubSettingLauncher(caller)
                .setDestination(RestrictedAppDetails.class.getName())
                .setArguments(args)
                .setTitle(R.string.restricted_app_title)
                .setSourceMetricsCategory(fragment.getMetricsCategory())
                .launch();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getContext();

        mFooterPreferenceMixin.createFooterPreference().setTitle(
                R.string.restricted_app_detail_footer);
        mRestrictedAppListGroup = (PreferenceGroup) findPreference(KEY_PREF_RESTRICTED_APP_LIST);
        mAppInfos = getArguments().getParcelableArrayList(EXTRA_APP_INFO_LIST);
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
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

        for (int i = 0, size = mAppInfos.size(); i < size; i++) {
            final CheckBoxPreference checkBoxPreference = new AppCheckBoxPreference(context);
            final AppInfo appInfo = mAppInfos.get(i);
            try {
                final ApplicationInfo applicationInfo = mPackageManager.getApplicationInfoAsUser(
                        appInfo.packageName, 0 /* flags */, UserHandle.getUserId(appInfo.uid));
                checkBoxPreference.setChecked(true);
                checkBoxPreference.setTitle(mPackageManager.getApplicationLabel(applicationInfo));
                checkBoxPreference.setIcon(
                        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager,
                                appInfo.packageName,
                                UserHandle.getUserId(appInfo.uid)));
                checkBoxPreference.setOnPreferenceChangeListener((pref, value) -> {
                    // change the toggle
                    final int mode = (Boolean) value ? AppOpsManager.MODE_IGNORED
                            : AppOpsManager.MODE_ALLOWED;
                    mBatteryUtils.setForceAppStandby(appInfo.uid, appInfo.packageName, mode);
                    return true;
                });
                mRestrictedAppListGroup.addPreference(checkBoxPreference);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
