/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppInfoBase;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

abstract public class NotificationSettingsBase extends SettingsPreferenceFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected static final String ARG_PACKAGE_INFO = "arg_info";

    protected static final String KEY_BYPASS_DND = "bypass_dnd";
    protected static final String KEY_SENSITIVE = "sensitive";
    protected static final String KEY_IMPORTANCE = "importance";

    protected PackageManager mPm;
    protected final NotificationBackend mBackend = new NotificationBackend();
    protected Context mContext;
    protected boolean mCreated;
    protected int mUid;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected ImportanceSeekBarPreference mImportance;
    protected SwitchPreference mPriority;
    protected SwitchPreference mSensitive;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onActivityCreated mCreated=" + mCreated);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null && args == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        mPm = getPackageManager();

        mPkg = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_NAME)
                ? args.getString(AppInfoBase.ARG_PACKAGE_NAME)
                : intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? args.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
        if (mUid == -1 || TextUtils.isEmpty(mPkg)) {
            Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_PACKAGE + " was " + mPkg + ", "
                    + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }

        if (DEBUG) Log.d(TAG, "Load details for pkg=" + mPkg + " uid=" + mUid);
        mPkgInfo = args != null && args.containsKey(ARG_PACKAGE_INFO)
                ? (PackageInfo) args.getParcelable(ARG_PACKAGE_INFO) : null;
        if (mPkgInfo == null) {
            mPkgInfo = findPackageInfo(mPkg, mUid);
        }
        if (mPkgInfo == null) {
            Log.w(TAG, "Failed to find package info: " + Settings.EXTRA_APP_PACKAGE + " was " + mPkg
                    + ", " + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid != -1 && getPackageManager().getPackagesForUid(mUid) == null) {
            // App isn't around anymore, must have been removed.
            finish();
        }
    }

    protected void setupImportancePref(final NotificationBackend.AppRow appRow,
            final Notification.Topic topic, int importance) {
        if (mImportance == null) {
            mImportance = new ImportanceSeekBarPreference(getPrefContext());
            mImportance.setTitle(R.string.notification_importance_title);
            mImportance.setKey(KEY_IMPORTANCE);
            getPreferenceScreen().addPreference(mImportance);
        }
        mImportance.setSystemApp(appRow.systemApp);
        mImportance.setMinimumProgress(
                appRow.systemApp ? Ranking.IMPORTANCE_LOW : Ranking.IMPORTANCE_NONE);
        mImportance.setMax(Ranking.IMPORTANCE_MAX);
        // TODO: stop defaulting to 'normal' in the UI when there are mocks for this scenario.
        importance = importance == Ranking.IMPORTANCE_UNSPECIFIED
                        ? Ranking.IMPORTANCE_DEFAULT
                        : importance;
        mImportance.setProgress(importance);
        mImportance.setCallback(new ImportanceSeekBarPreference.Callback() {
            @Override
            public void onImportanceChanged(int progress) {
                mBackend.setImportance(appRow.pkg, appRow.uid, topic, progress);
                updateDependents(progress);
            }
        });
    }

    protected void setupPriorityPref(final Notification.Topic topic, boolean priority) {
        if (mPriority == null) {
            mPriority = new SwitchPreference(getPrefContext());
            mPriority.setTitle(R.string.app_notification_override_dnd_title);
            mPriority.setSummary(R.string.app_notification_override_dnd_summary);
            mPriority.setKey(KEY_BYPASS_DND);
            getPreferenceScreen().addPreference(mPriority);
        }
        mPriority.setChecked(priority);
        mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                return mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, topic, bypassZenMode);
            }
        });
    }

    protected void setupSensitivePref(final Notification.Topic topic, boolean sensitive) {
        if (mSensitive == null) {
            mSensitive = new SwitchPreference(getPrefContext());
            mSensitive.setTitle(R.string.app_notification_sensitive_title);
            mSensitive.setSummary(R.string.app_notification_sensitive_summary);
            mSensitive.setKey(KEY_SENSITIVE);
            getPreferenceScreen().addPreference(mSensitive);
        }
        mSensitive.setChecked(sensitive);
        mSensitive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean sensitive = (Boolean) newValue;
                return mBackend.setSensitive(mPkgInfo.packageName, mUid, topic, sensitive);
            }
        });
    }

    abstract void updateDependents(int progress);

    protected void setVisible(Preference p, boolean visible) {
        final boolean isVisible = getPreferenceScreen().findPreference(p.getKey()) != null;
        if (isVisible == visible) return;
        if (visible) {
            getPreferenceScreen().addPreference(p);
        } else {
            getPreferenceScreen().removePreference(p);
        }
    }

    protected void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    private PackageInfo findPackageInfo(String pkg, int uid) {
        final String[] packages = mPm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return mPm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }
}
