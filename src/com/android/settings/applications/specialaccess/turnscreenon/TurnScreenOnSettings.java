/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.applications.specialaccess.turnscreenon;

import android.Manifest;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.util.Pair;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.AppPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Settings page for providing special app access to turn the screen of the device on.
 */
@SearchIndexable
public class TurnScreenOnSettings extends EmptyTextSettings {

    @VisibleForTesting
    static final List<String> IGNORE_PACKAGE_LIST = new ArrayList<>();

    static {
        IGNORE_PACKAGE_LIST.add("com.android.systemui");
    }

    /**
     * Comparator by name, then user id.
     * {@see PackageItemInfo#DisplayNameComparator}
     */
    static class AppComparator implements Comparator<Pair<ApplicationInfo, Integer>> {

        private final Collator mCollator = Collator.getInstance();
        private final PackageManager mPm;

        AppComparator(PackageManager pm) {
            mPm = pm;
        }

        public final int compare(Pair<ApplicationInfo, Integer> a,
                Pair<ApplicationInfo, Integer> b) {
            CharSequence sa = a.first.loadLabel(mPm);
            if (sa == null) sa = a.first.name;
            CharSequence sb = b.first.loadLabel(mPm);
            if (sb == null) sb = b.first.name;
            int nameCmp = mCollator.compare(sa.toString(), sb.toString());
            if (nameCmp != 0) {
                return nameCmp;
            } else {
                return a.second - b.second;
            }
        }
    }

    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private IconDrawableFactory mIconDrawableFactory;

    public TurnScreenOnSettings() {
        // Do nothing
    }

    public TurnScreenOnSettings(PackageManager pm, UserManager um) {
        mPackageManager = pm;
        mUserManager = um;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPackageManager = mContext.getPackageManager();
        mUserManager = mContext.getSystemService(UserManager.class);
        mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Clear the prefs
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        // Fetch the set of applications for each profile which have the permission required to turn
        // the screen on with a wake lock.
        final ArrayList<Pair<ApplicationInfo, Integer>> apps = collectTurnScreenOnApps(
                UserHandle.myUserId());
        apps.sort(new AppComparator(mPackageManager));

        // Rebuild the list of prefs
        final Context prefContext = getPrefContext();
        for (final Pair<ApplicationInfo, Integer> appData : apps) {
            final ApplicationInfo appInfo = appData.first;
            final int userId = appData.second;
            final UserHandle user = UserHandle.of(userId);
            final String packageName = appInfo.packageName;
            final CharSequence label = appInfo.loadLabel(mPackageManager);

            final Preference pref = new AppPreference(prefContext);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(appInfo, userId));
            pref.setTitle(mPackageManager.getUserBadgedLabel(label, user));
            pref.setSummary(TurnScreenOnDetails.getPreferenceSummary(mAppOpsManager,
                    appInfo.uid, packageName));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(TurnScreenOnDetails.class,
                            getString(R.string.turn_screen_on_title),
                            packageName, appInfo.uid,
                            TurnScreenOnSettings.this, -1, getMetricsCategory());
                    return true;
                }
            });
            screen.addPreference(pref);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.no_applications);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.turn_screen_on_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_TURN_SCREEN_ON_ACCESS;
    }

    /**
     * @return the list of applications for the given user and all their profiles that can turn on
     * the screen with wake locks.
     */
    @VisibleForTesting
    ArrayList<Pair<ApplicationInfo, Integer>> collectTurnScreenOnApps(int userId) {
        final ArrayList<Pair<ApplicationInfo, Integer>> apps = new ArrayList<>();
        final ArrayList<Integer> userIds = new ArrayList<>();
        for (UserInfo user : mUserManager.getProfiles(userId)) {
            userIds.add(user.id);
        }

        for (int id : userIds) {
            final List<PackageInfo> installedPackages = mPackageManager.getInstalledPackagesAsUser(
                    /* flags= */ 0, id);
            for (PackageInfo packageInfo : installedPackages) {
                if (hasTurnScreenOnPermission(mPackageManager, packageInfo.packageName)) {
                    apps.add(new Pair<>(packageInfo.applicationInfo, id));
                }
            }
        }
        return apps;
    }

    /**
     * @return true if the package has the permission to turn the screen on.
     */
    @VisibleForTesting
    static boolean hasTurnScreenOnPermission(PackageManager packageManager, String packageName) {
        if (IGNORE_PACKAGE_LIST.contains(packageName)) {
            return false;
        }
        return packageManager.checkPermission(Manifest.permission.TURN_SCREEN_ON, packageName)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.turn_screen_on_settings);
}
