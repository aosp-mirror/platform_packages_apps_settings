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
package com.android.settings.applications.specialaccess.interactacrossprofiles;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.PermissionChecker;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settingslib.widget.LayoutPreference;

public class InteractAcrossProfilesDetails extends AppInfoBase
        implements Preference.OnPreferenceClickListener {

    private static final String INTERACT_ACROSS_PROFILES_SETTINGS_SWITCH =
            "interact_across_profiles_settings_switch";
    private static final String INTERACT_ACROSS_PROFILES_HEADER = "interact_across_profiles_header";

    private Context mContext;
    private CrossProfileApps mCrossProfileApps;
    private UserManager mUserManager;
    private SwitchPreference mSwitchPref;
    private LayoutPreference mHeader;
    private PackageManager mPackageManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mCrossProfileApps = mContext.getSystemService(CrossProfileApps.class);
        mUserManager = mContext.getSystemService(UserManager.class);
        mPackageManager = mContext.getPackageManager();

        addPreferencesFromResource(R.xml.interact_across_profiles_permissions_details);
        mSwitchPref = findPreference(INTERACT_ACROSS_PROFILES_SETTINGS_SWITCH);
        mSwitchPref.setOnPreferenceClickListener(this);
        mHeader = findPreference(INTERACT_ACROSS_PROFILES_HEADER);

        // refreshUi checks that the user can still configure the appOp, return to the
        // previous page if it can't.
        if (!refreshUi()) {
            setIntentAndFinish(true/* appChanged */);
        }
        final UserHandle workProfile = getWorkProfile();
        final UserHandle personalProfile = mUserManager.getProfileParent(workProfile);
        addAppTitleAndIcons(personalProfile, workProfile);
    }

    private void addAppTitleAndIcons(UserHandle personalProfile, UserHandle workProfile) {
        final TextView title = mHeader.findViewById(R.id.entity_header_title);
        if (title != null) {
            final String appLabel = mPackageInfo.applicationInfo.loadLabel(
                    mPackageManager).toString();
            title.setText(appLabel);
        }

        final ImageView personalIconView = mHeader.findViewById(R.id.entity_header_icon_personal);
        if (personalIconView != null) {
            personalIconView.setImageDrawable(IconDrawableFactory.newInstance(mContext)
                    .getBadgedIcon(mPackageInfo.applicationInfo, personalProfile.getIdentifier()));
        }
        final ImageView workIconView2 = mHeader.findViewById(R.id.entity_header_icon_work);
        if (workIconView2 != null) {
            workIconView2.setImageDrawable(IconDrawableFactory.newInstance(mContext)
                    .getBadgedIcon(mPackageInfo.applicationInfo, workProfile.getIdentifier()));
        }
    }

    @Nullable
    private UserHandle getWorkProfile() {
        for (UserInfo user : mUserManager.getProfiles(UserHandle.myUserId())) {
            if (mUserManager.isManagedProfile(user.id)) {
                return user.getUserHandle();
            }
        }
        return null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference != mSwitchPref) {
            return false;
        }
        // refreshUi checks that the user can still configure the appOp, return to the
        // previous page if it can't.
        if (!refreshUi()) {
            setIntentAndFinish(true/* appChanged */);
        }
        if (isInteractAcrossProfilesEnabled()) {
            enableInteractAcrossProfiles(false);
            refreshUi();
            return true;
        }
        if (!isInteractAcrossProfilesEnabled()) {
            showConsentDialog();
        }
        return true;
    }

    private void showConsentDialog() {
        final String appLabel = mPackageInfo.applicationInfo.loadLabel(mPackageManager).toString();

        final View dialogView = getLayoutInflater().inflate(
                R.layout.interact_across_profiles_consent_dialog, null);

        final TextView dialogTitle = dialogView.findViewById(
                R.id.interact_across_profiles_consent_dialog_title);
        dialogTitle.setText(
                getString(R.string.interact_across_profiles_consent_dialog_title, appLabel));

        final TextView dialogSummary = dialogView.findViewById(
                R.id.interact_across_profiles_consent_dialog_summary);
        dialogSummary.setText(
                getString(R.string.interact_across_profiles_consent_dialog_summary, appLabel));

        final TextView appDataSummary = dialogView.findViewById(R.id.app_data_summary);
        appDataSummary.setText(getString(
                R.string.interact_across_profiles_consent_dialog_app_data_summary, appLabel));

        final TextView permissionsSummary = dialogView.findViewById(R.id.permissions_summary);
        permissionsSummary.setText(getString(
                R.string.interact_across_profiles_consent_dialog_permissions_summary, appLabel));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView)
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        enableInteractAcrossProfiles(true);
                        refreshUi();
                    }
                })
                .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        refreshUi();
                    }
                })
                .create().show();
    }

    private boolean isInteractAcrossProfilesEnabled() {
        return isInteractAcrossProfilesEnabled(
                mContext, mPackageName, mPackageInfo.applicationInfo.uid);
    }

    static boolean isInteractAcrossProfilesEnabled(Context context, String packageName, int uid) {
        return PermissionChecker.PERMISSION_GRANTED
                == PermissionChecker.checkPermissionForPreflight(
                        context,
                        Manifest.permission.INTERACT_ACROSS_PROFILES,
                        PermissionChecker.PID_UNKNOWN,
                        uid,
                        packageName);
    }

    private void enableInteractAcrossProfiles(boolean newState) {
        mCrossProfileApps.setInteractAcrossProfilesAppOp(
                mPackageName, newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * {@code packageName} is allowed to interact across profiles.
     */
    public static CharSequence getPreferenceSummary(Context context, String packageName, int uid) {
        return context.getString(isInteractAcrossProfilesEnabled(context, packageName, uid)
                ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        if (!mCrossProfileApps.canConfigureInteractAcrossProfiles(mPackageName)) {
            // Invalid app entry. Should not allow changing permission
            mSwitchPref.setEnabled(false);
            return false;
        }

        final ImageView horizontalArrowIcon = mHeader.findViewById(R.id.entity_header_swap_horiz);
        if (isInteractAcrossProfilesEnabled()) {
            mSwitchPref.setChecked(true);
            mSwitchPref.setTitle(R.string.interact_across_profiles_switch_enabled);
            if (horizontalArrowIcon != null) {
                horizontalArrowIcon.setImageDrawable(
                        mContext.getDrawable(R.drawable.ic_swap_horiz_blue));
            }
        } else {
            mSwitchPref.setChecked(false);
            mSwitchPref.setTitle(R.string.interact_across_profiles_switch_disabled);
            if (horizontalArrowIcon != null) {
                horizontalArrowIcon.setImageDrawable(
                        mContext.getDrawable(R.drawable.ic_swap_horiz_grey));
            }
        }
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INTERACT_ACROSS_PROFILES;
    }
}
