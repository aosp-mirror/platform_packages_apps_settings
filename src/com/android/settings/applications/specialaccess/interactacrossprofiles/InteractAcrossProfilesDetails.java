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

import static android.app.admin.DevicePolicyResources.Strings.Settings.APP_CAN_ACCESS_PERSONAL_DATA;
import static android.app.admin.DevicePolicyResources.Strings.Settings.APP_CAN_ACCESS_PERSONAL_PERMISSIONS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_APPS_SHARE_PERMISSIONS_AND_DATA;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_WORK_AND_PERSONAL_APPS_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECT_APPS_DIALOG_SUMMARY;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECT_APPS_DIALOG_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.HOW_TO_DISCONNECT_APPS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.INSTALL_IN_PERSONAL_PROFILE_TO_CONNECT_PROMPT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.INSTALL_IN_WORK_PROFILE_TO_CONNECT_PROMPT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ONLY_CONNECT_TRUSTED_APPS;
import static android.content.pm.PackageManager.MATCH_DIRECT_BOOT_AWARE;
import static android.content.pm.PackageManager.MATCH_DIRECT_BOOT_UNAWARE;
import static android.provider.Settings.ACTION_MANAGE_CROSS_PROFILE_ACCESS;
import static android.provider.Settings.Global.CONNECTED_APPS_ALLOWED_PACKAGES;
import static android.provider.Settings.Global.CONNECTED_APPS_DISALLOWED_PACKAGES;

import android.Manifest;
import android.annotation.UserIdInt;
import android.app.ActionBar;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.flags.Flags;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.PermissionChecker;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.stats.devicepolicy.DevicePolicyEnums;
import android.util.IconDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppStoreUtil;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

