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
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settings.applications.LayoutPreference;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

abstract public class NotificationSettingsBase extends SettingsPreferenceFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected static final String ARG_PACKAGE_INFO = "arg_info";
    protected static final String ARG_TOPIC = "arg_topic";

    protected static final String KEY_BYPASS_DND = "bypass_dnd";
    protected static final String KEY_SENSITIVE = "sensitive";
    protected static final String KEY_IMPORTANCE = "importance";
    protected static final String KEY_IMPORTANCE_TITLE = "importance_title";
    protected static final String KEY_IMPORTANCE_RESET = "importance_reset_button";

    protected PackageManager mPm;
    protected final NotificationBackend mBackend = new NotificationBackend();
    protected Context mContext;
    protected boolean mCreated;
    protected int mUid;
    protected int mUserId;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected Notification.Topic mTopic;
    protected ImportanceSeekBarPreference mImportance;
    protected Preference mImportanceTitle;
    protected LayoutPreference mImportanceReset;
    protected RestrictedSwitchPreference mPriority;
    protected RestrictedSwitchPreference mSensitive;

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
        mUserId = UserHandle.getUserId(mUid);

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

        // Will be null for app wide settings.
        mTopic = args != null && args.containsKey(ARG_TOPIC)
                ? (Notification.Topic) args.getParcelable(ARG_TOPIC) : null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((mUid != -1 && getPackageManager().getPackagesForUid(mUid) == null)) {
            // App isn't around anymore, must have been removed.
            finish();
            return;
        }
        EnforcedAdmin admin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
        if (mImportance != null) {
            mImportance.setDisabledByAdmin(admin);
        }
        if (mPriority != null) {
            mPriority.setDisabledByAdmin(admin);
        }
        if (mSensitive != null) {
            mSensitive.setDisabledByAdmin(admin);
        }
    }

    protected void setupImportancePrefs(boolean isSystemApp, int importance) {
        mImportance.setDisabledByAdmin(
                RestrictedLockUtils.checkIfApplicationIsSuspended(mContext, mPkg, mUserId));
        if (importance == Ranking.IMPORTANCE_UNSPECIFIED) {
            mImportance.setVisible(false);
            mImportanceReset.setVisible(false);
            mImportanceTitle.setOnPreferenceClickListener(showEditableImportance);
        } else {
            mImportanceTitle.setOnPreferenceClickListener(null);
        }

        mImportanceTitle.setSummary(getProgressSummary(importance));
        mImportance.setSystemApp(isSystemApp);
        mImportance.setMinimumProgress(
                isSystemApp ? Ranking.IMPORTANCE_LOW : Ranking.IMPORTANCE_NONE);
        mImportance.setMax(Ranking.IMPORTANCE_MAX);
        mImportance.setProgress(importance);
        mImportance.setCallback(new ImportanceSeekBarPreference.Callback() {
            @Override
            public void onImportanceChanged(int progress) {
                mBackend.setImportance(mPkg, mUid, mTopic, progress);
                mImportanceTitle.setSummary(getProgressSummary(progress));
                updateDependents(progress);
            }
        });

        Button button = (Button) mImportanceReset.findViewById(R.id.left_button);
        button.setText(R.string.importance_reset);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackend.setImportance(mPkg, mUid, mTopic, Ranking.IMPORTANCE_UNSPECIFIED);
                mImportanceReset.setVisible(false);
                mImportance.setVisible(false);
                mImportanceTitle.setOnPreferenceClickListener(showEditableImportance);
                mImportanceTitle.setSummary(getProgressSummary(Ranking.IMPORTANCE_UNSPECIFIED));
                updateDependents(Ranking.IMPORTANCE_UNSPECIFIED);
            }
        });
        mImportanceReset.findViewById(R.id.right_button).setVisibility(View.INVISIBLE);
    }

    private String getProgressSummary(int progress) {
        switch (progress) {
            case Ranking.IMPORTANCE_NONE:
                return mContext.getString(R.string.notification_importance_blocked);
            case Ranking.IMPORTANCE_LOW:
                return mContext.getString(R.string.notification_importance_low);
            case Ranking.IMPORTANCE_DEFAULT:
                return mContext.getString(R.string.notification_importance_default);
            case Ranking.IMPORTANCE_HIGH:
                return mContext.getString(R.string.notification_importance_high);
            case Ranking.IMPORTANCE_MAX:
                return mContext.getString(R.string.notification_importance_max);
            case Ranking.IMPORTANCE_UNSPECIFIED:
                return mContext.getString(R.string.notification_importance_none);
            default:
                return "";
        }
    }

    protected void setupPriorityPref(boolean priority) {
        mPriority.setDisabledByAdmin(
                RestrictedLockUtils.checkIfApplicationIsSuspended(mContext, mPkg, mUserId));
        mPriority.setChecked(priority);
        mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                return mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, mTopic, bypassZenMode);
            }
        });
    }

    protected void setupSensitivePref(boolean sensitive) {
        mSensitive.setDisabledByAdmin(
                RestrictedLockUtils.checkIfApplicationIsSuspended(mContext, mPkg, mUserId));
        mSensitive.setChecked(sensitive);
        mSensitive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean sensitive = (Boolean) newValue;
                return mBackend.setSensitive(mPkgInfo.packageName, mUid, mTopic, sensitive);
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

    private Preference.OnPreferenceClickListener showEditableImportance =
            new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mBackend.setImportance(mPkg, mUid, mTopic, Ranking.IMPORTANCE_DEFAULT);
                    mImportance.setProgress(Ranking.IMPORTANCE_DEFAULT);
                    mImportanceTitle.setSummary(getProgressSummary(Ranking.IMPORTANCE_DEFAULT));
                    mImportance.setVisible(true);
                    mImportanceReset.setVisible(true);
                    mImportanceTitle.setOnPreferenceClickListener(null);
                    return true;
                }
            };
}
