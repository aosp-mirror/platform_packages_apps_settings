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
package com.android.settings.applications;

import static com.android.settings.widget.EntityHeaderController.ActionType;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.IconDrawableFactory;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.LayoutPreference;

/**
 *  The header controller displays on the top of the page.
 */
public class AppHeaderPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnResume {
    private DashboardFragment mParent;
    private PackageInfo mPackageInfo;
    private Lifecycle mLifecycle;
    private LayoutPreference mHeaderPreference;

    public AppHeaderPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @param fragment set the parent fragment.
     * @return return controller-self.
     */
    public AppHeaderPreferenceController setParentFragment(DashboardFragment fragment) {
        mParent = fragment;
        return this;
    }

    /**
     * @param packageInfo set the {@link PackageInfo}.
     * @return return controller-self.
     */
    public AppHeaderPreferenceController setPackageInfo(PackageInfo packageInfo) {
        mPackageInfo = packageInfo;
        return this;
    }

    /**
     * @param lifeCycle set the {@link Lifecycle}.
     * @return return controller-self.
     */
    public AppHeaderPreferenceController setLifeCycle(Lifecycle lifeCycle) {
        mLifecycle = lifeCycle;
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHeaderPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onResume() {
        final Activity activity = mParent.getActivity();
        final PackageManager packageManager = activity.getPackageManager();
        EntityHeaderController
                .newInstance(activity, mParent, mHeaderPreference.findViewById(R.id.entity_header))
                .setRecyclerView(mParent.getListView(), mLifecycle)
                .setIcon(IconDrawableFactory.newInstance(activity).getBadgedIcon(
                        mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(packageManager))
                .setSummary(mPackageInfo)
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageInfo.packageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE)
                .done(mParent.getActivity(), true /* rebindActions */);
    }
}
