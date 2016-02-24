/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.UserManager;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultHomePreference extends AppListPreference {

    private final ArrayList<ComponentName> mAllHomeComponents = new ArrayList<>();
    private final IntentFilter mHomeFilter;

    public DefaultHomePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);
        refreshHomeOptions();
    }

    @Override
    public void performClick() {
        refreshHomeOptions();
        super.performClick();
    }

    @Override
    protected boolean persistString(String value) {
        if (value != null) {
            ComponentName component = ComponentName.unflattenFromString(value);
            getContext().getPackageManager().replacePreferredActivity(mHomeFilter,
                    IntentFilter.MATCH_CATEGORY_EMPTY,
                    mAllHomeComponents.toArray(new ComponentName[0]), component);
            setSummary(getEntry());
        }
        return super.persistString(value);
    }

    public void refreshHomeOptions() {
        final String myPkg = getContext().getPackageName();
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        PackageManager pm = getContext().getPackageManager();
        ComponentName currentDefaultHome  = pm.getHomeActivities(homeActivities);
        ArrayList<ComponentName> components = new ArrayList<>();
        mAllHomeComponents.clear();
        List<CharSequence> summaries = new ArrayList<>();

        boolean mustSupportManagedProfile = hasManagedProfile();
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mAllHomeComponents.add(activityName);
            if (info.packageName.equals(myPkg)) {
                continue;
            }
            components.add(activityName);
            if (mustSupportManagedProfile && !launcherHasManagedProfilesFeature(candidate, pm)) {
                summaries.add(getContext().getString(R.string.home_work_profile_not_supported));
            } else {
                summaries.add(null);
            }
        }
        setComponentNames(components.toArray(new ComponentName[0]), currentDefaultHome,
                summaries.toArray(new CharSequence[0]));
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo, PackageManager pm) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(
                    resolveInfo.activityInfo.packageName, 0 /* default flags */);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean hasManagedProfile() {
        UserManager userManager = getContext().getSystemService(UserManager.class);
        List<UserInfo> profiles = userManager.getProfiles(getContext().getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) return true;
        }
        return false;
    }

    public static boolean hasHomePreference(String pkg, Context context) {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        PackageManager pm = context.getPackageManager();
        pm.getHomeActivities(homeActivities);
        for (int i = 0; i < homeActivities.size(); i++) {
            if (homeActivities.get(i).activityInfo.packageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHomeDefault(String pkg, Context context) {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        PackageManager pm = context.getPackageManager();
        ComponentName def = pm.getHomeActivities(homeActivities);

        return def != null && def.getPackageName().equals(pkg);
    }
}