public class InteractAcrossProfilesDetails extends AppInfoBase
        implements Preference.OnPreferenceClickListener {

    private static final String INTERACT_ACROSS_PROFILES_SETTINGS_SWITCH =
            "interact_across_profiles_settings_switch";
    private static final String INTERACT_ACROSS_PROFILES_HEADER = "interact_across_profiles_header";
    public static final String INSTALL_APP_BANNER_KEY = "install_app_banner";
    public static final String INTERACT_ACROSS_PROFILE_EXTRA_SUMMARY_KEY =
            "interact_across_profiles_extra_summary";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    public static final String INTENT_KEY = "intent";
    private static final String TAG = "InteractAcrossProfilesDetails";

    private Context mContext;
    private CrossProfileApps mCrossProfileApps;
    private UserManager mUserManager;
    private RestrictedSwitchPreference mSwitchPref;
    private LayoutPreference mHeader;
    private CardPreference mInstallBanner;
    private PackageManager mPackageManager;
    private UserHandle mPersonalProfile;
    private UserHandle mWorkProfile;
    private boolean mInstalledInPersonal;
    private boolean mInstalledInWork;
    private String mAppLabel;
    private Intent mInstallAppIntent;
    private boolean mIsPageLaunchedByApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mCrossProfileApps = mContext.getSystemService(CrossProfileApps.class);
        mUserManager = mContext.getSystemService(UserManager.class);
        mPackageManager = mContext.getPackageManager();

        mWorkProfile = InteractAcrossProfilesSettings.getWorkProfile(mUserManager);
        mPersonalProfile = mUserManager.getProfileParent(mWorkProfile);
        mInstalledInWork = isPackageInstalled(mPackageName, mWorkProfile.getIdentifier());
        mInstalledInPersonal = isPackageInstalled(mPackageName, mPersonalProfile.getIdentifier());

        mAppLabel = mPackageInfo.applicationInfo.loadLabel(mPackageManager).toString();
        mInstallAppIntent = AppStoreUtil.getAppStoreLink(mContext, mPackageName);

        addPreferencesFromResource(R.xml.interact_across_profiles_permissions_details);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view =  super.onCreateView(inflater, container, savedInstanceState);

        replaceEnterprisePreferenceScreenTitle(CONNECTED_WORK_AND_PERSONAL_APPS_TITLE,
                R.string.interact_across_profiles_title);
        replaceEnterpriseStringSummary("interact_across_profiles_summary_1",
                CONNECTED_APPS_SHARE_PERMISSIONS_AND_DATA,
                R.string.interact_across_profiles_summary_1);
        replaceEnterpriseStringSummary("interact_across_profiles_summary_2",
                ONLY_CONNECT_TRUSTED_APPS,
                R.string.interact_across_profiles_summary_2);
        replaceEnterpriseStringSummary("interact_across_profiles_extra_summary",
                HOW_TO_DISCONNECT_APPS,
                R.string.interact_across_profiles_summary_3);

        mSwitchPref = findPreference(INTERACT_ACROSS_PROFILES_SETTINGS_SWITCH);
        mSwitchPref.setOnPreferenceClickListener(this);

        mHeader = findPreference(INTERACT_ACROSS_PROFILES_HEADER);

        mInstallBanner = findPreference(INSTALL_APP_BANNER_KEY);
        mInstallBanner.setOnPreferenceClickListener(this);

        mIsPageLaunchedByApp = launchedByApp();

        // refreshUi checks that the user can still configure the appOp, return to the
        // previous page if it can't.
        if (!refreshUi()) {
            setIntentAndFinish(true/* appChanged */);
        }
        addAppTitleAndIcons(mPersonalProfile, mWorkProfile);
        styleActionBar();
        maybeShowExtraSummary();
        logPageLaunchMetrics();

        return view;
    }

    private void maybeShowExtraSummary() {
        Preference extraSummary = findPreference(INTERACT_ACROSS_PROFILE_EXTRA_SUMMARY_KEY);
        if (extraSummary == null) {
            return;
        }
        extraSummary.setVisible(mIsPageLaunchedByApp);
    }

    private void logPageLaunchMetrics() {
        if (!mCrossProfileApps.canConfigureInteractAcrossProfiles(mPackageName)) {
            logNonConfigurableAppMetrics();
        }
        if (mIsPageLaunchedByApp) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_LAUNCHED_FROM_APP);
        } else {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_LAUNCHED_FROM_SETTINGS);
        }
    }

    private void logNonConfigurableAppMetrics() {
        if (!isCrossProfilePackageAllowlisted(mPackageName)) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_ADMIN_RESTRICTED);
            return;
        }
        if (mInstallBanner == null) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_MISSING_INSTALL_BANNER_INTENT);
        }
        if (!mInstalledInPersonal) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_MISSING_PERSONAL_APP);
            return;
        }
        if (!mInstalledInWork) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_MISSING_WORK_APP);
        }
    }

    private void logEvent(int eventId) {
        DevicePolicyEventLogger.createEvent(eventId)
                .setStrings(mPackageName)
                .setInt(UserHandle.myUserId())
                .setAdmin(RestrictedLockUtils.getProfileOrDeviceOwner(
                        mContext, mWorkProfile).component)
                .write();
    }

    private void addAppTitleAndIcons(UserHandle personalProfile, UserHandle workProfile) {
        final TextView title = mHeader.findViewById(R.id.entity_header_title);
        if (title != null) {
            final String appLabel = mPackageInfo.applicationInfo.loadLabel(
                    mPackageManager).toString();
            title.setText(appLabel);
        }

        final ImageView personalIconView = mHeader.findViewById(
                com.android.settingslib.widget.preference.layout.R.id.entity_header_icon_personal);
        if (personalIconView != null) {
            Drawable icon = IconDrawableFactory.newInstance(mContext)
                    .getBadgedIcon(mPackageInfo.applicationInfo, personalProfile.getIdentifier())
                    .mutate();
            if (!mInstalledInPersonal) {
                icon.setColorFilter(createSuspendedColorMatrix());
            }
            personalIconView.setImageDrawable(icon);
        }

        final ImageView workIconView = mHeader.findViewById(
                com.android.settingslib.widget.preference.layout.R.id.entity_header_icon_work);
        if (workIconView != null) {
            Drawable icon = IconDrawableFactory.newInstance(mContext)
                    .getBadgedIcon(mPackageInfo.applicationInfo, workProfile.getIdentifier())
                    .mutate();
            if (!mInstalledInWork) {
                icon.setColorFilter(createSuspendedColorMatrix());
            }
            workIconView.setImageDrawable(icon);
        }
    }

    private void styleActionBar() {
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }

    private ColorMatrixColorFilter createSuspendedColorMatrix() {
        int grayValue = 127;
        float scale = 0.5f; // half bright

        ColorMatrix tempBrightnessMatrix = new ColorMatrix();
        float[] mat = tempBrightnessMatrix.getArray();
        mat[0] = scale;
        mat[6] = scale;
        mat[12] = scale;
        mat[4] = grayValue;
        mat[9] = grayValue;
        mat[14] = grayValue;

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.0f);
        matrix.preConcat(tempBrightnessMatrix);
        return new ColorMatrixColorFilter(matrix);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // refreshUi checks that the user can still configure the appOp, return to the
        // previous page if it can't.
        if (!refreshUi()) {
            setIntentAndFinish(true/* appChanged */);
        }
        if (preference == mSwitchPref) {
            handleSwitchPreferenceClick();
            return true;
        }
        if (preference == mInstallBanner) {
            handleInstallBannerClick();
            return true;
        }
        return false;
    }

    private void handleSwitchPreferenceClick() {
        if (isInteractAcrossProfilesEnabled()) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_PERMISSION_REVOKED);
            enableInteractAcrossProfiles(false);
            refreshUi();
        } else {
            showConsentDialog();
        }
    }

    private void showConsentDialog() {
        final View dialogView = getLayoutInflater().inflate(
                R.layout.interact_across_profiles_consent_dialog, null);

        final TextView dialogTitle = dialogView.findViewById(
                R.id.interact_across_profiles_consent_dialog_title);
        dialogTitle.setText(mDpm.getResources().getString(CONNECT_APPS_DIALOG_TITLE, () ->
                getString(R.string.interact_across_profiles_consent_dialog_title, mAppLabel),
                mAppLabel));

        final TextView appDataSummary = dialogView.findViewById(R.id.app_data_summary);
        appDataSummary.setText(
                mDpm.getResources().getString(APP_CAN_ACCESS_PERSONAL_DATA,
                        () -> getString(
                                R.string.interact_across_profiles_consent_dialog_app_data_summary,
                                mAppLabel), mAppLabel));

        final TextView permissionsSummary = dialogView.findViewById(R.id.permissions_summary);
        permissionsSummary.setText(mDpm.getResources().getString(
                APP_CAN_ACCESS_PERSONAL_PERMISSIONS,
                () -> getString(
                        R.string.interact_across_profiles_consent_dialog_permissions_summary,
                        mAppLabel),
                mAppLabel));

        final TextView dialogSummary =
                dialogView.findViewById(R.id.interact_across_profiles_consent_dialog_summary);
        dialogSummary.setText(mDpm.getResources().getString(CONNECT_APPS_DIALOG_SUMMARY,
                () -> getString(
                        R.string.interact_across_profiles_consent_dialog_summary)));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView)
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_USER_CONSENTED);
                        enableInteractAcrossProfiles(true);
                        refreshUi();
                        if (mIsPageLaunchedByApp) {
                            setIntentAndFinish(/* appChanged= */ true);
                        }
                    }
                })
                .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logEvent(
                                DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_USER_DECLINED_CONSENT);
                        refreshUi();
                    }
                })
                .create().show();
    }

    private boolean isInteractAcrossProfilesEnabled() {
        return isInteractAcrossProfilesEnabled(mContext, mPackageName);
    }

    static boolean isInteractAcrossProfilesEnabled(
            Context context, String packageName) {
        UserManager userManager = context.getSystemService(UserManager.class);
        UserHandle workProfile = InteractAcrossProfilesSettings.getWorkProfile(userManager);
        if (workProfile == null) {
            return false;
        }
        UserHandle personalProfile = userManager.getProfileParent(workProfile);
        return context.getSystemService(
                CrossProfileApps.class).canConfigureInteractAcrossProfiles(packageName)
                && isInteractAcrossProfilesEnabledInProfile(context, packageName, personalProfile)
                && isInteractAcrossProfilesEnabledInProfile(context, packageName, workProfile);

    }

    private static boolean isInteractAcrossProfilesEnabledInProfile(
            Context context, String packageName, UserHandle userHandle) {
        final PackageManager packageManager = context.getPackageManager();
        final int uid;
        try {
            uid = packageManager.getApplicationInfoAsUser(
                    packageName, /* flags= */0, userHandle).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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
        setUserPreferenceForPackage(newState, mPackageName);
    }

    private void handleInstallBannerClick() {
        if (mInstallAppIntent != null
                && !mInstalledInWork
                && isInstallableInProfile(mInstallAppIntent, mWorkProfile)) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_INSTALL_BANNER_CLICKED);
            mContext.startActivityAsUser(mInstallAppIntent, mWorkProfile);
            return;
        }
        if (mInstallAppIntent != null
                && !mInstalledInPersonal
                && isInstallableInProfile(mInstallAppIntent, mPersonalProfile)) {
            logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_INSTALL_BANNER_CLICKED);
            mContext.startActivityAsUser(mInstallAppIntent, mPersonalProfile);
            return;
        }
        logEvent(DevicePolicyEnums.CROSS_PROFILE_SETTINGS_PAGE_INSTALL_BANNER_NO_INTENT_CLICKED);
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * {@code packageName} is allowed to interact across profiles.
     */
    public static String getPreferenceSummary(Context context, String packageName) {
        return context.getString(isInteractAcrossProfilesEnabled(context, packageName)
                ? R.string.interact_across_profiles_summary_allowed
                : R.string.interact_across_profiles_summary_not_allowed);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        if (!mCrossProfileApps.canUserAttemptToConfigureInteractAcrossProfiles(mPackageName)) {
            // Invalid app entry. Should not allow changing permission
            mSwitchPref.setEnabled(false);
            return false;
        }
        if (!mCrossProfileApps.canConfigureInteractAcrossProfiles(mPackageName)) {
            return refreshUiForNonConfigurableApps();
        }
        refreshUiForConfigurableApps();
        return true;
    }

    private boolean refreshUiForNonConfigurableApps() {
        mSwitchPref.setChecked(false);
        mSwitchPref.setTitle(R.string.interact_across_profiles_switch_disabled);
        if (!isCrossProfilePackageAllowlisted(mPackageName)) {
            mInstallBanner.setVisible(false);
            mSwitchPref.setDisabledByAdmin(RestrictedLockUtils.getProfileOrDeviceOwner(
                    mContext, mWorkProfile));
            return true;
        }
        mSwitchPref.setEnabled(false);
        if (!mInstalledInPersonal && !mInstalledInWork) {
            return false;
        }
        if (!mInstalledInPersonal) {
            mInstallBanner.setTitle(
                    mDpm.getResources().getString(INSTALL_IN_PERSONAL_PROFILE_TO_CONNECT_PROMPT,
                            () -> getString(
                                    R.string.interact_across_profiles_install_personal_app_title,
                                    mAppLabel),
                            mAppLabel));
            if (mInstallAppIntent != null
                    && isInstallableInProfile(mInstallAppIntent, mPersonalProfile)) {
                mInstallBanner.setSummary(
                        R.string.interact_across_profiles_install_app_summary);
            }
            mInstallBanner.setVisible(true);
            return true;
        }
        if (!mInstalledInWork) {
            mInstallBanner.setTitle(
                    mDpm.getResources().getString(INSTALL_IN_WORK_PROFILE_TO_CONNECT_PROMPT,
                            () -> getString(
                                    R.string.interact_across_profiles_install_work_app_title,
                                    mAppLabel),
                            mAppLabel));
            if (mInstallAppIntent != null
                    && isInstallableInProfile(mInstallAppIntent, mWorkProfile)) {
                mInstallBanner.setSummary(
                        R.string.interact_across_profiles_install_app_summary);
            }
            mInstallBanner.setVisible(true);
            return true;
        }
        return false;
    }

    private boolean isCrossProfilePackageAllowlisted(String packageName) {
        return mContext.getSystemService(DevicePolicyManager.class)
                .getAllCrossProfilePackages().contains(packageName);
    }

    private boolean isPackageInstalled(String packageName, @UserIdInt int userId) {
        final PackageInfo info;
        try {
            info = mContext.createContextAsUser(UserHandle.of(userId), /* flags= */0)
                    .getPackageManager().getPackageInfo(packageName,
                            MATCH_DIRECT_BOOT_AWARE | MATCH_DIRECT_BOOT_UNAWARE);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return info != null;
    }

    private boolean isInstallableInProfile(Intent intent, UserHandle profile) {
        return !mContext.getPackageManager()
                .queryIntentActivitiesAsUser(intent, /* flags= */ 0, profile)
                .isEmpty();
    }

    private void refreshUiForConfigurableApps() {
        mInstallBanner.setVisible(false);
        mSwitchPref.setEnabled(true);
        if (isInteractAcrossProfilesEnabled()) {
            enableSwitchPref();
        } else {
            disableSwitchPref();
        }
    }

    private void enableSwitchPref() {
        mSwitchPref.setChecked(true);
        mSwitchPref.setTitle(R.string.interact_across_profiles_switch_enabled);
        final ImageView horizontalArrowIcon =
                mHeader.findViewById(com.android.settingslib.widget.preference.layout.R.id.entity_header_swap_horiz);
        if (horizontalArrowIcon != null) {
            horizontalArrowIcon.setImageDrawable(
                    mContext.getDrawable(
                            com.android.settingslib.widget.preference.layout.R.drawable.ic_swap_horiz_blue));
        }
    }

    private void disableSwitchPref() {
        mSwitchPref.setChecked(false);
        mSwitchPref.setTitle(R.string.interact_across_profiles_switch_disabled);
        final ImageView horizontalArrowIcon =
                mHeader.findViewById(com.android.settingslib.widget.preference.layout.R.id.entity_header_swap_horiz);
        if (horizontalArrowIcon != null) {
            horizontalArrowIcon.setImageDrawable(
                    mContext.getDrawable(
                            com.android.settingslib.widget.preference.layout.R.drawable.ic_swap_horiz_grey));
        }
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INTERACT_ACROSS_PROFILES;
    }

    private boolean launchedByApp() {
        final Bundle bundle = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGS);
        if (bundle == null) {
            return false;
        }
        final Intent intent = (Intent) bundle.get(INTENT_KEY);
        if (intent == null) {
            return false;
        }
        return ACTION_MANAGE_CROSS_PROFILE_ACCESS.equals(intent.getAction());
    }

    private void setUserPreferenceForPackage(boolean enabled, String crossProfilePackage) {
        if (!Flags.backupConnectedAppsSettings()) {
            return;
        }
        String allowedPackagesString = Settings.Global.getString(getContentResolver(),
                CONNECTED_APPS_ALLOWED_PACKAGES);
        String disallowedPackagesString = Settings.Global.getString(getContentResolver(),
                CONNECTED_APPS_DISALLOWED_PACKAGES);

        HashSet<String> allowedPackagesSet = getSetFromString(allowedPackagesString);
        HashSet<String> disallowedPackagesSet = getSetFromString(disallowedPackagesString);

        if (enabled) {
            allowedPackagesSet.add(crossProfilePackage);
            disallowedPackagesSet.remove(crossProfilePackage);

        } else {
            allowedPackagesSet.remove(crossProfilePackage);
            disallowedPackagesSet.add(crossProfilePackage);
        }

        Settings.Global.putString(getContentResolver(),
                CONNECTED_APPS_ALLOWED_PACKAGES,
                String.join(",", allowedPackagesSet));

        Settings.Global.putString(getContentResolver(),
                CONNECTED_APPS_DISALLOWED_PACKAGES,
                String.join(",", disallowedPackagesSet));
    }

    private HashSet<String> getSetFromString(String packages) {
        return Optional.ofNullable(packages)
                .map(pkg -> new HashSet<>(Arrays.asList(pkg.split(","))))
                .orElseGet(HashSet::new);
    }
}
