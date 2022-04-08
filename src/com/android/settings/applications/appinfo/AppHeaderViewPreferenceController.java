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

package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.LayoutPreference;

public class AppHeaderViewPreferenceController extends BasePreferenceController
        implements AppInfoDashboardFragment.Callback, LifecycleObserver, OnStart {

    private static final String KEY_HEADER = "header_view";

    private LayoutPreference mHeader;
    private final AppInfoDashboardFragment mParent;
    private final String mPackageName;
    private final Lifecycle mLifecycle;

    private EntityHeaderController mEntityHeaderController;

    public AppHeaderViewPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName, Lifecycle lifecycle) {
        super(context, KEY_HEADER);
        mParent = parent;
        mPackageName = packageName;
        mLifecycle = lifecycle;
        if (mLifecycle != null) {
            mLifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHeader = screen.findPreference(KEY_HEADER);
        final Activity activity = mParent.getActivity();
        mEntityHeaderController = EntityHeaderController
                .newInstance(activity, mParent, mHeader.findViewById(R.id.entity_header))
                .setPackageName(mPackageName)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .bindHeaderButtons();
    }

    @Override
    public void onStart() {
        mEntityHeaderController
                .setRecyclerView(mParent.getListView(), mLifecycle)
                .styleActionBar(mParent.getActivity());
    }

    @Override
    public void refreshUi() {
        setAppLabelAndIcon(mParent.getPackageInfo(), mParent.getAppEntry());
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo, AppEntry appEntry) {
        final Activity activity = mParent.getActivity();
        final boolean isInstantApp = AppUtils.isInstant(pkgInfo.applicationInfo);
        mEntityHeaderController
                .setLabel(appEntry)
                .setIcon(appEntry)
                .setIsInstantApp(isInstantApp)
                .done(activity, false /* rebindActions */);
    }
}
