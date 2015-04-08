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

import android.app.Activity;
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
import android.provider.SearchIndexableResource;
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
import com.android.settings.applications.PermissionsInfo.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements Callbacks,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Callback, Indexable {

    static final String TAG = "AdvancedAppSettings";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_APP_PERM = "manage_perms";
    private static final String KEY_ALL_APPS = "all_apps";
    private static final String KEY_APP_DOMAIN_URLS = "domain_urls";
    private static final String KEY_RESET_ALL = "reset_all";
    private static final String KEY_DEFAULT_EMERGENCY_APP = "default_emergency_app";
    private static final String EXTRA_RESET_DIALOG = "resetDialog";

    private ApplicationsState mApplicationsState;
    private Session mSession;
    private Preference mAppPermsPreference;
    private Preference mAppDomainURLsPreference;
    private Preference mAllAppsPreference;
    private Preference mResetAllPreference;

    AlertDialog mResetDialog;

    private boolean mActivityResumed;
    private PackageManager mPm;
    private IPackageManager mIPm;
    private INotificationManager mNm;
    private NetworkPolicyManager mNpm;
    private AppOpsManager mAom;
    private Handler mHandler;
    private PermissionsInfo mPermissionsInfo;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advanced_apps);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);

        mAppPermsPreference = findPreference(KEY_APP_PERM);
        mAppDomainURLsPreference = findPreference(KEY_APP_DOMAIN_URLS);
        mAllAppsPreference = findPreference(KEY_ALL_APPS);
        mResetAllPreference = findPreference(KEY_RESET_ALL);
        mResetAllPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                buildResetDialog();
                return true;
            }
        });
        updateUI();

        mPm = getActivity().getPackageManager();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mNm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mNpm = NetworkPolicyManager.from(getActivity());
        mAom = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mHandler = new Handler(getActivity().getMainLooper());

        if (!DefaultEmergencyPreference.isAvailable(getActivity())) {
            removePreference(KEY_DEFAULT_EMERGENCY_APP);
        }
    }

    private void updateUI() {
        ArrayList<AppEntry> allApps = mSession.getAllApps();
        mAllAppsPreference.setSummary(getString(R.string.all_apps_summary, allApps.size()));

        int countAppWithDomainURLs = 0;
        for (AppEntry entry : allApps) {
            boolean hasDomainURLs =
                    (entry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;
            if (hasDomainURLs) countAppWithDomainURLs++;
        }
        String summary = getResources().getQuantityString(
                R.plurals.domain_urls_apps_summary, countAppWithDomainURLs, countAppWithDomainURLs);
        mAppDomainURLsPreference.setSummary(summary);
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
        mPermissionsInfo = new PermissionsInfo(getActivity(), this);
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
        updateUI();
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

    @Override
    public void onPermissionLoadComplete() {
        Activity activity = getActivity();
        if (activity == null) return;
        mAppPermsPreference.setSummary(activity.getString(R.string.app_permissions_summary,
                mPermissionsInfo.getRuntimePermAppsGrantedCount(),
                mPermissionsInfo.getRuntimePermAppsCount()));
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                boolean enabled) {
            ArrayList<SearchIndexableResource> result = new ArrayList<>(1);
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.advanced_apps;
            result.add(sir);
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<>(1);
            if (!DefaultEmergencyPreference.isAvailable(context)) {
                result.add(KEY_DEFAULT_EMERGENCY_APP);
            }
            return result;
        }
    };
}
