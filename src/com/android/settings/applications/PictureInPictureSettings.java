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
package com.android.settings.applications;

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.ArrayMap;
import android.view.View;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.wrapper.ActivityInfoWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PictureInPictureSettings extends EmptyTextSettings {

    private static final String TAG = PictureInPictureSettings.class.getSimpleName();
    @VisibleForTesting
    static final List<String> IGNORE_PACKAGE_LIST = new ArrayList<>();
    static {
        IGNORE_PACKAGE_LIST.add("com.android.systemui");
    }

    private Context mContext;
    private PackageManager mPackageManager;

    /**
     * @return true if the package has any activities that declare that they support
     *         picture-in-picture.
     */
    static boolean checkPackageHasPictureInPictureActivities(String packageName,
            ActivityInfo[] activities) {
        ActivityInfoWrapper[] wrappedActivities = null;
        if (activities != null) {
            wrappedActivities = new ActivityInfoWrapper[activities.length];
            for (int i = 0; i < activities.length; i++) {
                wrappedActivities[i] = new ActivityInfoWrapper(activities[i]);
            }
        }
        return checkPackageHasPictureInPictureActivities(packageName, wrappedActivities);
    }

    /**
     * @return true if the package has any activities that declare that they support
     *         picture-in-picture.
     */
    @VisibleForTesting
    static boolean checkPackageHasPictureInPictureActivities(String packageName,
            ActivityInfoWrapper[] activities) {
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

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPackageManager = mContext.getPackageManager();
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(mContext));
    }

    @Override
    public void onResume() {
        super.onResume();

        // Clear the prefs
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        // Fetch the set of applications which have at least one activity that declare that they
        // support picture-in-picture
        final ArrayMap<String, Boolean> packageToState = new ArrayMap<>();
        final ArrayList<ApplicationInfo> pipApps = new ArrayList<>();
        final List<PackageInfo> installedPackages = mPackageManager.getInstalledPackagesAsUser(
                GET_ACTIVITIES, UserHandle.myUserId());
        for (PackageInfo packageInfo : installedPackages) {
            if (checkPackageHasPictureInPictureActivities(packageInfo.packageName,
                    packageInfo.activities)) {
                final String packageName = packageInfo.applicationInfo.packageName;
                final boolean state = PictureInPictureDetails.getEnterPipStateForPackage(
                        mContext, packageInfo.applicationInfo.uid, packageName);
                pipApps.add(packageInfo.applicationInfo);
                packageToState.put(packageName, state);
            }
        }
        Collections.sort(pipApps, new PackageItemInfo.DisplayNameComparator(mPackageManager));

        // Rebuild the list of prefs
        final Context prefContext = getPrefContext();
        for (final ApplicationInfo appInfo : pipApps) {
            final String packageName = appInfo.packageName;
            final CharSequence label = appInfo.loadLabel(mPackageManager);

            final Preference pref = new Preference(prefContext);
            pref.setIcon(appInfo.loadIcon(mPackageManager));
            pref.setTitle(label);
            pref.setSummary(PictureInPictureDetails.getPreferenceSummary(prefContext,
                    appInfo.uid, packageName));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(PictureInPictureDetails.class,
                            R.string.picture_in_picture_app_detail_title, packageName, appInfo.uid,
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
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_MANAGE_PICTURE_IN_PICTURE;
    }
}
