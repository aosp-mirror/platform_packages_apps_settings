/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.gup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GupPreferenceController
        extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private final CharSequence[] mEntryList;
    private final String mPreferenceTitle;
    private final String mPreferenceDefault;
    private final String mPreferenceGup;
    private final String mPreferenceNative;

    private final List<AppInfo> mAppInfos;
    private final Set<String> mDevOptInApps;
    private final Set<String> mDevOptOutApps;

    public GupPreferenceController(Context context, String key) {
        super(context, key);

        final Resources resources = context.getResources();
        mEntryList = resources.getStringArray(R.array.gup_app_preference_values);
        mPreferenceTitle = resources.getString(R.string.gup_app_preference_title);
        mPreferenceDefault = resources.getString(R.string.gup_app_preference_default);
        mPreferenceGup = resources.getString(R.string.gup_app_preference_gup);
        mPreferenceNative = resources.getString(R.string.gup_app_preference_native);

        // TODO: Move this task to background if there's potential ANR/Jank.
        // Update the UI when all the app infos are ready.
        mAppInfos = getAppInfos(context);

        final ContentResolver contentResolver = context.getContentResolver();
        mDevOptInApps =
                getGlobalSettingsString(contentResolver, Settings.Global.GUP_DEV_OPT_IN_APPS);
        mDevOptOutApps =
                getGlobalSettingsString(contentResolver, Settings.Global.GUP_DEV_OPT_OUT_APPS);
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                ? AVAILABLE
                : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final PreferenceGroup preferenceGroup =
                (PreferenceGroup) screen.findPreference(getPreferenceKey());
        if (preferenceGroup == null) {
            return;
        }

        for (AppInfo appInfo : mAppInfos) {
            preferenceGroup.addPreference(
                    createListPreference(appInfo.info.packageName, appInfo.label));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ListPreference listPref = (ListPreference) preference;
        final String value = newValue.toString();
        final String packageName = preference.getKey();

        // When user choose a new preference, update both Sets for
        // opt-in and opt-out apps. Then set the new summary text.
        if (value.equals(mPreferenceNative)) {
            mDevOptInApps.remove(packageName);
            mDevOptOutApps.add(packageName);
            listPref.setSummary(mPreferenceNative);
        } else if (value.equals(mPreferenceGup)) {
            mDevOptInApps.add(packageName);
            mDevOptOutApps.remove(packageName);
            listPref.setSummary(mPreferenceGup);
        } else {
            mDevOptInApps.remove(packageName);
            mDevOptOutApps.remove(packageName);
            listPref.setSummary(mPreferenceDefault);
        }

        // Push the updated Sets for opt-in and opt-out apps to
        // corresponding Settings.Global.GUP_DEV_OPT_(IN|OUT)_APPS
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.GUP_DEV_OPT_IN_APPS, String.join(",", mDevOptInApps));
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.GUP_DEV_OPT_OUT_APPS, String.join(",", mDevOptOutApps));

        return true;
    }

    // AppInfo class to achieve loading the application label only once
    class AppInfo {
        AppInfo(PackageManager packageManager, ApplicationInfo applicationInfo) {
            info = applicationInfo;
            label = packageManager.getApplicationLabel(applicationInfo).toString();
        }
        final ApplicationInfo info;
        final String label;
    }

    // List of non-system packages that are installed for the current user.
    private List<AppInfo> getAppInfos(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ApplicationInfo> applicationInfos =
                packageManager.getInstalledApplications(0 /* flags */);

        final List<AppInfo> appInfos = new ArrayList<>();
        for (ApplicationInfo applicationInfo : applicationInfos) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                appInfos.add(new AppInfo(packageManager, applicationInfo));
            }
        }

        Collections.sort(appInfos, appInfoComparator);

        return appInfos;
    }

    // Parse the raw comma separated package names into a String Set
    private Set<String> getGlobalSettingsString(ContentResolver contentResolver, String name) {
        final String settingsValue = Settings.Global.getString(contentResolver, name);
        if (settingsValue == null) {
            return new HashSet<>();
        }

        final Set<String> valueSet = new HashSet<>(Arrays.asList(settingsValue.split(",")));
        valueSet.remove("");

        return valueSet;
    }

    private final Comparator<AppInfo> appInfoComparator = new Comparator<AppInfo>() {
        public final int compare(AppInfo a, AppInfo b) {
            return Collator.getInstance().compare(a.label, b.label);
        }
    };

    @VisibleForTesting
    protected ListPreference createListPreference(String packageName, String appName) {
        final ListPreference listPreference = new ListPreference(mContext);

        listPreference.setKey(packageName);
        listPreference.setTitle(appName);
        listPreference.setDialogTitle(mPreferenceTitle);
        listPreference.setEntries(mEntryList);
        listPreference.setEntryValues(mEntryList);

        // Initialize preference default and summary with the opt in/out choices
        // from Settings.Global.GUP_DEV_OPT_(IN|OUT)_APPS
        if (mDevOptOutApps.contains(packageName)) {
            listPreference.setValue(mPreferenceNative);
            listPreference.setSummary(mPreferenceNative);
        } else if (mDevOptInApps.contains(packageName)) {
            listPreference.setValue(mPreferenceGup);
            listPreference.setSummary(mPreferenceGup);
        } else {
            listPreference.setValue(mPreferenceDefault);
            listPreference.setSummary(mPreferenceDefault);
        }

        listPreference.setOnPreferenceChangeListener(this);

        return listPreference;
    }
}
