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

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;

public class ApprovalPreferenceController extends BasePreferenceController {

    private static final String TAG = "ApprovalPrefController";

    private PackageInfo mPkgInfo;
    private ComponentName mCn;
    private PreferenceFragmentCompat mParent;
    private NotificationManager mNm;
    private PackageManager mPm;
    // The appOp representing this preference
    private String mSettingIdentifier;

    public ApprovalPreferenceController(Context context, String key) {
        super(context, key);
    }

    public ApprovalPreferenceController setPkgInfo(PackageInfo pkgInfo) {
        mPkgInfo = pkgInfo;
        return this;
    }

    public ApprovalPreferenceController setCn(ComponentName cn) {
        mCn = cn;
        return this;
    }

    public ApprovalPreferenceController setParent(PreferenceFragmentCompat parent) {
        mParent = parent;
        return this;
    }

    public ApprovalPreferenceController setNm(NotificationManager nm) {
        mNm = nm;
        return this;
    }

    public ApprovalPreferenceController setPm(PackageManager pm) {
        mPm = pm;
        return this;
    }

    /**
     * Set the associated appOp for the Setting
     */
    @NonNull
    public ApprovalPreferenceController setSettingIdentifier(@NonNull String settingIdentifier) {
        mSettingIdentifier = settingIdentifier;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference pref) {
        final RestrictedSwitchPreference preference =
                (RestrictedSwitchPreference) pref;
        final CharSequence label = mPkgInfo.applicationInfo.loadLabel(mPm);
        final boolean isAllowedCn = mCn.flattenToShortString().length()
                <= NotificationManager.MAX_SERVICE_COMPONENT_NAME_LENGTH;
        final boolean isEnabled = isServiceEnabled(mCn);
        preference.setChecked(isEnabled);
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            final boolean access = (Boolean) newValue;
            if (!access) {
                if (!isServiceEnabled(mCn)) {
                    return true; // already disabled
                }
                // show a friendly dialog
                new FriendlyWarningDialogFragment()
                        .setServiceInfo(mCn, label, mParent)
                        .show(mParent.getFragmentManager(), "friendlydialog");
                return false;
            } else {
                if (isServiceEnabled(mCn)) {
                    return true; // already enabled
                }
                // show a scary dialog
                new ScaryWarningDialogFragment()
                        .setServiceInfo(mCn, label, mParent)
                        .show(mParent.getFragmentManager(), "dialog");
                return false;
            }
        });

        if (android.permission.flags.Flags.enhancedConfirmationModeApisEnabled()
                && android.security.Flags.extendEcmToAllSettings()) {
            if (!isAllowedCn && !isEnabled) {
                preference.setEnabled(false);
            } else if (isEnabled) {
                preference.setEnabled(true);
            } else {
                preference.checkEcmRestrictionAndSetDisabled(mSettingIdentifier,
                        mCn.getPackageName());
            }
        } else {
            preference.updateState(
                    mCn.getPackageName(), mPkgInfo.applicationInfo.uid, isAllowedCn, isEnabled);
        }
    }

    public void disable(final ComponentName cn) {
        logSpecialPermissionChange(true, cn.getPackageName());
        mNm.setNotificationListenerAccessGranted(cn, false);
        AsyncTask.execute(() -> {
            if (!mNm.isNotificationPolicyAccessGrantedForPackage(
                    cn.getPackageName())) {
                if (android.app.Flags.modesApi()) {
                    mNm.removeAutomaticZenRules(cn.getPackageName(), /* fromUser= */ true);
                } else {
                    mNm.removeAutomaticZenRules(cn.getPackageName());
                }
            }
        });
    }

    protected void enable(ComponentName cn) {
        logSpecialPermissionChange(true, cn.getPackageName());
        mNm.setNotificationListenerAccessGranted(cn, true);
    }

    protected boolean isServiceEnabled(ComponentName cn) {
        return mNm.isNotificationListenerAccessGranted(cn);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean enable, String packageName) {
        final int logCategory = enable ? SettingsEnums.APP_SPECIAL_PERMISSION_NOTIVIEW_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_NOTIVIEW_DENY;
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(mContext,
                logCategory, packageName);
    }
}