/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.content.PackageMonitor;

import android.Manifest;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.ArrayMap;
import android.util.Log;

import java.util.List;

public class UsageAccessSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UsageAccessSettings";

    private static final String[] PM_USAGE_STATS_PERMISSION = new String[] {
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    private static final int[] APP_OPS_OP_CODES = new int[] {
            AppOpsManager.OP_GET_USAGE_STATS
    };

    private static class PackageEntry {
        public PackageEntry(String packageName) {
            this.packageName = packageName;
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
        }

        final String packageName;
        PackageInfo packageInfo;
        boolean permissionGranted;
        int appOpMode;

        SwitchPreference preference;
    }

    /**
     * Fetches the list of Apps that are requesting access to the UsageStats API and updates
     * the PreferenceScreen with the results when complete.
     */
    private class AppsRequestingAccessFetcher extends
            AsyncTask<Void, Void, ArrayMap<String, PackageEntry>> {

        private final Context mContext;
        private final PackageManager mPackageManager;
        private final IPackageManager mIPackageManager;

        public AppsRequestingAccessFetcher(Context context) {
            mContext = context;
            mPackageManager = context.getPackageManager();
            mIPackageManager = ActivityThread.getPackageManager();
        }

        @Override
        protected ArrayMap<String, PackageEntry> doInBackground(Void... params) {
            final String[] packages;
            try {
                packages = mIPackageManager.getAppOpPermissionPackages(
                        Manifest.permission.PACKAGE_USAGE_STATS);
            } catch (RemoteException e) {
                Log.w(TAG, "PackageManager is dead. Can't get list of packages requesting "
                        + Manifest.permission.PACKAGE_USAGE_STATS);
                return null;
            }

            if (packages == null) {
                // No packages are requesting permission to use the UsageStats API.
                return null;
            }

            ArrayMap<String, PackageEntry> entries = new ArrayMap<>();
            for (final String packageName : packages) {
                if (!shouldIgnorePackage(packageName)) {
                    entries.put(packageName, new PackageEntry(packageName));
                }
            }

             // Load the packages that have been granted the PACKAGE_USAGE_STATS permission.
            final List<PackageInfo> packageInfos = mPackageManager.getPackagesHoldingPermissions(
                    PM_USAGE_STATS_PERMISSION, 0);
            final int packageInfoCount = packageInfos != null ? packageInfos.size() : 0;
            for (int i = 0; i < packageInfoCount; i++) {
                final PackageInfo packageInfo = packageInfos.get(i);
                final PackageEntry pe = entries.get(packageInfo.packageName);
                if (pe != null) {
                    pe.packageInfo = packageInfo;
                    pe.permissionGranted = true;
                }
            }

            // Load the remaining packages that have requested but don't have the
            // PACKAGE_USAGE_STATS permission.
            int packageCount = entries.size();
            for (int i = 0; i < packageCount; i++) {
                final PackageEntry pe = entries.valueAt(i);
                if (pe.packageInfo == null) {
                    try {
                        pe.packageInfo = mPackageManager.getPackageInfo(pe.packageName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        // This package doesn't exist. This may occur when an app is uninstalled for
                        // one user, but it is not removed from the system.
                        entries.removeAt(i);
                        i--;
                        packageCount--;
                    }
                }
            }

            // Find out which packages have been granted permission from AppOps.
            final List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(
                    APP_OPS_OP_CODES);
            final int packageOpsCount = packageOps != null ? packageOps.size() : 0;
            for (int i = 0; i < packageOpsCount; i++) {
                final AppOpsManager.PackageOps packageOp = packageOps.get(i);
                final PackageEntry pe = entries.get(packageOp.getPackageName());
                if (pe == null) {
                    Log.w(TAG, "AppOp permission exists for package " + packageOp.getPackageName()
                            + " but package doesn't exist or did not request UsageStats access");
                    continue;
                }

                if (packageOp.getUid() != pe.packageInfo.applicationInfo.uid) {
                    // This AppOp does not belong to this user.
                    continue;
                }

                if (packageOp.getOps().size() < 1) {
                    Log.w(TAG, "No AppOps permission exists for package "
                            + packageOp.getPackageName());
                    continue;
                }

                pe.appOpMode = packageOp.getOps().get(0).getMode();
            }

            return entries;
        }

        @Override
        protected void onPostExecute(ArrayMap<String, PackageEntry> newEntries) {
            mLastFetcherTask = null;

            if (getActivity() == null) {
                // We must have finished the Activity while we were processing in the background.
                return;
            }

            if (newEntries == null) {
                mPackageEntryMap.clear();
                mPreferenceScreen.removeAll();
                return;
            }

            // Find the deleted entries and remove them from the PreferenceScreen.
            final int oldPackageCount = mPackageEntryMap.size();
            for (int i = 0; i < oldPackageCount; i++) {
                final PackageEntry oldPackageEntry = mPackageEntryMap.valueAt(i);
                final PackageEntry newPackageEntry = newEntries.get(oldPackageEntry.packageName);
                if (newPackageEntry == null) {
                    // This package has been removed.
                    mPreferenceScreen.removePreference(oldPackageEntry.preference);
                } else {
                    // This package already exists in the preference hierarchy, so reuse that
                    // Preference.
                    newPackageEntry.preference = oldPackageEntry.preference;
                }
            }

            // Now add new packages to the PreferenceScreen.
            final int packageCount = newEntries.size();
            for (int i = 0; i < packageCount; i++) {
                final PackageEntry packageEntry = newEntries.valueAt(i);
                if (packageEntry.preference == null) {
                    packageEntry.preference = new SwitchPreference(mContext);
                    packageEntry.preference.setPersistent(false);
                    packageEntry.preference.setOnPreferenceChangeListener(UsageAccessSettings.this);
                    mPreferenceScreen.addPreference(packageEntry.preference);
                }
                updatePreference(packageEntry);
            }

            mPackageEntryMap.clear();
            mPackageEntryMap = newEntries;
        }

        private void updatePreference(PackageEntry pe) {
            pe.preference.setIcon(pe.packageInfo.applicationInfo.loadIcon(mPackageManager));
            pe.preference.setTitle(pe.packageInfo.applicationInfo.loadLabel(mPackageManager));
            pe.preference.setKey(pe.packageName);

            boolean check = false;
            if (pe.appOpMode == AppOpsManager.MODE_ALLOWED) {
                check = true;
            } else if (pe.appOpMode == AppOpsManager.MODE_DEFAULT) {
                // If the default AppOps mode is set, then fall back to
                // whether the app has been granted permission by PackageManager.
                check = pe.permissionGranted;
            }

            if (check != pe.preference.isChecked()) {
                pe.preference.setChecked(check);
            }
        }
    }

    static boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals("com.android.settings");
    }

    private AppsRequestingAccessFetcher mLastFetcherTask;
    ArrayMap<String, PackageEntry> mPackageEntryMap = new ArrayMap<>();
    AppOpsManager mAppOpsManager;
    PreferenceScreen mPreferenceScreen;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.usage_access_settings);
        mPreferenceScreen = getPreferenceScreen();
        mPreferenceScreen.setOrderingAsAdded(false);
        mAppOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateInterestedApps();
        mPackageMonitor.register(getActivity(), Looper.getMainLooper(), false);
    }

    @Override
    public void onPause() {
        super.onPause();

        mPackageMonitor.unregister();
        if (mLastFetcherTask != null) {
            mLastFetcherTask.cancel(true);
            mLastFetcherTask = null;
        }
    }

    private void updateInterestedApps() {
        if (mLastFetcherTask != null) {
            // Canceling can only fail for some obscure reason since mLastFetcherTask would be
            // null if the task has already completed. So we ignore the result of cancel and
            // spawn a new task to get fresh data. AsyncTask executes tasks serially anyways,
            // so we are safe from running two tasks at the same time.
            mLastFetcherTask.cancel(true);
        }

        mLastFetcherTask = new AppsRequestingAccessFetcher(getActivity());
        mLastFetcherTask.execute();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String packageName = preference.getKey();
        final PackageEntry pe = mPackageEntryMap.get(packageName);
        if (pe == null) {
            Log.w(TAG, "Preference change event for package " + packageName
                    + " but that package is no longer valid.");
            return false;
        }

        if (!(newValue instanceof Boolean)) {
            Log.w(TAG, "Preference change event for package " + packageName
                    + " had non boolean value of type " + newValue.getClass().getName());
            return false;
        }

        final int newMode = (Boolean) newValue ?
                AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED;

        // Check if we need to do any work.
        if (pe.appOpMode != newMode) {
            if (newMode != AppOpsManager.MODE_ALLOWED) {
                // Turning off the setting has no warning.
                setNewMode(pe, newMode);
                return true;
            }

            // Turning on the setting has a Warning.
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            Fragment prev = getChildFragmentManager().findFragmentByTag("warning");
            if (prev != null) {
                ft.remove(prev);
            }
            WarningDialogFragment.newInstance(pe.packageName).show(ft, "warning");
            return false;
        }
        return true;
    }

    void setNewMode(PackageEntry pe, int newMode) {
        mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS,
        pe.packageInfo.applicationInfo.uid, pe.packageName, newMode);
        pe.appOpMode = newMode;
    }

    void allowAccess(String packageName) {
        final PackageEntry entry = mPackageEntryMap.get(packageName);
        if (entry == null) {
            Log.w(TAG, "Unable to give access to package " + packageName + ": it does not exist.");
            return;
        }

        setNewMode(entry, AppOpsManager.MODE_ALLOWED);
        entry.preference.setChecked(true);
    }

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            updateInterestedApps();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            updateInterestedApps();
        }
    };

    public static class WarningDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        private static final String ARG_PACKAGE_NAME = "package";

        public static WarningDialogFragment newInstance(String packageName) {
            WarningDialogFragment dialog = new WarningDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PACKAGE_NAME, packageName);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.allow_usage_access_title)
                    .setMessage(R.string.allow_usage_access_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(R.string.cancel, this)
                    .setPositiveButton(android.R.string.ok, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                ((UsageAccessSettings) getParentFragment()).allowAccess(
                        getArguments().getString(ARG_PACKAGE_NAME));
            } else {
                dialog.cancel();
            }
        }
    }
}
