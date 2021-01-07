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

package com.android.settings.applications.specialaccess.notificationaccess;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.IconDrawableFactory;
import android.view.View;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

public class HeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver {

    private DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;
    private PackageInfo mPackageInfo;
    private PackageManager mPm;
    private CharSequence mServiceName;

    public HeaderPreferenceController(Context context, String key) {
        super(context, key);
    }

    public HeaderPreferenceController setFragment(DashboardFragment fragment) {
        mFragment = fragment;
        return this;
    }

    public HeaderPreferenceController setPackageInfo(PackageInfo packageInfo) {
        mPackageInfo = packageInfo;
        return this;
    }

    public HeaderPreferenceController setPm(PackageManager pm) {
        mPm = pm;
        return this;
    }

    public HeaderPreferenceController setServiceName(CharSequence serviceName) {
        mServiceName = serviceName;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mFragment == null) {
            return;
        }
        LayoutPreference pref = screen.findPreference(getPreferenceKey());
        mHeaderController = EntityHeaderController.newInstance(
                mFragment.getActivity(), mFragment, pref.findViewById(R.id.entity_header));
        pref = mHeaderController
                .setRecyclerView(mFragment.getListView(), mFragment.getSettingsLifecycle())
                .setIcon(IconDrawableFactory.newInstance(mFragment.getActivity())
                        .getBadgedIcon(mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setSummary(mServiceName)
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageInfo.packageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(mFragment.getActivity(), mContext);
        pref.findViewById(R.id.entity_header).setVisibility(View.VISIBLE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mHeaderController != null) {
            mHeaderController.styleActionBar(mFragment.getActivity());
        }
    }
}
