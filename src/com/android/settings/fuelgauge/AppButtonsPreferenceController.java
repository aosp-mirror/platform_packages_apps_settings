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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.webkit.IWebViewUpdateService;
import android.widget.Button;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Controller to control the uninstall button and forcestop button. All fragments that use
 * this controller should implement {@link ButtonActionDialogFragment.AppButtonsDialogListener} and
 * handle {@link Fragment#onActivityResult(int, int, Intent)}
 *
 * An easy way to handle them is to delegate them to {@link #handleDialogClick(int)} and
 * {@link #handleActivityResult(int, int, Intent)} in this controller.
 */
//TODO(b/35810915): Make InstalledAppDetails use this controller
public class AppButtonsPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause, OnDestroy,
        View.OnClickListener, ApplicationsState.Callbacks {
    public static final String APP_CHG = "chg";

    private static final String TAG = "AppButtonsPrefCtl";
    private static final String KEY_ACTION_BUTTONS = "action_buttons";
    private static final boolean LOCAL_LOGV = false;

    @VisibleForTesting
    final HashSet<String> mHomePackages = new HashSet<>();
    @VisibleForTesting
    ApplicationsState mState;
    @VisibleForTesting
    ApplicationsState.AppEntry mAppEntry;
    @VisibleForTesting
    PackageInfo mPackageInfo;
    @VisibleForTesting
    Button mForceStopButton;
    @VisibleForTesting
    Button mUninstallButton;
    @VisibleForTesting
    String mPackageName;
    @VisibleForTesting
    boolean mDisableAfterUninstall = false;

    private final int mRequestUninstall;
    private final int mRequestRemoveDeviceAdmin;

    private ApplicationsState.Session mSession;
    private DevicePolicyManagerWrapper mDpm;
    private UserManager mUserManager;
    private PackageManager mPm;
    private SettingsActivity mActivity;
    private Fragment mFragment;
    private RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private LayoutPreference mButtonsPref;
    private int mUserId;
    private boolean mUpdatedSysApp = false;
    private boolean mListeningToPackageRemove = false;
    private boolean mFinishing = false;
    private boolean mAppsControlDisallowedBySystem;

    public AppButtonsPreferenceController(SettingsActivity activity, Fragment fragment,
            Lifecycle lifecycle, String packageName, ApplicationsState state,
            DevicePolicyManagerWrapper dpm, UserManager userManager,
            PackageManager packageManager, int requestUninstall, int requestRemoveDeviceAdmin) {
        super(activity);

        if (!(fragment instanceof ButtonActionDialogFragment.AppButtonsDialogListener)) {
            throw new IllegalArgumentException(
                    "Fragment should implement AppButtonsDialogListener");
        }

        mMetricsFeatureProvider = FeatureFactory.getFactory(activity).getMetricsFeatureProvider();

        mState = state;
        mDpm = dpm;
        mUserManager = userManager;
        mPm = packageManager;
        mPackageName = packageName;
        mActivity = activity;
        mFragment = fragment;
        mUserId = UserHandle.myUserId();
        mRequestUninstall = requestUninstall;
        mRequestRemoveDeviceAdmin = requestRemoveDeviceAdmin;

        if (packageName != null) {
            mAppEntry = mState.getEntry(packageName, mUserId);
            mSession = mState.newSession(this);
            lifecycle.addObserver(this);
        } else {
            mFinishing = true;
        }
    }

    @Override
    public boolean isAvailable() {
        // TODO(b/37313605): Re-enable once this controller supports instant apps
        return mAppEntry != null && !AppUtils.isInstant(mAppEntry.info);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mButtonsPref = (LayoutPreference) screen.findPreference(KEY_ACTION_BUTTONS);

            mUninstallButton = (Button) mButtonsPref.findViewById(R.id.left_button);
            mUninstallButton.setText(R.string.uninstall_text);

            mForceStopButton = (Button) mButtonsPref.findViewById(R.id.right_button);
            mForceStopButton.setText(R.string.force_stop);
            mForceStopButton.setEnabled(false);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ACTION_BUTTONS;
    }

    @Override
    public void onResume() {
        mSession.resume();
        if (isAvailable() && !mFinishing) {
            mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(mActivity,
                    UserManager.DISALLOW_APPS_CONTROL, mUserId);
            mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(mActivity,
                    UserManager.DISALLOW_APPS_CONTROL, mUserId);

            if (!refreshUi()) {
                setIntentAndFinish(true);
            }
        }
    }

    @Override
    public void onPause() {
        mSession.pause();
    }

    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        mSession.release();
    }

    @Override
    public void onClick(View v) {
        final String packageName = mAppEntry.info.packageName;
        final int id = v.getId();
        if (id == R.id.left_button) {
            // Uninstall
            if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
                stopListeningToPackageRemove();
                Intent uninstallDaIntent = new Intent(mActivity, DeviceAdminAdd.class);
                uninstallDaIntent.putExtra(DeviceAdminAdd.EXTRA_DEVICE_ADMIN_PACKAGE_NAME,
                        packageName);
                mMetricsFeatureProvider.action(mActivity,
                        MetricsProto.MetricsEvent.ACTION_SETTINGS_UNINSTALL_DEVICE_ADMIN);
                mFragment.startActivityForResult(uninstallDaIntent, mRequestRemoveDeviceAdmin);
                return;
            }
            RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtils.checkIfUninstallBlocked(mActivity,
                            packageName, mUserId);
            boolean uninstallBlockedBySystem = mAppsControlDisallowedBySystem ||
                    RestrictedLockUtils.hasBaseUserRestriction(mActivity, packageName, mUserId);
            if (admin != null && !uninstallBlockedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mActivity, admin);
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
                    // If the system app has an update and this is the only user on the device,
                    // then offer to downgrade the app, otherwise only offer to disable the
                    // app for this user.
                    if (mUpdatedSysApp && isSingleUser()) {
                        showDialogInner(ButtonActionDialogFragment.DialogType.SPECIAL_DISABLE);
                    } else {
                        showDialogInner(ButtonActionDialogFragment.DialogType.DISABLE);
                    }
                } else {
                    mMetricsFeatureProvider.action(
                            mActivity,
                            mAppEntry.info.enabled
                                    ? MetricsProto.MetricsEvent.ACTION_SETTINGS_DISABLE_APP
                                    : MetricsProto.MetricsEvent.ACTION_SETTINGS_ENABLE_APP);
                    AsyncTask.execute(new DisableChangerRunnable(mPm, mAppEntry.info.packageName,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT));
                }
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                uninstallPkg(packageName, true, false);
            } else {
                uninstallPkg(packageName, false, false);
            }
        } else if (id == R.id.right_button) {
            // force stop
            if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        mActivity, mAppsControlDisallowedAdmin);
            } else {
                showDialogInner(ButtonActionDialogFragment.DialogType.FORCE_STOP);
            }
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mRequestUninstall) {
            if (mDisableAfterUninstall) {
                mDisableAfterUninstall = false;
                AsyncTask.execute(new DisableChangerRunnable(mPm, mAppEntry.info.packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER));
            }
            refreshAndFinishIfPossible();
        } else if (requestCode == mRequestRemoveDeviceAdmin) {
            refreshAndFinishIfPossible();
        }
    }

    public void handleDialogClick(int id) {
        switch (id) {
            case ButtonActionDialogFragment.DialogType.DISABLE:
                mMetricsFeatureProvider.action(mActivity,
                        MetricsProto.MetricsEvent.ACTION_SETTINGS_DISABLE_APP);
                AsyncTask.execute(new DisableChangerRunnable(mPm, mAppEntry.info.packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER));
                break;
            case ButtonActionDialogFragment.DialogType.SPECIAL_DISABLE:
                mMetricsFeatureProvider.action(mActivity,
                        MetricsProto.MetricsEvent.ACTION_SETTINGS_DISABLE_APP);
                uninstallPkg(mAppEntry.info.packageName, false, true);
                break;
            case ButtonActionDialogFragment.DialogType.FORCE_STOP:
                forceStopPackage(mAppEntry.info.packageName);
                break;
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {
        refreshUi();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {

    }

    @Override
    public void onPackageIconChanged() {

    }

    @Override
    public void onPackageSizeChanged(String packageName) {

    }

    @Override
    public void onAllSizesComputed() {

    }

    @Override
    public void onLauncherInfoChanged() {

    }

    @Override
    public void onLoadEntriesCompleted() {

    }

    @VisibleForTesting
    void retrieveAppEntry() {
        mAppEntry = mState.getEntry(mPackageName, mUserId);
        if (mAppEntry != null) {
            try {
                mPackageInfo = mPm.getPackageInfo(mAppEntry.info.packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS |
                                PackageManager.MATCH_ANY_USER |
                                PackageManager.GET_SIGNATURES |
                                PackageManager.GET_PERMISSIONS);

                mPackageName = mAppEntry.info.packageName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
                mPackageInfo = null;
            }
        } else {
            mPackageInfo = null;
        }
    }

    @VisibleForTesting
    void updateUninstallButton() {
        final boolean isBundled = (mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean enabled = true;
        if (isBundled) {
            enabled = handleDisableable(mUninstallButton);
        } else {
            if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                    && mUserManager.getUsers().size() >= 2) {
                // When we have multiple users, there is a separate menu
                // to uninstall for all users.
                enabled = false;
            }
        }
        // If this is a device admin, it can't be uninstalled or disabled.
        // We do this here so the text of the button is still set correctly.
        if (isBundled && mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            enabled = false;
        }

        // We don't allow uninstalling DO/PO on *any* users, because if it's a system app,
        // "uninstall" is actually "downgrade to the system version + disable", and "downgrade"
        // will clear data on all users.
        if (Utils.isProfileOrDeviceOwner(mUserManager, mDpm, mPackageInfo.packageName)) {
            enabled = false;
        }

        // Don't allow uninstalling the device provisioning package.
        if (Utils.isDeviceProvisioningPackage(mContext.getResources(),
                mAppEntry.info.packageName)) {
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
        if (enabled && mHomePackages.contains(mPackageInfo.packageName)) {
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
                    enabled = !mPackageInfo.packageName.equals(currentDefaultHome.getPackageName());
                }
            }
        }

        if (mAppsControlDisallowedBySystem) {
            enabled = false;
        }

        if (isFallbackPackage(mAppEntry.info.packageName)) {
            enabled = false;
        }

        mUninstallButton.setEnabled(enabled);
        if (enabled) {
            // Register listener
            mUninstallButton.setOnClickListener(this);
        }
    }

    /**
     * Finish this fragment and return data if possible
     */
    private void setIntentAndFinish(boolean appChanged) {
        if (LOCAL_LOGV) {
            Log.i(TAG, "appChanged=" + appChanged);
        }
        Intent intent = new Intent();
        intent.putExtra(APP_CHG, appChanged);
        mActivity.finishPreferencePanel(mFragment, Activity.RESULT_OK, intent);
        mFinishing = true;
    }

    private void refreshAndFinishIfPossible() {
        if (!refreshUi()) {
            setIntentAndFinish(true);
        } else {
            startListeningToPackageRemove();
        }
    }

    @VisibleForTesting
    boolean isFallbackPackage(String packageName) {
        try {
            IWebViewUpdateService webviewUpdateService =
                    IWebViewUpdateService.Stub.asInterface(
                            ServiceManager.getService("webviewupdate"));
            if (webviewUpdateService.isFallbackPackage(packageName)) {
                return true;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    @VisibleForTesting
    void updateForceStopButton() {
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            // User can't force stop device admin.
            Log.w(TAG, "User can't force stop device admin");
            updateForceStopButtonInner(false);
        } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            Log.w(TAG, "App is not explicitly stopped");
            updateForceStopButtonInner(true);
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mAppEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[]{mAppEntry.info.packageName});
            intent.putExtra(Intent.EXTRA_UID, mAppEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppEntry.info.uid));
            Log.d(TAG, "Sending broadcast to query restart status for "
                    + mAppEntry.info.packageName);
            mActivity.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
    }

    @VisibleForTesting
    void updateForceStopButtonInner(boolean enabled) {
        if (mAppsControlDisallowedBySystem) {
            mForceStopButton.setEnabled(false);
        } else {
            mForceStopButton.setEnabled(enabled);
            mForceStopButton.setOnClickListener(this);
        }
    }

    @VisibleForTesting
    void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        stopListeningToPackageRemove();
        // Create new intent to launch Uninstaller activity
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, allUsers);

        mMetricsFeatureProvider.action(
                mActivity, MetricsProto.MetricsEvent.ACTION_SETTINGS_UNINSTALL_APP);
        mFragment.startActivityForResult(uninstallIntent, mRequestUninstall);
        mDisableAfterUninstall = andDisable;
    }

    @VisibleForTesting
    void forceStopPackage(String pkgName) {
        FeatureFactory.getFactory(mContext).getMetricsFeatureProvider().action(mContext,
                MetricsProto.MetricsEvent.ACTION_APP_FORCE_STOP, pkgName);
        ActivityManager am = (ActivityManager) mActivity.getSystemService(
                Context.ACTIVITY_SERVICE);
        Log.d(TAG, "Stopping package " + pkgName);
        am.forceStopPackage(pkgName);
        int userId = UserHandle.getUserId(mAppEntry.info.uid);
        mState.invalidatePackage(pkgName, userId);
        ApplicationsState.AppEntry newEnt = mState.getEntry(pkgName, userId);
        if (newEnt != null) {
            mAppEntry = newEnt;
        }
        updateForceStopButton();
    }

    @VisibleForTesting
    boolean handleDisableable(Button button) {
        boolean disableable = false;
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        if (mHomePackages.contains(mAppEntry.info.packageName)
                || isSystemPackage(mActivity.getResources(), mPm, mPackageInfo)) {
            // Disable button for core system applications.
            button.setText(R.string.disable_text);
        } else if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            button.setText(R.string.disable_text);
            disableable = true;
        } else {
            button.setText(R.string.enable_text);
            disableable = true;
        }

        return disableable;
    }

    @VisibleForTesting
    boolean isSystemPackage(Resources resources, PackageManager pm, PackageInfo packageInfo) {
        return Utils.isSystemPackage(resources, pm, packageInfo);
    }

    private boolean isDisabledUntilUsed() {
        return mAppEntry.info.enabledSetting
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    private void showDialogInner(@ButtonActionDialogFragment.DialogType int id) {
        ButtonActionDialogFragment newFragment = ButtonActionDialogFragment.newInstance(id);
        newFragment.setTargetFragment(mFragment, 0);
        newFragment.show(mActivity.getFragmentManager(), "dialog " + id);
    }

    /** Returns whether there is only one user on this device, not including the system-only user */
    private boolean isSingleUser() {
        final int userCount = mUserManager.getUserCount();
        return userCount == 1
                || (mUserManager.isSplitSystemUser() && userCount == 2);
    }

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean enabled = getResultCode() != Activity.RESULT_CANCELED;
            Log.d(TAG, "Got broadcast response: Restart status for "
                    + mAppEntry.info.packageName + " " + enabled);
            updateForceStopButtonInner(enabled);
        }
    };

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                final int match = mPm.checkSignatures(pkg1, pkg2);
                if (match >= PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean refreshUi() {
        if (mPackageName == null) {
            return false;
        }
        retrieveAppEntry();
        if (mAppEntry == null || mPackageInfo == null) {
            return false;
        }
        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<>();
        mPm.getHomeActivities(homeActivities);
        mHomePackages.clear();
        for (int i = 0, size = homeActivities.size(); i < size; i++) {
            ResolveInfo ri = homeActivities.get(i);
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

        updateUninstallButton();
        updateForceStopButton();

        return true;
    }

    private void startListeningToPackageRemove() {
        if (mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = true;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        mActivity.registerReceiver(mPackageRemovedReceiver, filter);
    }

    private void stopListeningToPackageRemove() {
        if (!mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = false;
        mActivity.unregisterReceiver(mPackageRemovedReceiver);
    }


    /**
     * Changes the status of disable/enable for a package
     */
    private class DisableChangerRunnable implements Runnable {
        final PackageManager mPm;
        final String mPackageName;
        final int mState;

        public DisableChangerRunnable(PackageManager pm, String packageName, int state) {
            mPm = pm;
            mPackageName = packageName;
            mState = state;
        }

        @Override
        public void run() {
            mPm.setApplicationEnabledSetting(mPackageName, mState, 0);
        }
    }

    /**
     * Receiver to listen to the remove action for packages
     */
    private final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (!mFinishing && mAppEntry.info.packageName.equals(packageName)) {
                mActivity.finishAndRemoveTask();
            }
        }
    };

}
