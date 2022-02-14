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

package com.android.settings.applications.appinfo;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_DEFAULT;
import static android.app.AppOpsManager.MODE_IGNORED;
import static android.app.AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;
import static com.android.settings.Utils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS;

import android.app.AppOpsManager;
import android.apphibernation.AppHibernationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Slog;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.google.common.annotations.VisibleForTesting;

/**
 * A PreferenceController handling the logic for exempting hibernation of app
 */
public final class HibernationSwitchPreferenceController extends AppInfoPreferenceControllerBase
        implements LifecycleObserver, AppOpsManager.OnOpChangedListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "HibernationSwitchPrefController";
    private String mPackageName;
    private final AppOpsManager mAppOpsManager;
    private int mPackageUid;
    @VisibleForTesting
    boolean mIsPackageSet;
    private boolean mIsPackageExemptByDefault;

    public HibernationSwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mIsPackageSet) {
            mAppOpsManager.startWatchingMode(
                    OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageName, this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mAppOpsManager.stopWatchingMode(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return isHibernationEnabled() && mIsPackageSet ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Set the package. And also retrieve details from package manager. Some packages may be
     * exempted from hibernation by default. This method should only be called to initialize the
     * controller.
     * @param packageName The name of the package whose hibernation state to be managed.
     */
    void setPackage(@NonNull String packageName) {
        mPackageName = packageName;
        final PackageManager packageManager = mContext.getPackageManager();

        // Q- packages exempt by default, except R- on Auto since Auto-Revoke was skipped in R
        final int maxTargetSdkVersionForExemptApps =
                packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
                        ? android.os.Build.VERSION_CODES.R
                        : android.os.Build.VERSION_CODES.Q;
        try {
            mPackageUid = packageManager.getPackageUid(packageName, /* flags */ 0);
            mIsPackageExemptByDefault =
                    hibernationTargetsPreSApps()
                            ? false
                            : packageManager.getTargetSdkVersion(packageName)
                                    <= maxTargetSdkVersionForExemptApps;
            mIsPackageSet = true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Package [" + mPackageName + "] is not found!");
            mIsPackageSet = false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((SwitchPreference) preference).setChecked(!isPackageHibernationExemptByUser());
    }

    @VisibleForTesting
    boolean isPackageHibernationExemptByUser() {
        if (!mIsPackageSet) return true;
        final int mode = mAppOpsManager.unsafeCheckOpNoThrow(
                OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageUid, mPackageName);

        return mode == MODE_DEFAULT ? mIsPackageExemptByDefault : mode != MODE_ALLOWED;
    }

    @Override
    public void onOpChanged(String op, String packageName) {
        if (OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED.equals(op)
                && TextUtils.equals(mPackageName, packageName)) {
            updateState(mPreference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object isChecked) {
        try {
            final boolean checked = (boolean) isChecked;
            mAppOpsManager.setUidMode(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageUid,
                    checked ? MODE_ALLOWED : MODE_IGNORED);
            if (!checked) {
                final AppHibernationManager ahm =
                        mContext.getSystemService(AppHibernationManager.class);
                ahm.setHibernatingForUser(mPackageName, false);
                ahm.setHibernatingGlobally(mPackageName, false);
            }
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    private static boolean isHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, true);
    }

    private static boolean hibernationTargetsPreSApps() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS, false);
    }
}
