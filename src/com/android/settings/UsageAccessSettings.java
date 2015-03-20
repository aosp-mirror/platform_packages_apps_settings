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
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.logging.MetricsLogger;

import java.util.List;
import java.util.Collections;

public class UsageAccessSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UsageAccessSettings";
    private static final String BUNDLE_KEY_PROFILEID = "profileId";

    private static final String[] PM_USAGE_STATS_PERMISSION = new String[] {
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    private static final int[] APP_OPS_OP_CODES = new int[] {
            AppOpsManager.OP_GET_USAGE_STATS
    };

    private static class PackageEntry implements Comparable<PackageEntry> {
        public PackageEntry(String packageName, UserHandle userHandle) {
            this.packageName = packageName;
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
            this.userHandle = userHandle;
        }

        @Override
        public int compareTo(PackageEntry another) {
            return packageName.compareTo(another.packageName);
        }

        final String packageName;
        PackageInfo packageInfo;
        boolean permissionGranted;
        int appOpMode;
        UserHandle userHandle;

        SwitchPreference preference;
    }

    /**
     * Fetches the list of Apps that are requesting access to the UsageStats API and updates
     * the PreferenceScreen with the results when complete.
     */
    private class AppsRequestingAccessFetcher extends
            AsyncTask<Void, Void, SparseArray<ArrayMap<String, PackageEntry>>> {

        private final Context mContext;
        private final PackageManager mPackageManager;
        private final IPackageManager mIPackageManager;
        private final UserManager mUserManager;
        private final List<UserHandle> mProfiles;

        public AppsRequestingAccessFetcher(Context context) {
            mContext = context;
            mPackageManager = context.getPackageManager();
            mIPackageManager = ActivityThread.getPackageManager();
            mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            mProfiles = mUserManager.getUserProfiles();
        }

        @Override
        protected SparseArray<ArrayMap<String, PackageEntry>> doInBackground(Void... params) {
            final String[] packages;
            SparseArray<ArrayMap<String, PackageEntry>> entries;
            try {
                packages = mIPackageManager.getAppOpPermissionPackages(
                        Manifest.permission.PACKAGE_USAGE_STATS);

                if (packages == null) {
                    // No packages are requesting permission to use the UsageStats API.
                    return null;
                }

                entries = new SparseArray<>();
                for (final UserHandle profile : mProfiles) {
                    final ArrayMap<String, PackageEntry> entriesForProfile = new ArrayMap<>();
                    final int profileId = profile.getIdentifier();
                    entries.put(profileId, entriesForProfile);
                    for (final String packageName : packages) {
                        final boolean isAvailable = mIPackageManager.isPackageAvailable(packageName,
                                profileId);
                        if (!shouldIgnorePackage(packageName) && isAvailable) {
                            final PackageEntry newEntry = new PackageEntry(packageName, profile);
                            entriesForProfile.put(packageName, newEntry);
                        }
                    }
                }
            } catch (RemoteException e) {
                Log.w(TAG, "PackageManager is dead. Can't get list of packages requesting "
                        + Manifest.permission.PACKAGE_USAGE_STATS);
                return null;
            }

             // Load the packages that have been granted the PACKAGE_USAGE_STATS permission.
            try {
                for (final UserHandle profile : mProfiles) {
                    final int profileId = profile.getIdentifier();
                    final ArrayMap<String, PackageEntry> entriesForProfile = entries.get(profileId);
                    if (entriesForProfile == null) {
                        continue;
                    }
                    final List<PackageInfo> packageInfos = mIPackageManager
                            .getPackagesHoldingPermissions(PM_USAGE_STATS_PERMISSION, 0, profileId)
                            .getList();
                    final int packageInfoCount = packageInfos != null ? packageInfos.size() : 0;
                    for (int i = 0; i < packageInfoCount; i++) {
                        final PackageInfo packageInfo = packageInfos.get(i);
                        final PackageEntry pe = entriesForProfile.get(packageInfo.packageName);
                        if (pe != null) {
                            pe.packageInfo = packageInfo;
                            pe.permissionGranted = true;
                        }
                    }
                }
            } catch (RemoteException e) {
                Log.w(TAG, "PackageManager is dead. Can't get list of packages granted "
                        + Manifest.permission.PACKAGE_USAGE_STATS);
                return null;
            }

            // Load the remaining packages that have requested but don't have the
            // PACKAGE_USAGE_STATS permission.
            for (final UserHandle profile : mProfiles) {
                final int profileId = profile.getIdentifier();
                final ArrayMap<String, PackageEntry> entriesForProfile = entries.get(profileId);
                if (entriesForProfile == null) {
                    continue;
                }
                int packageCount = entriesForProfile.size();
                for (int i = packageCount - 1; i >= 0; --i) {
                    final PackageEntry pe = entriesForProfile.valueAt(i);
                    if (pe.packageInfo == null) {
                        try {
                            pe.packageInfo = mIPackageManager.getPackageInfo(pe.packageName, 0,
                                    profileId);
                        } catch (RemoteException e) {
                            // This package doesn't exist. This may occur when an app is
                            // uninstalled for one user, but it is not removed from the system.
                            entriesForProfile.removeAt(i);
                        }
                    }
                }
            }

            // Find out which packages have been granted permission from AppOps.
            final List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(
                    APP_OPS_OP_CODES);
            final int packageOpsCount = packageOps != null ? packageOps.size() : 0;
            for (int i = 0; i < packageOpsCount; i++) {
                final AppOpsManager.PackageOps packageOp = packageOps.get(i);
                final int userId = UserHandle.getUserId(packageOp.getUid());
                if (!isThisUserAProfileOfCurrentUser(userId)) {
                    // This AppOp does not belong to any of this user's profiles.
                    continue;
                }

                final ArrayMap<String, PackageEntry> entriesForProfile = entries.get(userId);
                if (entriesForProfile == null) {
                    continue;
                }
                final PackageEntry pe = entriesForProfile.get(packageOp.getPackageName());
                if (pe == null) {
                    Log.w(TAG, "AppOp permission exists for package " + packageOp.getPackageName()
                            + " of user " + userId +
                            " but package doesn't exist or did not request UsageStats access");
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
        protected void onPostExecute(SparseArray<ArrayMap<String, PackageEntry>> newEntries) {
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
            final int oldProfileCount = mPackageEntryMap.size();
            for (int profileIndex = 0; profileIndex < oldProfileCount; ++profileIndex) {
                final int profileId = mPackageEntryMap.keyAt(profileIndex);
                final ArrayMap<String, PackageEntry> oldEntriesForProfile = mPackageEntryMap
                        .valueAt(profileIndex);
                final int oldPackageCount = oldEntriesForProfile.size();

                final ArrayMap<String, PackageEntry> newEntriesForProfile = newEntries.get(
                        profileId);

                for (int i = 0; i < oldPackageCount; i++) {
                    final PackageEntry oldPackageEntry = oldEntriesForProfile.valueAt(i);

                    PackageEntry newPackageEntry = null;
                    if (newEntriesForProfile != null) {
                        newPackageEntry = newEntriesForProfile.get(oldPackageEntry.packageName);
                    }
                    if (newPackageEntry == null) {
                        // This package has been removed.
                        mPreferenceScreen.removePreference(oldPackageEntry.preference);
                    } else {
                        // This package already exists in the preference hierarchy, so reuse that
                        // Preference.
                        newPackageEntry.preference = oldPackageEntry.preference;
                    }
                }
            }

            // Now add new packages to the PreferenceScreen.
            final int newProfileCount = newEntries.size();
            for (int profileIndex = 0; profileIndex < newProfileCount; ++profileIndex) {
                final int profileId = newEntries.keyAt(profileIndex);
                final ArrayMap<String, PackageEntry> newEntriesForProfile = newEntries.get(
                        profileId);
                final int packageCount = newEntriesForProfile.size();
                for (int i = 0; i < packageCount; i++) {
                    final PackageEntry packageEntry = newEntriesForProfile.valueAt(i);
                    if (packageEntry.preference == null) {
                        packageEntry.preference = new SwitchPreference(mContext);
                        packageEntry.preference.setPersistent(false);
                        packageEntry.preference.setOnPreferenceChangeListener(
                                UsageAccessSettings.this);
                        mPreferenceScreen.addPreference(packageEntry.preference);
                    }
                    updatePreference(packageEntry);
                }
            }
            mPackageEntryMap.clear();
            mPackageEntryMap = newEntries;

            // Add/remove headers if necessary. If there are package entries only for one user and
            // that user is not the managed profile then do not show headers.
            if (mPackageEntryMap.size() == 1 &&
                    mPackageEntryMap.keyAt(0) == UserHandle.myUserId()) {
                for (int i = 0; i < mCategoryHeaders.length; ++i) {
                    if (mCategoryHeaders[i] != null) {
                        mPreferenceScreen.removePreference(mCategoryHeaders[i]);
                    }
                    mCategoryHeaders[i] = null;
                }
            } else {
                for (int i = 0; i < mCategoryHeaders.length; ++i) {
                    if (mCategoryHeaders[i] == null) {
                        final Preference preference = new Preference(mContext, null,
                                com.android.internal.R.attr.preferenceCategoryStyle, 0);
                        mCategoryHeaders[i] = preference;
                        preference.setTitle(mCategoryHeaderTitleResIds[i]);
                        preference.setEnabled(false);
                        mPreferenceScreen.addPreference(preference);
                    }
                }
            }

            // Sort preferences alphabetically within categories
            int order = 0;
            final int profileCount = mProfiles.size();
            for (int i = 0; i < profileCount; ++i) {
                Preference header = mCategoryHeaders[i];
                if (header != null) {
                    header.setOrder(order++);
                }
                ArrayMap<String, PackageEntry> entriesForProfile =
                        mPackageEntryMap.get(mProfiles.get(i).getIdentifier());
                if (entriesForProfile != null) {
                    List<PackageEntry> sortedEntries = Collections.list(
                            Collections.enumeration(entriesForProfile.values()));
                    Collections.sort(sortedEntries);
                    for (PackageEntry pe : sortedEntries) {
                        pe.preference.setOrder(order++);
                    }
                }
            }
        }

        private void updatePreference(PackageEntry pe) {
            final int profileId = pe.userHandle.getIdentifier();
            // Set something as default
            pe.preference.setEnabled(false);
            pe.preference.setTitle(pe.packageName);
            pe.preference.setIcon(mUserManager.getBadgedIconForUser(mPackageManager
                    .getDefaultActivityIcon(), pe.userHandle));
            try {
                // Try setting real title and icon
                final ApplicationInfo info = mIPackageManager.getApplicationInfo(pe.packageName,
                        0 /* no flags */, profileId);
                if (info != null) {
                    pe.preference.setEnabled(true);
                    pe.preference.setTitle(info.loadLabel(mPackageManager).toString());
                    pe.preference.setIcon(mUserManager.getBadgedIconForUser(info.loadIcon(
                            mPackageManager), pe.userHandle));
                }
            } catch (RemoteException e) {
                Log.w(TAG, "PackageManager is dead. Can't get app info for package " +
                        pe.packageName + " of user " + profileId);
                // Keep going to update other parts of the preference
            }

            pe.preference.setKey(pe.packageName);
            Bundle extra = pe.preference.getExtras();
            extra.putInt(BUNDLE_KEY_PROFILEID, profileId);

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

        private boolean isThisUserAProfileOfCurrentUser(final int userId) {
            final int profilesMax = mProfiles.size();
            for (int i = 0; i < profilesMax; ++i) {
                if (mProfiles.get(i).getIdentifier() == userId) {
                    return true;
                }
            }
            return false;
        }
    }

    static boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals("com.android.settings");
    }

    private AppsRequestingAccessFetcher mLastFetcherTask;
    SparseArray<ArrayMap<String, PackageEntry>> mPackageEntryMap = new SparseArray<>();
    AppOpsManager mAppOpsManager;
    PreferenceScreen mPreferenceScreen;
    private Preference[] mCategoryHeaders = new Preference[2];
    private static int[] mCategoryHeaderTitleResIds = new int[] {
        R.string.category_personal,
        R.string.category_work
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.USAGE_ACCESS;
    }

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
        final int profileId = preference.getExtras().getInt(BUNDLE_KEY_PROFILEID);
        final PackageEntry pe = getPackageEntry(packageName, profileId);
        if (pe == null) {
            Log.w(TAG, "Preference change event handling failed");
            return false;
        }

        if (!(newValue instanceof Boolean)) {
            Log.w(TAG, "Preference change event for package " + packageName + " of user " +
                    profileId + " had non boolean value of type " + newValue.getClass().getName());
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
            WarningDialogFragment.newInstance(pe).show(ft, "warning");
            return false;
        }
        return true;
    }

    void setNewMode(PackageEntry pe, int newMode) {
        mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS,
        pe.packageInfo.applicationInfo.uid, pe.packageName, newMode);
        pe.appOpMode = newMode;
    }

    void allowAccess(String packageName, int profileId) {
        final PackageEntry entry = getPackageEntry(packageName, profileId);
        if (entry == null) {
            Log.w(TAG, "Unable to give access");
            return;
        }

        setNewMode(entry, AppOpsManager.MODE_ALLOWED);
        entry.preference.setChecked(true);
    }

    private PackageEntry getPackageEntry(String packageName, int profileId) {
        ArrayMap<String, PackageEntry> entriesForProfile = mPackageEntryMap.get(profileId);
        if (entriesForProfile == null) {
            Log.w(TAG, "getPackageEntry fails for package " + packageName + " of user " +
                    profileId + ": user does not seem to be valid.");
            return null;
        }
        final PackageEntry entry = entriesForProfile.get(packageName);
        if (entry == null) {
            Log.w(TAG, "getPackageEntry fails for package " + packageName + " of user " +
                    profileId + ": package does not exist.");
        }
        return entry;
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
        private static final String ARG_PROFILE_ID = "profileId";

        public static WarningDialogFragment newInstance(PackageEntry pe) {
            WarningDialogFragment dialog = new WarningDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PACKAGE_NAME, pe.packageName);
            args.putInt(ARG_PROFILE_ID, pe.userHandle.getIdentifier());
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
                        getArguments().getString(ARG_PACKAGE_NAME),
                        getArguments().getInt(ARG_PROFILE_ID));
            } else {
                dialog.cancel();
            }
        }
    }
}
