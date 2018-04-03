/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.IWebViewUpdateService;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AppActionButtonPreferenceController extends BasePreferenceController
        implements AppInfoDashboardFragment.Callback {

    private static final String TAG = "AppActionButtonControl";
    private static final String KEY_ACTION_BUTTONS = "action_buttons";

    @VisibleForTesting
    ActionButtonPreference mActionButtons;
    private final AppInfoDashboardFragment mParent;
    private final String mPackageName;
    private final HashSet<String> mHomePackages = new HashSet<>();
    private final ApplicationFeatureProvider mApplicationFeatureProvider;

    private int mUserId;
    private DevicePolicyManager mDpm;
    private UserManager mUserManager;
    private PackageManager mPm;

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean enabled = getResultCode() != Activity.RESULT_CANCELED;
            Log.d(TAG, "Got broadcast response: Restart status for "
                    + mParent.getAppEntry().info.packageName + " " + enabled);
            updateForceStopButton(enabled);
        }
    };

    public AppActionButtonPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName) {
        super(context, KEY_ACTION_BUTTONS);
        mParent = parent;
        mPackageName = packageName;
        mUserId = UserHandle.myUserId();
        mApplicationFeatureProvider = FeatureFactory.getFactory(context)
                .getApplicationFeatureProvider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AppUtils.isInstant(mParent.getPackageInfo().applicationInfo)
                ? DISABLED_FOR_USER : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mActionButtons = ((ActionButtonPreference) screen.findPreference(KEY_ACTION_BUTTONS))
                .setButton2Text(R.string.force_stop)
                .setButton2Positive(false)
                .setButton2Enabled(false);
    }

    @Override
    public void refreshUi() {
        if (mPm == null) {
            mPm = mContext.getPackageManager();
        }
        if (mDpm == null) {
            mDpm = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
        if (mUserManager == null) {
            mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        }
        final AppEntry appEntry = mParent.getAppEntry();
        final PackageInfo packageInfo = mParent.getPackageInfo();

        // Get list of "home" apps and trace through any meta-data references
        final List<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        mPm.getHomeActivities(homeActivities);
        mHomePackages.clear();
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo ri = homeActivities.get(i);
            final String activityPkg = ri.activityInfo.packageName;
            mHomePackages.add(activityPkg);

            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(metaPkg, activityPkg)) {
                    mHomePackages.add(metaPkg);
                }
            }
        }

        checkForceStop(appEntry, packageInfo);
        initUninstallButtons(appEntry, packageInfo);
    }

    @VisibleForTesting
    void initUninstallButtons(AppEntry appEntry, PackageInfo packageInfo) {
        final boolean isBundled = (appEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean enabled;
        if (isBundled) {
            enabled = handleDisableable(appEntry, packageInfo);
        } else {
            enabled = initUninstallButtonForUserApp();
        }
        // If this is a device admin, it can't be uninstalled or disabled.
        // We do this here so the text of the button is still set correctly.
        if (isBundled && mDpm.packageHasActiveAdmins(packageInfo.packageName)) {
            enabled = false;
        }

        // We don't allow uninstalling DO/PO on *any* users, because if it's a system app,
        // "uninstall" is actually "downgrade to the system version + disable", and "downgrade"
        // will clear data on all users.
        if (Utils.isProfileOrDeviceOwner(mUserManager, mDpm, packageInfo.packageName)) {
            enabled = false;
        }

        // Don't allow uninstalling the device provisioning package.
        if (Utils.isDeviceProvisioningPackage(mContext.getResources(), appEntry.info.packageName)) {
            enabled = false;
        }

        // If the uninstall intent is already queued, disable the uninstall button
        if (mDpm.isUninstallInQueue(mPackageName)) {
            enabled = false;
        }

        // Home apps need special handling.  Bundled ones we don't risk downgrading
        // because that can interfere with home-key resolution.  Furthermore, we
        // can't allow uninstallation of the only home app, and we don't want to
        // allow uninstallation of an explicitly preferred one -- the user can go
        // to Home settings and pick a different one, after which we'll permit
        // uninstallation of the now-not-default one.
        if (enabled && mHomePackages.contains(packageInfo.packageName)) {
            if (isBundled) {
                enabled = false;
            } else {
                ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
                ComponentName currentDefaultHome = mPm.getHomeActivities(homeActivities);
                if (currentDefaultHome == null) {
                    // No preferred default, so permit uninstall only when
                    // there is more than one candidate
                    enabled = (mHomePackages.size() > 1);
                } else {
                    // There is an explicit default home app -- forbid uninstall of
                    // that one, but permit it for installed-but-inactive ones.
                    enabled = !packageInfo.packageName.equals(currentDefaultHome.getPackageName());
                }
            }
        }

        if (RestrictedLockUtils.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_APPS_CONTROL, mUserId)) {
            enabled = false;
        }

        try {
            final IWebViewUpdateService webviewUpdateService =
                    IWebViewUpdateService.Stub.asInterface(
                            ServiceManager.getService("webviewupdate"));
            if (webviewUpdateService.isFallbackPackage(appEntry.info.packageName)) {
                enabled = false;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mActionButtons.setButton1Enabled(enabled);
        if (enabled) {
            // Register listener
            mActionButtons.setButton1OnClickListener(v -> mParent.handleUninstallButtonClick());
        }
    }

    @VisibleForTesting
    boolean initUninstallButtonForUserApp() {
        boolean enabled = true;
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                && mUserManager.getUsers().size() >= 2) {
            // When we have multiple users, there is a separate menu
            // to uninstall for all users.
            enabled = false;
        } else if (AppUtils.isInstant(packageInfo.applicationInfo)) {
            enabled = false;
            mActionButtons.setButton1Visible(false);
        }
        mActionButtons.setButton1Text(R.string.uninstall_text).setButton1Positive(false);
        return enabled;
    }

    @VisibleForTesting
    boolean handleDisableable(AppEntry appEntry, PackageInfo packageInfo) {
        boolean disableable = false;
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        if (mHomePackages.contains(appEntry.info.packageName)
                || Utils.isSystemPackage(mContext.getResources(), mPm, packageInfo)) {
            // Disable button for core system applications.
            mActionButtons
                    .setButton1Text(R.string.disable_text)
                    .setButton1Positive(false);
        } else if (appEntry.info.enabled && appEntry.info.enabledSetting
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            mActionButtons
                    .setButton1Text(R.string.disable_text)
                    .setButton1Positive(false);
            disableable = !mApplicationFeatureProvider.getKeepEnabledPackages()
                    .contains(appEntry.info.packageName);
        } else {
            mActionButtons
                    .setButton1Text(R.string.enable_text)
                    .setButton1Positive(true);
            disableable = true;
        }

        return disableable;
    }

    private void updateForceStopButton(boolean enabled) {
        final boolean disallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mActionButtons
                .setButton2Enabled(disallowedBySystem ? false : enabled)
                .setButton2OnClickListener(
                        disallowedBySystem ? null : v -> mParent.handleForceStopButtonClick());
    }

    void checkForceStop(AppEntry appEntry, PackageInfo packageInfo) {
        if (mDpm.packageHasActiveAdmins(packageInfo.packageName)) {
            // User can't force stop device admin.
            Log.w(TAG, "User can't force stop device admin");
            updateForceStopButton(false);
        } else if (mPm.isPackageStateProtected(packageInfo.packageName,
                UserHandle.getUserId(appEntry.info.uid))) {
            Log.w(TAG, "User can't force stop protected packages");
            updateForceStopButton(false);
        } else if (AppUtils.isInstant(packageInfo.applicationInfo)) {
            updateForceStopButton(false);
            mActionButtons.setButton2Visible(false);
        } else if ((appEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            Log.w(TAG, "App is not explicitly stopped");
            updateForceStopButton(true);
        } else {
            final Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", appEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] {appEntry.info.packageName});
            intent.putExtra(Intent.EXTRA_UID, appEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(appEntry.info.uid));
            Log.d(TAG, "Sending broadcast to query restart status for "
                    + appEntry.info.packageName);
            mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
    }

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                return mPm.checkSignatures(pkg1, pkg2) >= PackageManager.SIGNATURE_MATCH;
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
            }
        }
        return false;
    }

}
