/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.users;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.users.AppCopyHelper;
import com.android.settingslib.widget.AppSwitchPreference;

/**
 * Allows an admin user to selectively copy some of their installed packages to a second user.
 */
public class AppCopyFragment extends SettingsPreferenceFragment {
    private static final String TAG = AppCopyFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final String PKG_PREFIX = "pkg_";

    /** Key for extra passed in from calling fragment for the userId of the user being edited */
    public static final String EXTRA_USER_ID = "user_id";

    protected UserManager mUserManager;
    protected UserHandle mUser;

    private AppCopyHelper mHelper;

    /** List of installable apps presented to the user. */
    private PreferenceGroup mAppList;

    private boolean mAppListChanged;

    private AsyncTask mAppLoadingTask;

    private final BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "mUserBackgrounding onReceive");
            // Update the user's app selection right away without waiting for a pause
            // onPause() might come in too late, causing apps to disappear after broadcasts
            // have been scheduled during user startup.
            if (mAppListChanged) {
                if (DEBUG) Log.d(TAG, "User backgrounding: installing apps");
                mHelper.installSelectedApps();
                if (DEBUG) Log.d(TAG, "User backgrounding: done installing apps");
            }
        }
    };

    private final BroadcastReceiver mPackageObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onPackageChanged(intent);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        init(icicle);
    }

    protected void init(Bundle icicle) {
        if (icicle != null) {
            mUser = new UserHandle(icicle.getInt(EXTRA_USER_ID));
        } else {
            final Bundle args = getArguments();
            if (args != null) {
                if (args.containsKey(EXTRA_USER_ID)) {
                    mUser = new UserHandle(args.getInt(EXTRA_USER_ID));
                }
            }
        }
        if (mUser == null) {
            throw new IllegalStateException("No user specified.");
        }

        mHelper = new AppCopyHelper(getContext(), mUser);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);

        addPreferencesFromResource(R.xml.app_copier);
        mAppList = getPreferenceScreen();
        mAppList.setOrderingAsAdded(false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USERS_APP_COPYING;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_USER_ID, mUser.getIdentifier());
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mUserBackgrounding,
                new IntentFilter(Intent.ACTION_USER_BACKGROUND));

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(mPackageObserver, packageFilter);

        mAppListChanged = false;
        if (mAppLoadingTask == null || mAppLoadingTask.getStatus() == AsyncTask.Status.FINISHED) {
            mAppLoadingTask = new AppLoadingTask().execute();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mUserBackgrounding);
        getActivity().unregisterReceiver(mPackageObserver);
        if (mAppListChanged) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    mHelper.installSelectedApps();
                    return null;
                }
            }.execute();
        }
    }

    private void onPackageChanged(Intent intent) {
        final String action = intent.getAction();
        final String packageName = intent.getData().getSchemeSpecificPart();
        if (DEBUG) Log.d(TAG, "onPackageChanged (" + action + "): " + packageName);

        // Package added/removed, so check if the preference needs to be enabled
        final AppSwitchPreference pref = findPreference(getKeyForPackage(packageName));
        if (pref == null) return;

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            pref.setEnabled(false);
            pref.setChecked(false);
            mHelper.setPackageSelected(packageName, false);
        } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            pref.setEnabled(true);
        }
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mHelper.fetchAndMergeApps();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            populateApps();
        }
    }

    private void populateApps() {
        // Check if the user was removed in the meantime.
        if (Utils.getExistingUser(mUserManager, mUser) == null) {
            return;
        }
        mHelper.resetSelectedPackages();
        mAppList.removeAll();
        for (AppCopyHelper.SelectableAppInfo app : mHelper.getVisibleApps()) {
            if (app.packageName == null) continue;

            final AppSwitchPreference p = new AppSwitchPreference(getPrefContext());
            p.setIcon(app.icon != null ? app.icon.mutate() : null);
            p.setChecked(false);
            p.setTitle(app.appName);
            p.setKey(getKeyForPackage(app.packageName));
            p.setPersistent(false);
            p.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!preference.isEnabled()) {
                    // This item isn't available anymore (perhaps it was since uninstalled).
                    if (DEBUG) Log.d(TAG, "onPreferenceChange but not enabled");
                    return false;
                }

                final boolean checked = (boolean) newValue;
                final String packageName = preference.getKey().substring(PKG_PREFIX.length());
                if (DEBUG) Log.d(TAG, "onPreferenceChange: " + packageName + " check=" + newValue);
                mHelper.setPackageSelected(packageName, checked);
                mAppListChanged = true;
                return true;
            });

            mAppList.addPreference(p);
        }
        mAppListChanged = true;
    }

    private String getKeyForPackage(String packageName) {
        return PKG_PREFIX + packageName;
    }
}
