/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.applications.specialaccess.pictureinpicture;

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ActivityInfo;
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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.AppPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SearchIndexable
public class PictureInPictureSettings extends EmptyTextSettings {

    @VisibleForTesting
    static final List<String> IGNORE_PACKAGE_LIST = new ArrayList<>();

    static {
        IGNORE_PACKAGE_LIST.add(Utils.SYSTEMUI_PACKAGE_NAME);
    }

    /**
     * Comparator by name, then user id.
     * {@see PackageItemInfo#DisplayNameComparator}
     */
    static class AppComparator implements Comparator<Pair<ApplicationInfo, Integer>> {

        private final Collator mCollator = Collator.getInstance();
        private final PackageManager mPm;

        public AppComparator(PackageManager pm) {
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

    private Context mContext;
    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private IconDrawableFactory mIconDrawableFactory;

    /**
     * @return true if the package has any activities that declare that they support
     * picture-in-picture.
     */

    public static boolean checkPackageHasPictureInPictureActivities(String packageName,
            ActivityInfo[] activities) {
        // Skip if it's in the ignored list
        if (IGNORE_PACKAGE_LIST.contains(packageName)) {
            return false;
        }

        // Iterate through all the activities and check if it is resizeable and supports
        // picture-in-picture
        if (activities != null) {
            for (int i = activities.length - 1; i >= 0; i--) {
                if (activities[i].supportsPictureInPicture()) {
                    return true;
                }
            }
        }
        return false;
    }

    public PictureInPictureSettings() {
        // Do nothing
    }

    public PictureInPictureSettings(PackageManager pm, UserManager um) {
        mPackageManager = pm;
        mUserManager = um;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPackageManager = mContext.getPackageManager();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Clear the prefs
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        // Fetch the set of applications for each profile which have at least one activity that
        // declare that they support picture-in-picture
        final ArrayList<Pair<ApplicationInfo, Integer>> pipApps =
                collectPipApps(UserHandle.myUserId());
        Collections.sort(pipApps, new AppComparator(mPackageManager));

        // Rebuild the list of prefs
        final Context prefContext = getPrefContext();
        for (final Pair<ApplicationInfo, Integer> appData : pipApps) {
            final ApplicationInfo appInfo = appData.first;
            final int userId = appData.second;
            final UserHandle user = UserHandle.of(userId);
            final String packageName = appInfo.packageName;
            final CharSequence label = appInfo.loadLabel(mPackageManager);

            final Preference pref = new AppPreference(prefContext);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(appInfo, userId));
            pref.setTitle(mPackageManager.getUserBadgedLabel(label, user));
            pref.setSummary(PictureInPictureDetails.getPreferenceSummary(prefContext,
                    appInfo.uid, packageName));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(PictureInPictureDetails.class,
                            getString(R.string.picture_in_picture_app_detail_title),
                            packageName, appInfo.uid,
                            PictureInPictureSettings.this, -1, getMetricsCategory());
                    return true;
                }
            });
            screen.addPreference(pref);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.picture_in_picture_empty_text);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.picture_in_picture_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_MANAGE_PICTURE_IN_PICTURE;
    }

    /**
     * @return the list of applications for the given user and all their profiles that have
     * activities which support PiP.
     */
    ArrayList<Pair<ApplicationInfo, Integer>> collectPipApps(int userId) {
        final ArrayList<Pair<ApplicationInfo, Integer>> pipApps = new ArrayList<>();
        final ArrayList<Integer> userIds = new ArrayList<>();
        for (UserInfo user : mUserManager.getProfiles(userId)) {
            userIds.add(user.id);
        }

        for (int id : userIds) {
            final List<PackageInfo> installedPackages = mPackageManager.getInstalledPackagesAsUser(
                    GET_ACTIVITIES, id);
            for (PackageInfo packageInfo : installedPackages) {
                if (checkPackageHasPictureInPictureActivities(packageInfo.packageName,
                        packageInfo.activities)) {
                    pipApps.add(new Pair<>(packageInfo.applicationInfo, id));
                }
            }
        }
        return pipApps;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.picture_in_picture_settings);
}
