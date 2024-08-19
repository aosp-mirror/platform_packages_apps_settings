/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.EXTRA_AUTOMATIC_RULE_ID;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.notification.ConditionProviderService;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settingslib.notification.modes.ZenMode;

import java.util.List;
import java.util.function.Function;

class ConfigurationActivityHelper {

    private static final String TAG = "ConfigurationActivityHelper";

    private final PackageManager mPm;

    ConfigurationActivityHelper(PackageManager pm) {
        mPm = pm;
    }

    @Nullable
    Intent getConfigurationActivityIntentForMode(ZenMode zenMode,
            Function<ComponentName, ComponentInfo> approvedServiceFinder) {

        String owner = zenMode.getRule().getPackageName();
        ComponentName configActivity = null;
        if (zenMode.getRule().getConfigurationActivity() != null) {
            // If a configuration activity is present, use that directly in the intent
            configActivity = zenMode.getRule().getConfigurationActivity();
        } else {
            // Otherwise, look for a condition provider service for the rule's package
            ComponentInfo ci = approvedServiceFinder.apply(zenMode.getRule().getOwner());
            if (ci != null) {
                configActivity = extractConfigurationActivityFromComponent(ci);
            }
        }

        if (configActivity != null
                && (owner == null || isSameOwnerPackage(owner, configActivity))
                && isResolvableActivity(configActivity)) {
            return new Intent()
                    .setComponent(configActivity)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ConditionProviderService.EXTRA_RULE_ID, zenMode.getId())
                    .putExtra(EXTRA_AUTOMATIC_RULE_ID, zenMode.getId());
        } else {
            return null;
        }
    }

    @Nullable
    ComponentName getConfigurationActivityFromApprovedComponent(ComponentInfo ci) {
        ComponentName configActivity = extractConfigurationActivityFromComponent(ci);
        if (configActivity != null
                && isSameOwnerPackage(ci.packageName, configActivity)
                && isResolvableActivity(configActivity)) {
            return configActivity;
        } else {
            return null;
        }
    }

    /**
     * Extract the {@link ComponentName} corresponding to the mode configuration <em>activity</em>
     * from the component declaring the rule (which may be the Activity itself, or a CPS that points
     * to the activity in question in its metadata).
     *
     * <p>This method doesn't perform any validation, so the activity may or may not exist.
     */
    @Nullable
    private ComponentName extractConfigurationActivityFromComponent(ComponentInfo ci) {
        if (ci instanceof ActivityInfo) {
            // New (activity-backed) rule.
            return new ComponentName(ci.packageName, ci.name);
        } else if (ci.metaData != null) {
            // Old (service-backed) rule.
            final String configurationActivity = ci.metaData.getString(
                    ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
            if (configurationActivity != null) {
                return ComponentName.unflattenFromString(configurationActivity);
            }
        }
        return null;
    }

    /**
     * Verifies that the activity is the same package as the rule owner.
     */
    private boolean isSameOwnerPackage(String ownerPkg, ComponentName activityName) {
        try {
            int ownerUid = mPm.getPackageUid(ownerPkg, 0);
            int configActivityOwnerUid = mPm.getPackageUid(activityName.getPackageName(), 0);
            if (ownerUid == configActivityOwnerUid) {
                return true;
            } else {
                Log.w(TAG, String.format("Config activity (%s) not in owner package (%s)",
                        activityName, ownerPkg));
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to find config activity " + activityName);
            return false;
        }
    }

    /** Verifies that the activity exists and hasn't been disabled. */
    private boolean isResolvableActivity(ComponentName activityName) {
        Intent intent = new Intent().setComponent(activityName);
        List<ResolveInfo> results = mPm.queryIntentActivities(intent, /* flags= */ 0);

        if (intent.resolveActivity(mPm) == null || results.isEmpty()) {
            Log.w(TAG, "Cannot resolve: " + activityName);
            return false;
        }
        return true;
    }
}
