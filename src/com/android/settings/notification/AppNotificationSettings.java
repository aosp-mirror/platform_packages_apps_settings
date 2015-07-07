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

package com.android.settings.notification;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.notification.NotificationBackend.AppRow;

import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends SettingsPreferenceFragment {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_BLOCK = "block";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_PEEKABLE = "peekable";
    private static final String KEY_SENSITIVE = "sensitive";
    private static final String KEY_APP_SETTINGS = "app_settings";

    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT
            = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES);

    private final NotificationBackend mBackend = new NotificationBackend();

    private Context mContext;
    private SwitchPreference mBlock;
    private SwitchPreference mPriority;
    private SwitchPreference mPeekable;
    private SwitchPreference mSensitive;
    private AppRow mAppRow;
    private boolean mCreated;
    private boolean mIsSystemPackage;
    private int mUid;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onActivityCreated mCreated=" + mCreated);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mAppRow == null) return;
        AppHeader.createAppHeader(this, mAppRow.icon, mAppRow.label,
                AppInfoWithHeader.getInfoIntent(this, mAppRow.pkg));
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_APP_NOTIFICATION;
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

        final String pkg = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_NAME)
                ? args.getString(AppInfoBase.ARG_PACKAGE_NAME)
                : intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? args.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
        if (mUid == -1 || TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg + ", "
                    + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }

        if (DEBUG) Log.d(TAG, "Load details for pkg=" + pkg + " uid=" + mUid);
        final PackageManager pm = getPackageManager();
        final PackageInfo info = findPackageInfo(pm, pkg, mUid);
        if (info == null) {
            Log.w(TAG, "Failed to find package info: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg
                    + ", " + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }
        mIsSystemPackage = Utils.isSystemPackage(pm, info);

        addPreferencesFromResource(R.xml.app_notification_settings);
        mBlock = (SwitchPreference) findPreference(KEY_BLOCK);
        mPriority = (SwitchPreference) findPreference(KEY_PRIORITY);
        mPeekable = (SwitchPreference) findPreference(KEY_PEEKABLE);
        mSensitive = (SwitchPreference) findPreference(KEY_SENSITIVE);

        mAppRow = mBackend.loadAppRow(pm, info.applicationInfo);

        // load settings intent
        ArrayMap<String, AppRow> rows = new ArrayMap<String, AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(getPackageManager(), rows);

        mBlock.setChecked(mAppRow.banned);
        updateDependents(mAppRow.banned);
        mPriority.setChecked(mAppRow.priority);
        mPeekable.setChecked(mAppRow.peekable);
        mSensitive.setChecked(mAppRow.sensitive);

        mBlock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean banned = (Boolean) newValue;
                if (banned) {
                    MetricsLogger.action(getActivity(), MetricsLogger.ACTION_BAN_APP_NOTES, pkg);
                }
                final boolean success =  mBackend.setNotificationsBanned(pkg, mUid, banned);
                if (success) {
                    updateDependents(banned);
                }
                return success;
            }
        });

        mPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean priority = (Boolean) newValue;
                return mBackend.setHighPriority(pkg, mUid, priority);
            }
        });

        mPeekable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean peekable = (Boolean) newValue;
                return mBackend.setPeekable(pkg, mUid, peekable);
            }
        });

        mSensitive.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean sensitive = (Boolean) newValue;
                return mBackend.setSensitive(pkg, mUid, sensitive);
            }
        });

        if (mAppRow.settingsIntent != null) {
            findPreference(KEY_APP_SETTINGS).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mContext.startActivity(mAppRow.settingsIntent);
                    return true;
                }
            });
        } else {
            removePreference(KEY_APP_SETTINGS);
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

    private void updateDependents(boolean banned) {
        final boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(
                UserHandle.myUserId());
        final boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();

        setVisible(mBlock, !mIsSystemPackage);
        setVisible(mPriority, mIsSystemPackage || !banned);
        setVisible(mPeekable, mIsSystemPackage || !banned);
        setVisible(mSensitive, mIsSystemPackage || !banned && lockscreenSecure
                && lockscreenNotificationsEnabled && allowPrivate);
    }

    private void setVisible(Preference p, boolean visible) {
        final boolean isVisible = getPreferenceScreen().findPreference(p.getKey()) != null;
        if (isVisible == visible) return;
        if (visible) {
            getPreferenceScreen().addPreference(p);
        } else {
            getPreferenceScreen().removePreference(p);
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    private static PackageInfo findPackageInfo(PackageManager pm, String pkg, int uid) {
        final String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }

    public static List<ResolveInfo> queryNotificationConfigActivities(PackageManager pm) {
        if (DEBUG) Log.d(TAG, "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is "
                + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(
                APP_NOTIFICATION_PREFS_CATEGORY_INTENT,
                0 //PackageManager.MATCH_DEFAULT_ONLY
        );
        return resolveInfos;
    }

    public static void collectConfigActivities(PackageManager pm, ArrayMap<String, AppRow> rows) {
        final List<ResolveInfo> resolveInfos = queryNotificationConfigActivities(pm);
        applyConfigActivities(pm, rows, resolveInfos);
    }

    public static void applyConfigActivities(PackageManager pm, ArrayMap<String, AppRow> rows,
            List<ResolveInfo> resolveInfos) {
        if (DEBUG) Log.d(TAG, "Found " + resolveInfos.size() + " preference activities"
                + (resolveInfos.size() == 0 ? " ;_;" : ""));
        for (ResolveInfo ri : resolveInfos) {
            final ActivityInfo activityInfo = ri.activityInfo;
            final ApplicationInfo appInfo = activityInfo.applicationInfo;
            final AppRow row = rows.get(appInfo.packageName);
            if (row == null) {
                if (DEBUG) Log.v(TAG, "Ignoring notification preference activity ("
                        + activityInfo.name + ") for unknown package "
                        + activityInfo.packageName);
                continue;
            }
            if (row.settingsIntent != null) {
                if (DEBUG) Log.v(TAG, "Ignoring duplicate notification preference activity ("
                        + activityInfo.name + ") for package "
                        + activityInfo.packageName);
                continue;
            }
            row.settingsIntent = new Intent(APP_NOTIFICATION_PREFS_CATEGORY_INTENT)
                    .setClassName(activityInfo.packageName, activityInfo.name);
        }
    }
}
