/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateLongBackgroundTasksBridge;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

/**
 * Preference controller for
 * {@link LongBackgroundTasksDetails} Settings fragment.
 */
public class LongBackgroundTasksDetailsPreferenceController extends
        AppInfoPreferenceControllerBase {

    private final ApplicationFeatureProvider mAppFeatureProvider;

    private String mPackageName;

    public LongBackgroundTasksDetailsPreferenceController(Context context, String key) {
        super(context, key);
        mAppFeatureProvider = FeatureFactory.getFeatureFactory()
                .getApplicationFeatureProvider();
    }

    @VisibleForTesting
    LongBackgroundTasksDetailsPreferenceController(Context context, String key,
            ApplicationFeatureProvider appFeatureProvider) {
        super(context, key);
        mAppFeatureProvider = appFeatureProvider;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mAppFeatureProvider.isLongBackgroundTaskPermissionToggleSupported()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return isCandidate() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return LongBackgroundTasksDetails.class;
    }

    @VisibleForTesting
    CharSequence getPreferenceSummary() {
        return LongBackgroundTasksDetails.getSummary(mContext, mParent.getAppEntry());
    }

    @VisibleForTesting
    boolean isCandidate() {
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return false;
        }
        final AppStateLongBackgroundTasksBridge.LongBackgroundTasksState appState =
                new AppStateLongBackgroundTasksBridge(
                        mContext, /*appState=*/null, /*callback=*/null)
                        .createPermissionState(mPackageName, packageInfo.applicationInfo.uid);
        return appState.shouldBeVisible();
    }

    void setPackageName(String packageName) {
        mPackageName = packageName;
    }
}
