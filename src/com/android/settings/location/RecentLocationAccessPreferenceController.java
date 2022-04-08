/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.location;

import static java.util.concurrent.TimeUnit.DAYS;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.icu.text.RelativeDateTimeFormatter;
import android.provider.DeviceConfig;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.location.RecentLocationAccesses;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppEntitiesHeaderController;
import com.android.settingslib.widget.AppEntityInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

public class RecentLocationAccessPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {
    /** Key for the recent location apps dashboard */
    private static final String KEY_APPS_DASHBOARD = "apps_dashboard";
    private final RecentLocationAccesses mRecentLocationAccesses;
    private AppEntitiesHeaderController mController;
    private static final int MAXIMUM_APP_COUNT = 3;

    public RecentLocationAccessPreferenceController(Context context) {
        this(context, new RecentLocationAccesses(context));
    }

    @VisibleForTesting
    RecentLocationAccessPreferenceController(Context context,
            RecentLocationAccesses recentAccesses) {
        super(context);
        mRecentLocationAccesses = recentAccesses;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_APPS_DASHBOARD;
    }

    @Override
    public boolean isAvailable() {
        return Boolean.parseBoolean(
                DeviceConfig.getProperty(DeviceConfig.NAMESPACE_PRIVACY,
                        Utils.PROPERTY_PERMISSIONS_HUB_ENABLED));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(KEY_APPS_DASHBOARD);
        final View view = preference.findViewById(R.id.app_entities_header);
        mController = AppEntitiesHeaderController.newInstance(mContext, view)
                .setHeaderTitleRes(R.string.location_category_recent_location_access)
                .setHeaderDetailsRes(R.string.location_recent_location_access_view_details)
                .setHeaderEmptyRes(R.string.location_no_recent_accesses)
                .setHeaderDetailsClickListener((View v) -> {
                    final Intent intent = new Intent(Intent.ACTION_REVIEW_PERMISSION_USAGE);
                    intent.putExtra(Intent.EXTRA_PERMISSION_NAME,
                            Manifest.permission.ACCESS_FINE_LOCATION);
                    intent.putExtra(Intent.EXTRA_DURATION_MILLIS, DAYS.toMillis(1));
                    mContext.startActivity(intent);
                });
    }

    @Override
    public void updateState(Preference preference) {
        updateRecentApps();
    }

    private void updateRecentApps() {
        final List<RecentLocationAccesses.Access> recentLocationAccesses =
                mRecentLocationAccesses.getAppListSorted();
        if (recentLocationAccesses.size() > 0) {
            // Display the top 3 preferences to container in original order.
            int i = 0;
            for (; i < Math.min(recentLocationAccesses.size(), MAXIMUM_APP_COUNT); i++) {
                final RecentLocationAccesses.Access access = recentLocationAccesses.get(i);
                final AppEntityInfo appEntityInfo = new AppEntityInfo.Builder()
                        .setIcon(access.icon)
                        .setTitle(access.label)
                        .setSummary(StringUtil.formatRelativeTime(mContext,
                                System.currentTimeMillis() - access.accessFinishTime, false,
                                RelativeDateTimeFormatter.Style.SHORT))
                        .setOnClickListener((v) -> {
                            final Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSION);
                            intent.putExtra(Intent.EXTRA_PERMISSION_NAME,
                                    Manifest.permission.ACCESS_FINE_LOCATION);
                            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, access.packageName);
                            intent.putExtra(Intent.EXTRA_USER, access.userHandle);
                            mContext.startActivity(intent);
                        })
                        .build();
                mController.setAppEntity(i, appEntityInfo);
            }
            for (; i < MAXIMUM_APP_COUNT; i++) {
                mController.removeAppEntity(i);
            }
        }
        mController.apply();
    }
}
