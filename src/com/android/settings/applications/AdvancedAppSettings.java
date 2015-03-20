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
package com.android.settings.applications;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ApplicationsState.AppEntry;
import com.android.settings.applications.ApplicationsState.Callbacks;
import com.android.settings.applications.ApplicationsState.Session;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements Callbacks,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    static final String TAG = "AdvancedAppSettings";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_ALL_APPS = "all_apps";
    private static final String KEY_RESET_ALL = "reset_all";
    private static final String EXTRA_RESET_DIALOG = "resetDialog";

    private ApplicationsState mApplicationsState;
    private Session mSession;
    private Preference mAllApps;
    private Preference mResetAll;

    AlertDialog mResetDialog;

    private boolean mActivityResumed;
    private PackageManager mPm;
    private IPackageManager mIPm;
    private INotificationManager mNm;
    private NetworkPolicyManager mNpm;
    private AppOpsManager mAom;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advanced_apps);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);

        mAllApps = findPreference(KEY_ALL_APPS);
        mResetAll = findPreference(KEY_RESET_ALL);
        mResetAll.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                buildResetDialog();
                return true;
            }
        });
        updateAllAppsSummary();

        mPm = getActivity().getPackageManager();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mNm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mNpm = NetworkPolicyManager.from(getActivity());
        mAom = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mHandler = new Handler(getActivity().getMainLooper());
    }

    private void updateAllAppsSummary() {
        mAllApps.setSummary(getString(R.string.all_apps_summary, mSession.getAllApps().size()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RESET_DIALOG)) {
            buildResetDialog();
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mResetDialog != null) {
            mResetDialog.dismiss();
            mResetDialog = null;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_ADVANCED;
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivityResumed = true;
    }

    @Override
    public void onPause() {
        mActivityResumed = false;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResetDialog != null) {
            outState.putBoolean(EXTRA_RESET_DIALOG, true);
        }
    }

    void buildResetDialog() {
        if (mResetDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.reset_app_preferences_title);
            builder.setMessage(R.string.reset_app_preferences_desc);
            builder.setPositiveButton(R.string.reset_app_preferences_button, this);
            builder.setNegativeButton(R.string.cancel, null);
            mResetDialog = builder.show();
            mResetDialog.setOnDismissListener(this);
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
        if (mResetDialog == dialog) {
            (new AsyncTask<Void, Void, Void>() {
                @Override protected Void doInBackground(Void... params) {
                    List<ApplicationInfo> apps = mPm.getInstalledApplications(
                            PackageManager.GET_DISABLED_COMPONENTS);
                    for (int i=0; i<apps.size(); i++) {
                        ApplicationInfo app = apps.get(i);
                        try {
                            if (DEBUG) Log.v(TAG, "Enabling notifications: " + app.packageName);
                            mNm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                        } catch (android.os.RemoteException ex) {
                        }
                        if (!app.enabled) {
                            if (DEBUG) Log.v(TAG, "Enabling app: " + app.packageName);
                            if (mPm.getApplicationEnabledSetting(app.packageName)
                                    == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                                mPm.setApplicationEnabledSetting(app.packageName,
                                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                        PackageManager.DONT_KILL_APP);
                            }
                        }
                    }
                    try {
                        mIPm.resetPreferredActivities(UserHandle.myUserId());
                    } catch (RemoteException e) {
                    }
                    mAom.resetAllModes();
                    final int[] restrictedUids = mNpm.getUidsWithPolicy(
                            POLICY_REJECT_METERED_BACKGROUND);
                    final int currentUserId = ActivityManager.getCurrentUser();
                    for (int uid : restrictedUids) {
                        // Only reset for current user
                        if (UserHandle.getUserId(uid) == currentUserId) {
                            if (DEBUG) Log.v(TAG, "Clearing data policy: " + uid);
                            mNpm.setUidPolicy(uid, POLICY_NONE);
                        }
                    }
                    mHandler.post(new Runnable() {
                        @Override public void run() {
                            if (DEBUG) Log.v(TAG, "Done clearing");
                            if (getActivity() != null && mActivityResumed) {
                                if (DEBUG) Log.v(TAG, "Updating UI!");

                            }
                        }
                    });
                    return null;
                }
            }).execute();
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No-op.
    }

    @Override
    public void onPackageListChanged() {
        updateAllAppsSummary();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No-op.
    }

    @Override
    public void onPackageIconChanged() {
        // No-op.
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        // No-op.
    }

    @Override
    public void onAllSizesComputed() {
        // No-op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No-op.
    }

    @Override
    public void onLoadEntriesCompleted() {
        // No-op.
    }

}
