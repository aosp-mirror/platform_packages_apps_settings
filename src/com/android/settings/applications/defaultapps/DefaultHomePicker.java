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

package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultHomePicker extends DefaultAppPickerFragment {

    private String mPackageName;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPackageName = context.getPackageName();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_HOME_PICKER;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final boolean mustSupportManagedProfile = hasManagedProfile();
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> homeActivities = new ArrayList<>();
        mPm.getHomeActivities(homeActivities);

        for (ResolveInfo resolveInfo : homeActivities) {
            final ActivityInfo info = resolveInfo.activityInfo;
            final ComponentName activityName = new ComponentName(info.packageName, info.name);
            if (info.packageName.equals(mPackageName)) {
                continue;
            }

            final String summary;
            boolean enabled = true;
            if (mustSupportManagedProfile && !launcherHasManagedProfilesFeature(resolveInfo)) {
                summary = getContext().getString(R.string.home_work_profile_not_supported);
                enabled = false;
            } else {
                summary = null;
            }
            final DefaultAppInfo candidate =
                    new DefaultAppInfo(mPm, mUserId, activityName, summary, enabled);
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        final ComponentName currentDefaultHome = mPm.getHomeActivities(homeActivities);
        if (currentDefaultHome != null) {
            return currentDefaultHome.flattenToString();
        }
        return null;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (!TextUtils.isEmpty(key)) {
            final ComponentName component = ComponentName.unflattenFromString(key);
            final List<ResolveInfo> homeActivities = new ArrayList<>();
            mPm.getHomeActivities(homeActivities);
            final List<ComponentName> allComponents = new ArrayList<>();
            for (ResolveInfo info : homeActivities) {
                final ActivityInfo appInfo = info.activityInfo;
                ComponentName activityName = new ComponentName(appInfo.packageName, appInfo.name);
                allComponents.add(activityName);
            }
            mPm.replacePreferredActivity(
                    DefaultHomePreferenceController.HOME_FILTER,
                    IntentFilter.MATCH_CATEGORY_EMPTY,
                    allComponents.toArray(new ComponentName[0]),
                    component);
            return true;
        }
        return false;
    }

    private boolean hasManagedProfile() {
        final Context context = getContext();
        List<UserInfo> profiles = mUserManager.getProfiles(context.getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo) {
        try {
            ApplicationInfo appInfo = mPm.getPackageManager().getApplicationInfo(
                    resolveInfo.activityInfo.packageName, 0 /* default flags */);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= Build.VERSION_CODES.LOLLIPOP;
    }
}
