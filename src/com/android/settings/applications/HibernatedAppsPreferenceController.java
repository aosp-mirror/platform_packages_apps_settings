/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;

import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.permission.PermissionControllerManager;
import android.provider.DeviceConfig;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.common.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A preference controller handling the logic for updating summary of hibernated apps.
 */
public final class HibernatedAppsPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "HibernatedAppsPrefController";
    private PreferenceScreen mScreen;
    private int mUnusedCount = 0;
    private boolean mLoadingUnusedApps;
    private boolean mLoadedUnusedCount;
    private final Executor mMainExecutor;

    public HibernatedAppsPreferenceController(Context context, String preferenceKey) {
        this(context, preferenceKey, context.getMainExecutor());
    }

    @VisibleForTesting
    HibernatedAppsPreferenceController(Context context, String preferenceKey,
            Executor mainExecutor) {
        super(context, preferenceKey);
        mMainExecutor = mainExecutor;
    }

    @Override
    public int getAvailabilityStatus() {
        return isHibernationEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        MessageFormat msgFormat = new MessageFormat(
                mContext.getResources().getString(R.string.unused_apps_summary),
                Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", mUnusedCount);
        return mLoadedUnusedCount
                ? msgFormat.format(arguments)
                : mContext.getResources().getString(R.string.summary_placeholder);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;

        Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setIntent(new Intent(Intent.ACTION_MANAGE_UNUSED_APPS)
                    .setPackage(mContext.getPackageManager().getPermissionControllerPackageName()));
        }
    }

    /**
     * On lifecycle resume event.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        updatePreference();
    }

    private void updatePreference() {
        if (mScreen == null) {
            return;
        }
        if (!mLoadingUnusedApps) {
            final PermissionControllerManager permController =
                    mContext.getSystemService(PermissionControllerManager.class);
            permController.getUnusedAppCount(mMainExecutor, unusedCount -> {
                mUnusedCount = unusedCount;
                mLoadingUnusedApps = false;
                mLoadedUnusedCount = true;
                Preference pref = mScreen.findPreference(mPreferenceKey);
                refreshSummary(pref);
            });
            mLoadingUnusedApps = true;
        }
    }

    private static boolean isHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, true);
    }
}
