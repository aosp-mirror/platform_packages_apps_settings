/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.applications.manageapplications;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

import java.util.Arrays;
import java.util.List;

public class ResetAppsHelper implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {

    private static final String EXTRA_RESET_DIALOG = "resetDialog";

    private final PackageManager mPm;
    private final IPackageManager mIPm;
    private final INotificationManager mNm;
    private final NetworkPolicyManager mNpm;
    private final AppOpsManager mAom;
    private final Context mContext;

    private AlertDialog mResetDialog;

    public ResetAppsHelper(Context context) {
        mContext = context;
        mPm = context.getPackageManager();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mNm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mNpm = NetworkPolicyManager.from(context);
        mAom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RESET_DIALOG)) {
            buildResetDialog();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mResetDialog != null) {
            outState.putBoolean(EXTRA_RESET_DIALOG, true);
        }
    }

    public void stop() {
        if (mResetDialog != null) {
            mResetDialog.dismiss();
            mResetDialog = null;
        }
    }

    void buildResetDialog() {
        if (mResetDialog == null) {
            mResetDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.reset_app_preferences_title)
                    .setMessage(R.string.reset_app_preferences_desc)
                    .setPositiveButton(R.string.reset_app_preferences_button, this)
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener(this)
                    .show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mResetDialog == dialog) {
            mResetDialog = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mResetDialog != dialog) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<ApplicationInfo> apps = mPm.getInstalledApplications(
                        PackageManager.GET_DISABLED_COMPONENTS);
                final List<String> whiteList = Arrays.asList(
                        mContext.getResources().getStringArray(
                                R.array.config_skip_reset_apps_package_name));

                for (int i = 0; i < apps.size(); i++) {
                    ApplicationInfo app = apps.get(i);
                    if (whiteList.contains(app.packageName)) {
                        continue;
                    }
                    try {
                        mNm.clearData(app.packageName, app.uid, false);
                    } catch (android.os.RemoteException ex) {
                    }
                    if (!app.enabled) {
                        if (mPm.getApplicationEnabledSetting(app.packageName)
                                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                            mPm.setApplicationEnabledSetting(app.packageName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                    PackageManager.DONT_KILL_APP);
                        }
                    }
                }
                try {
                    mIPm.resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e) {
                }
                mAom.resetAllModes();
                final int[] restrictedUids = mNpm.getUidsWithPolicy(
                        POLICY_REJECT_METERED_BACKGROUND);
                final int currentUserId = ActivityManager.getCurrentUser();
                for (int uid : restrictedUids) {
                    // Only reset for current user
                    if (UserHandle.getUserId(uid) == currentUserId) {
                        mNpm.setUidPolicy(uid, POLICY_NONE);
                    }
                }
            }
        });
    }
}
