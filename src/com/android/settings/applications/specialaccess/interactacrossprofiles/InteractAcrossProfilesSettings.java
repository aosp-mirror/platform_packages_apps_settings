/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.applications.specialaccess.interactacrossprofiles;

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_WORK_AND_PERSONAL_APPS_TITLE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.util.Pair;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.AppPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class InteractAcrossProfilesSettings extends EmptyTextSettings {
    private Context mContext;
    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private CrossProfileApps mCrossProfileApps;
    private IconDrawableFactory mIconDrawableFactory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getContext();
        mPackageManager = mContext.getPackageManager();
        mUserManager = mContext.getSystemService(UserManager.class);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
        mCrossProfileApps = mContext.getSystemService(CrossProfileApps.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        replaceEnterprisePreferenceScreenTitle(CONNECTED_WORK_AND_PERSONAL_APPS_TITLE,
                R.string.interact_across_profiles_title);

        final ArrayList<Pair<ApplicationInfo, UserHandle>> crossProfileApps =
                collectConfigurableApps(mPackageManager, mUserManager, mCrossProfileApps);

        final Context prefContext = getPrefContext();
        for (final Pair<ApplicationInfo, UserHandle> appData : crossProfileApps) {
            final ApplicationInfo appInfo = appData.first;
            final UserHandle user = appData.second;
            final String packageName = appInfo.packageName;
            final CharSequence label = appInfo.loadLabel(mPackageManager);

            final Preference pref = new AppPreference(prefContext);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(appInfo, user.getIdentifier()));
            pref.setTitle(mPackageManager.getUserBadgedLabel(label, user));
            pref.setSummary(InteractAcrossProfilesDetails.getPreferenceSummary(
                    prefContext, packageName));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(InteractAcrossProfilesDetails.class,
                            mDevicePolicyManager.getResources().getString(
                                    CONNECTED_WORK_AND_PERSONAL_APPS_TITLE,
                                    () -> getString(R.string.interact_across_profiles_title)),
                            packageName,
                            appInfo.uid,
                            InteractAcrossProfilesSettings.this/* source */,
                            -1/* request */,
                            getMetricsCategory());
                    return true;
                }
            });
            screen.addPreference(pref);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.interact_across_profiles_empty_text);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.interact_across_profiles;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INTERACT_ACROSS_PROFILES;
    }

    /**
     * @return the list of applications for the personal profile in the calling user's profile group
     * that can configure interact across profiles.
     */
    static ArrayList<Pair<ApplicationInfo, UserHandle>> collectConfigurableApps(
            PackageManager packageManager, UserManager userManager,
            CrossProfileApps crossProfileApps) {
        final UserHandle workProfile = getWorkProfile(userManager);
        if (workProfile == null) {
            return new ArrayList<>();
        }
        final UserHandle personalProfile = userManager.getProfileParent(workProfile);
        if (personalProfile == null) {
            return new ArrayList<>();
        }

        final ArrayList<Pair<ApplicationInfo, UserHandle>> apps = new ArrayList<>();
        for (PackageInfo packageInfo : getAllInstalledPackages(
                packageManager, personalProfile, workProfile)) {
            if (crossProfileApps.canUserAttemptToConfigureInteractAcrossProfiles(
                    packageInfo.packageName)) {
                apps.add(new Pair<>(packageInfo.applicationInfo, personalProfile));
            }
        }
        return apps;
    }

    private static List<PackageInfo> getAllInstalledPackages(
            PackageManager packageManager, UserHandle personalProfile, UserHandle workProfile) {
        List<PackageInfo> personalPackages = packageManager.getInstalledPackagesAsUser(
                /* flags= */ 0, personalProfile.getIdentifier());
        List<PackageInfo> workPackages = packageManager.getInstalledPackagesAsUser(
                /* flags= */ 0, workProfile.getIdentifier());
        List<PackageInfo> allPackages = new ArrayList<>(personalPackages);
        for (PackageInfo workPackage : workPackages) {
            if (allPackages.stream().noneMatch(
                    p -> workPackage.packageName.equals(p.packageName))) {
                allPackages.add(workPackage);
            }
        }
        return allPackages;
    }

    /**
     * @return the number of applications that can interact across profiles.
     */
    static int getNumberOfEnabledApps(
            Context context, PackageManager packageManager, UserManager userManager,
            CrossProfileApps crossProfileApps) {
        UserHandle workProfile = getWorkProfile(userManager);
        if (workProfile == null) {
            return 0;
        }
        UserHandle personalProfile = userManager.getProfileParent(workProfile);
        if (personalProfile == null) {
            return 0;
        }
        final ArrayList<Pair<ApplicationInfo, UserHandle>> apps =
                collectConfigurableApps(packageManager, userManager, crossProfileApps);
        apps.removeIf(
                app -> !InteractAcrossProfilesDetails.isInteractAcrossProfilesEnabled(
                        context, app.first.packageName)
                        || !crossProfileApps.canConfigureInteractAcrossProfiles(
                                app.first.packageName));
        return apps.size();
    }

    /**
     * Returns the work profile in the profile group of the calling user.
     * Returns null if not found.
     */
    @Nullable
    static UserHandle getWorkProfile(UserManager userManager) {
        for (UserInfo user : userManager.getProfiles(UserHandle.myUserId())) {
            if (userManager.isManagedProfile(user.id)) {
                return user.getUserHandle();
            }
        }
        return null;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.interact_across_profiles);
}
