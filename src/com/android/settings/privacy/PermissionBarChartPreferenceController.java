/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static android.Manifest.permission_group.CAMERA;
import static android.Manifest.permission_group.LOCATION;
import static android.Manifest.permission_group.MICROPHONE;

import static java.util.concurrent.TimeUnit.DAYS;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.permission.PermissionControllerManager;
import android.permission.RuntimePermissionUsageInfo;
import android.provider.DeviceConfig;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.BarChartInfo;
import com.android.settingslib.widget.BarChartPreference;
import com.android.settingslib.widget.BarViewInfo;

import java.util.ArrayList;
import java.util.List;


public class PermissionBarChartPreferenceController extends BasePreferenceController implements
        PermissionControllerManager.OnPermissionUsageResultCallback, LifecycleObserver, OnCreate,
        OnStart, OnSaveInstanceState {

    private static final String TAG = "BarChartPreferenceCtl";
    private static final String KEY_PERMISSION_USAGE = "usage_infos";

    @VisibleForTesting
    List<RuntimePermissionUsageInfo> mOldUsageInfos;
    private PackageManager mPackageManager;
    private PrivacyDashboardFragment mParent;
    private BarChartPreference mBarChartPreference;

    public PermissionBarChartPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mOldUsageInfos = new ArrayList<>();
        mPackageManager = context.getPackageManager();
    }

    public void setFragment(PrivacyDashboardFragment fragment) {
        mParent = fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mOldUsageInfos = savedInstanceState.getParcelableArrayList(KEY_PERMISSION_USAGE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableList(KEY_PERMISSION_USAGE, mOldUsageInfos);
    }

    @Override
    public int getAvailabilityStatus() {
        return Boolean.parseBoolean(
                DeviceConfig.getProperty(DeviceConfig.NAMESPACE_PRIVACY,
                        com.android.settings.Utils.PROPERTY_PERMISSIONS_HUB_ENABLED)) ?
                AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBarChartPreference = screen.findPreference(getPreferenceKey());

        final BarChartInfo info = new BarChartInfo.Builder()
                .setTitle(R.string.permission_bar_chart_title)
                .setDetails(R.string.permission_bar_chart_details)
                .setEmptyText(R.string.permission_bar_chart_empty_text)
                .setDetailsOnClickListener((View v) -> {
                    final Intent intent = new Intent(Intent.ACTION_REVIEW_PERMISSION_USAGE);
                    intent.putExtra(Intent.EXTRA_DURATION_MILLIS, DAYS.toMillis(1));
                    mContext.startActivity(intent);
                })
                .build();

        mBarChartPreference.initializeBarChart(info);
        if (!mOldUsageInfos.isEmpty()) {
            mBarChartPreference.setBarViewInfos(createBarViews(mOldUsageInfos));
        }
    }

    @Override
    public void onStart() {
        if (!isAvailable()) {
            return;
        }

        // We don't hide chart when we have existing data.
        mBarChartPreference.updateLoadingState(mOldUsageInfos.isEmpty() /* isLoading */);
        // But we still need to hint user with progress bar that we are updating new usage data.
        mParent.setLoadingEnabled(true /* enabled */);
        retrievePermissionUsageData();
    }

    @Override
    public void onPermissionUsageResult(@NonNull List<RuntimePermissionUsageInfo> usageInfos) {
        usageInfos.sort((x, y) -> {
            int usageDiff = y.getAppAccessCount() - x.getAppAccessCount();
            if (usageDiff != 0) {
                return usageDiff;
            }
            String xName = x.getName();
            String yName = y.getName();
            if (xName.equals(LOCATION)) {
                return -1;
            } else if (yName.equals(LOCATION)) {
                return 1;
            } else if (xName.equals(MICROPHONE)) {
                return -1;
            } else if (yName.equals(MICROPHONE)) {
                return 1;
            } else if (xName.equals(CAMERA)) {
                return -1;
            } else if (yName.equals(CAMERA)) {
                return 1;
            }
            return x.getName().compareTo(y.getName());
        });

        // If the result is different, we need to update bar views.
        if (!areSamePermissionGroups(usageInfos)) {
            mBarChartPreference.setBarViewInfos(createBarViews(usageInfos));
            mOldUsageInfos = usageInfos;
        }

        mBarChartPreference.updateLoadingState(false /* isLoading */);
        mParent.setLoadingEnabled(false /* enabled */);
    }

    private void retrievePermissionUsageData() {
        mContext.getSystemService(PermissionControllerManager.class).getPermissionUsages(
                false /* countSystem */, (int) DAYS.toMillis(1),
                mContext.getMainExecutor() /* executor */, this /* callback */);
    }

    private BarViewInfo[] createBarViews(List<RuntimePermissionUsageInfo> usageInfos) {
        if (usageInfos.isEmpty()) {
            return null;
        }

        final BarViewInfo[] barViewInfos = new BarViewInfo[
                Math.min(BarChartPreference.MAXIMUM_BAR_VIEWS, usageInfos.size())];

        for (int index = 0; index < barViewInfos.length; index++) {
            final RuntimePermissionUsageInfo permissionGroupInfo = usageInfos.get(index);
            final int count = permissionGroupInfo.getAppAccessCount();
            final CharSequence permLabel = getPermissionGroupLabel(permissionGroupInfo.getName());

            barViewInfos[index] = new BarViewInfo(
                    getPermissionGroupIcon(permissionGroupInfo.getName()), count, permLabel,
                    mContext.getResources().getQuantityString(R.plurals.permission_bar_chart_label,
                            count, count), permLabel);

            // Set the click listener for each bar view.
            // The listener will navigate user to permission usage app.
            barViewInfos[index].setClickListener((View v) -> {
                final Intent intent = new Intent(Intent.ACTION_REVIEW_PERMISSION_USAGE);
                intent.putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, permissionGroupInfo.getName());
                intent.putExtra(Intent.EXTRA_DURATION_MILLIS, DAYS.toMillis(1));
                mContext.startActivity(intent);
            });
        }

        return barViewInfos;
    }

    private Drawable getPermissionGroupIcon(String permissionGroup) {
        Drawable icon = null;
        try {
            icon = mPackageManager.getPermissionGroupInfo(permissionGroup, 0)
                    .loadIcon(mPackageManager);
            icon.setTintList(Utils.getColorAttr(mContext, android.R.attr.textColorSecondary));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot find group icon for " + permissionGroup, e);
        }

        return icon;
    }

    private CharSequence getPermissionGroupLabel(String permissionGroup) {
        CharSequence label = null;
        try {
            label = mPackageManager.getPermissionGroupInfo(permissionGroup, 0)
                    .loadLabel(mPackageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot find group label for " + permissionGroup, e);
        }

        return label;
    }

    private boolean areSamePermissionGroups(List<RuntimePermissionUsageInfo> newUsageInfos) {
        if (newUsageInfos.size() != mOldUsageInfos.size()) {
            return false;
        }

        for (int index = 0; index < newUsageInfos.size(); index++) {
            final RuntimePermissionUsageInfo newInfo = newUsageInfos.get(index);
            final RuntimePermissionUsageInfo oldInfo = mOldUsageInfos.get(index);

            if (!newInfo.getName().equals(oldInfo.getName()) ||
                    newInfo.getAppAccessCount() != oldInfo.getAppAccessCount()) {
                return false;
            }
        }
        return true;
    }
}
