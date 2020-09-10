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

package com.android.settings.development.graphicsdriver;

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller of all the per App based list preferences.
 */
public class GraphicsDriverAppPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener,
        GraphicsDriverContentObserver.OnGraphicsDriverContentChangedListener, LifecycleObserver,
        OnStart, OnStop {

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final String mPreferenceTitle;
    private final String mPreferenceDefault;
    private final String mPreferenceGameDriver;
    private final String mPreferencePrereleaseDriver;
    private final String mPreferenceSystem;
    @VisibleForTesting
    CharSequence[] mEntryList;
    @VisibleForTesting
    GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private final List<AppInfo> mAppInfos;
    private final Set<String> mDevOptInApps;
    private final Set<String> mDevPrereleaseOptInApps;
    private final Set<String> mDevOptOutApps;

    private PreferenceGroup mPreferenceGroup;

    public GraphicsDriverAppPreferenceController(Context context, String key) {
        super(context, key);

        mContext = context;
        mContentResolver = context.getContentResolver();
        mGraphicsDriverContentObserver =
                new GraphicsDriverContentObserver(new Handler(Looper.getMainLooper()), this);

        final Resources resources = context.getResources();
        mPreferenceTitle = resources.getString(R.string.graphics_driver_app_preference_title);
        mPreferenceDefault = resources.getString(R.string.graphics_driver_app_preference_default);
        mPreferenceGameDriver =
                resources.getString(R.string.graphics_driver_app_preference_game_driver);
        mPreferencePrereleaseDriver =
                resources.getString(R.string.graphics_driver_app_preference_prerelease_driver);
        mPreferenceSystem = resources.getString(R.string.graphics_driver_app_preference_system);
        mEntryList = GraphicsDriverEnableForAllAppsPreferenceController.constructEntryList(
                mContext, true);

        // TODO: Move this task to background if there's potential ANR/Jank.
        // Update the UI when all the app infos are ready.
        mAppInfos = getAppInfos(context);

        mDevOptInApps =
                getGlobalSettingsString(mContentResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS);
        mDevPrereleaseOptInApps = getGlobalSettingsString(
                mContentResolver, Settings.Global.GAME_DRIVER_PRERELEASE_OPT_IN_APPS);
        mDevOptOutApps =
                getGlobalSettingsString(mContentResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS);
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                && (Settings.Global.getInt(mContentResolver,
                Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                != GAME_DRIVER_OFF)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());

        final Context context = mPreferenceGroup.getContext();
        for (AppInfo appInfo : mAppInfos) {
            mPreferenceGroup.addPreference(
                    createListPreference(context, appInfo.info.packageName, appInfo.label));
        }
    }

    @Override
    public void onStart() {
        mGraphicsDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mGraphicsDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setVisible(isAvailable());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ListPreference listPref = (ListPreference) preference;
        final String value = newValue.toString();
        final String packageName = preference.getKey();

        // When user choose a new preference, update both Sets for
        // opt-in and opt-out apps. Then set the new summary text.
        if (value.equals(mPreferenceSystem)) {
            mDevOptInApps.remove(packageName);
            mDevPrereleaseOptInApps.remove(packageName);
            mDevOptOutApps.add(packageName);
        } else if (value.equals(mPreferenceGameDriver)) {
            mDevOptInApps.add(packageName);
            mDevPrereleaseOptInApps.remove(packageName);
            mDevOptOutApps.remove(packageName);
        } else if (value.equals(mPreferencePrereleaseDriver)) {
            mDevOptInApps.remove(packageName);
            mDevPrereleaseOptInApps.add(packageName);
            mDevOptOutApps.remove(packageName);
        } else {
            mDevOptInApps.remove(packageName);
            mDevPrereleaseOptInApps.remove(packageName);
            mDevOptOutApps.remove(packageName);
        }
        listPref.setValue(value);
        listPref.setSummary(value);

        // Push the updated Sets for stable/prerelease opt-in and opt-out apps to
        // corresponding Settings.Global.GAME_DRIVER(_PRERELEASE)?_OPT_(IN|OUT)_APPS
        Settings.Global.putString(mContentResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS,
                String.join(",", mDevOptInApps));
        Settings.Global.putString(mContentResolver,
                Settings.Global.GAME_DRIVER_PRERELEASE_OPT_IN_APPS,
                String.join(",", mDevPrereleaseOptInApps));
        Settings.Global.putString(mContentResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS,
                String.join(",", mDevOptOutApps));

        return true;
    }

    @Override
    public void onGraphicsDriverContentChanged() {
        updateState(mPreferenceGroup);
    }

    // AppInfo class to achieve loading the application label only once
    class AppInfo {
        AppInfo(PackageManager packageManager, ApplicationInfo applicationInfo) {
            info = applicationInfo;
            label = packageManager.getApplicationLabel(applicationInfo).toString();
        }

        public final ApplicationInfo info;
        public final String label;
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

        Collections.sort(appInfos, mAppInfoComparator);

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

    private final Comparator<AppInfo> mAppInfoComparator = new Comparator<AppInfo>() {
        public int compare(AppInfo a, AppInfo b) {
            return Collator.getInstance().compare(a.label, b.label);
        }
    };

    @VisibleForTesting
    protected ListPreference createListPreference(
            Context context, String packageName, String appName) {
        final ListPreference listPreference = new ListPreference(context);

        listPreference.setKey(packageName);
        listPreference.setTitle(appName);
        listPreference.setDialogTitle(mPreferenceTitle);
        listPreference.setEntries(mEntryList);
        listPreference.setEntryValues(mEntryList);

        // Initialize preference default and summary with the opt in/out choices
        // from Settings.Global.GAME_DRIVER(_PRERELEASE)?_OPT_(IN|OUT)_APPS
        if (mDevOptOutApps.contains(packageName)) {
            listPreference.setValue(mPreferenceSystem);
            listPreference.setSummary(mPreferenceSystem);
        } else if (mDevPrereleaseOptInApps.contains(packageName)) {
            listPreference.setValue(mPreferencePrereleaseDriver);
            listPreference.setSummary(mPreferencePrereleaseDriver);
        } else if (mDevOptInApps.contains(packageName)) {
            listPreference.setValue(mPreferenceGameDriver);
            listPreference.setSummary(mPreferenceGameDriver);
        } else {
            listPreference.setValue(mPreferenceDefault);
            listPreference.setSummary(mPreferenceDefault);
        }

        listPreference.setOnPreferenceChangeListener(this);

        return listPreference;
    }
}
