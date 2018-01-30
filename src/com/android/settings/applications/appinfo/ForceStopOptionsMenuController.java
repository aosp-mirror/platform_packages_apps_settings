/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.FORCE_STOP_MENU;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;
import com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu;

public class ForceStopOptionsMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnPrepareOptionsMenu, OnOptionsItemSelected {

    private static final String TAG = "ForceStopMenuController";

    private final Context mContext;
    private final AppInfoDashboardFragment mParent;
    private final DevicePolicyManagerWrapper mDpm;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private int mUserId;
    private MenuItem mForceStopMenu;

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean enabled = getResultCode() != Activity.RESULT_CANCELED;
            Log.d(TAG, "Got broadcast response: Restart status for "
                + mParent.getAppEntry().info.packageName + " " + enabled);
            enableForceStopMenu(enabled);
        }
    };

    public ForceStopOptionsMenuController(Context context, AppInfoDashboardFragment parent,
            DevicePolicyManagerWrapper devicePolicyManager,
            MetricsFeatureProvider metricsFeatureProvider, Lifecycle lifecycle) {
        mContext = context;
        mParent = parent;
        mDpm = devicePolicyManager;
        mMetricsFeatureProvider = metricsFeatureProvider;
        mUserId = UserHandle.myUserId();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, FORCE_STOP_MENU, 2, R.string.force_stop)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == FORCE_STOP_MENU) {
            handleForceStopMenuClick();
            return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mForceStopMenu = menu.findItem(FORCE_STOP_MENU);
        updateForceStopMenu(mParent.getAppEntry(), mParent.getPackageInfo());
    }

    @VisibleForTesting
    void updateForceStopMenu(AppEntry appEntry, PackageInfo packageInfo) {
        boolean enabled = false;
        if (mDpm.packageHasActiveAdmins(packageInfo.packageName)) {
            // User can't force stop device admin.
            Log.w(TAG, "User can't force stop device admin");
        } else if (AppUtils.isInstant(packageInfo.applicationInfo)) {
            // No force stop for instant app
            if (mForceStopMenu != null) {
                mForceStopMenu.setVisible(false);
            }
        } else if ((appEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            Log.w(TAG, "App is not explicitly stopped");
            enabled = true;
        } else {
            final Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                Uri.fromParts("package", appEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] { appEntry.info.packageName });
            intent.putExtra(Intent.EXTRA_UID, appEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(appEntry.info.uid));
            Log.d(TAG, "Sending broadcast to query restart status for "
                + appEntry.info.packageName);
            mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
        enableForceStopMenu(enabled);
    }

    private void enableForceStopMenu(boolean enabled) {
        if (mForceStopMenu != null) {
            final boolean disallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_APPS_CONTROL, mUserId);
            mForceStopMenu.setEnabled(disallowedBySystem ? false : enabled);
        }
    }

    @VisibleForTesting
    void handleForceStopMenuClick() {
        if (mParent.getAppEntry() == null) {
            mParent.setIntentAndFinish(true, true);
            return;
        }
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
            mContext, UserManager.DISALLOW_APPS_CONTROL, mUserId);
        final boolean disallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
            mContext, UserManager.DISALLOW_APPS_CONTROL, mUserId);
        if (admin != null && !disallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext, admin);
        } else {
            mParent.showDialogInner(mParent.DLG_FORCE_STOP, 0);
        }
    }

    private void forceStopPackage(String pkgName) {
        mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_APP_FORCE_STOP, pkgName);
        final ActivityManager am = (ActivityManager) mContext.getSystemService(
            Context.ACTIVITY_SERVICE);
        Log.d(TAG, "Stopping package " + pkgName);
        am.forceStopPackage(pkgName);
        final int userId = UserHandle.getUserId(mParent.getAppEntry().info.uid);
        final ApplicationsState appState = mParent.getAppState();
        appState.invalidatePackage(pkgName, userId);
        final AppEntry newEnt = appState.getEntry(pkgName, userId);
        if (newEnt != null) {
            mParent.setAppEntry(newEnt);
        }
    }

    public AlertDialog createDialog(int id) {
        if (id != mParent.DLG_FORCE_STOP) {
            return null;
        }
        return new AlertDialog.Builder(mContext)
            .setTitle(mContext.getText(R.string.force_stop_dlg_title))
            .setMessage(mContext.getText(R.string.force_stop_dlg_text))
            .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Force stop
                    forceStopPackage(mParent.getAppEntry().info.packageName);
                }
            })
            .setNegativeButton(R.string.dlg_cancel, null)
            .create();
    }

}
