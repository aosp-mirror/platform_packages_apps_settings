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

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import android.annotation.Nullable;
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

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.apppreference.AppPreference;

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
            pref.setSummary(InteractAcrossProfilesDetails.getPreferenceSummary(prefContext,
                    packageName, appInfo.uid));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(InteractAcrossProfilesDetails.class,
                            R.string.interact_across_profiles_title,
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
        final UserHandle personalProfile = getPersonalProfileForCallingUser(userManager);
        if (personalProfile == null) {
            return new ArrayList<>();
        }

        final ArrayList<Pair<ApplicationInfo, UserHandle>> apps = new ArrayList<>();
        final List<PackageInfo> installedPackages = packageManager.getInstalledPackagesAsUser(
                GET_ACTIVITIES, personalProfile.getIdentifier());
        for (PackageInfo packageInfo : installedPackages) {
            if (crossProfileApps.canConfigureInteractAcrossProfiles(packageInfo.packageName)) {
                apps.add(new Pair<>(packageInfo.applicationInfo, personalProfile));
            }
        }
        return apps;
    }

    /**
     * @return the number of applications that can interact across profiles.
     */
    static int getNumberOfEnabledApps(
            Context context, PackageManager packageManager, UserManager userManager,
            CrossProfileApps crossProfileApps) {
        final ArrayList<Pair<ApplicationInfo, UserHandle>> apps =
                collectConfigurableApps(packageManager, userManager, crossProfileApps);
        apps.removeIf(
                app -> !InteractAcrossProfilesDetails.isInteractAcrossProfilesEnabled(
                        context, app.first.packageName, app.first.uid));
        return apps.size();
    }

    /**
     * Returns the personal profile in the profile group of the calling user.
     * Returns null if user is not in a profile group.
     */
    @Nullable
    private static UserHandle getPersonalProfileForCallingUser(UserManager userManager) {
        final int callingUser = UserHandle.myUserId();
        if (userManager.getProfiles(callingUser).isEmpty()) {
            return null;
        }
        final UserInfo parentProfile = userManager.getProfileParent(callingUser);
        return parentProfile == null
                ? UserHandle.of(callingUser) : parentProfile.getUserHandle();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.interact_across_profiles);
}
