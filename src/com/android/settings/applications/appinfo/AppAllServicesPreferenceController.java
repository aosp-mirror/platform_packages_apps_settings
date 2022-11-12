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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Objects;

/**
 * Preference Controller for the "All Services" preference in the "App Info" page.
 */
public class AppAllServicesPreferenceController extends AppInfoPreferenceControllerBase {

    private static final String TAG = "AllServicesPrefControl";
    private static final String SUMMARY_METADATA_KEY = "app_features_preference_summary";

    private final PackageManager mPackageManager;

    private String mPackageName;

    public AppAllServicesPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        CharSequence summary = getStorageSummary();
        if (summary != null) {
            mPreference.setSummary(summary);
        }
    }

    @Nullable
    private CharSequence getStorageSummary() {
        ResolveInfo resolveInfo = getResolveInfo(PackageManager.GET_META_DATA);
        if (resolveInfo == null) {
            Log.d(TAG, "mResolveInfo is null.");
            return null;
        }
        final Bundle metaData = resolveInfo.activityInfo.metaData;
        if (metaData != null) {
            try {
                final Resources pkgRes = mPackageManager.getResourcesForActivity(
                        new ComponentName(mPackageName, resolveInfo.activityInfo.name));
                return pkgRes.getString(metaData.getInt(SUMMARY_METADATA_KEY));
            } catch (Resources.NotFoundException exception) {
                Log.d(TAG, "Resource not found for summary string.");
            } catch (PackageManager.NameNotFoundException exception) {
                Log.d(TAG, "Name of resource not found for summary string.");
            }
        }
        return null;
    }

    @Override
    public int getAvailabilityStatus() {
        if (canPackageHandleIntent() && isLocationProvider()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @VisibleForTesting
    boolean isLocationProvider() {
        return Objects.requireNonNull(
                mContext.getSystemService(LocationManager.class)).isProviderPackage(mPackageName);
    }

    @VisibleForTesting
    boolean canPackageHandleIntent() {
        return getResolveInfo(0) != null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            startAllServicesActivity();
            return true;
        }
        return false;
    }

    /**
     * Set the package name of the package for which the "All Services" activity needs to be shown.
     *
     * @param packageName Name of the package for which the services need to be shown.
     */
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    private void startAllServicesActivity() {
        final Intent featuresIntent = new Intent(Intent.ACTION_VIEW_APP_FEATURES);
        // This won't be null since the preference is only shown for packages that can handle the
        // intent.
        ResolveInfo resolveInfo = getResolveInfo(0);
        featuresIntent.setComponent(
                new ComponentName(mPackageName, resolveInfo.activityInfo.name));

        Activity activity = mParent.getActivity();
        try {
            if (activity != null) {
                activity.startActivity(featuresIntent);
            }
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "The app cannot handle android.intent.action.VIEW_APP_FEATURES");
        }
    }

    @Nullable
    private ResolveInfo getResolveInfo(int flags) {
        if (mPackageName == null) {
            return null;
        }
        final Intent featuresIntent = new Intent(Intent.ACTION_VIEW_APP_FEATURES);
        featuresIntent.setPackage(mPackageName);

        return mPackageManager.resolveActivity(featuresIntent, flags);
    }
}
