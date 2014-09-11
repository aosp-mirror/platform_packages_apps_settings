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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.NotificationAppList.AppRow;
import com.android.settings.notification.NotificationAppList.Backend;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends SettingsPreferenceFragment {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_BLOCK = "block";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_SENSITIVE = "sensitive";

    static final String EXTRA_HAS_SETTINGS_INTENT = "has_settings_intent";
    static final String EXTRA_SETTINGS_INTENT = "settings_intent";

    private final Backend mBackend = new Backend();

    private Context mContext;
    private SwitchPreference mBlock;
    private SwitchPreference mPriority;
    private SwitchPreference mSensitive;
    private AppRow mAppRow;
    private boolean mCreated;

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
        final View content = getActivity().findViewById(R.id.main_content);
        final ViewGroup contentParent = (ViewGroup) content.getParent();
        final View bar = getActivity().getLayoutInflater().inflate(R.layout.app_notification_header,
                contentParent, false);

        final ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
        appIcon.setImageDrawable(mAppRow.icon);

        final TextView appName = (TextView) bar.findViewById(R.id.app_name);
        appName.setText(mAppRow.label);

        final View appSettings = bar.findViewById(R.id.app_settings);
        if (mAppRow.settingsIntent == null) {
            appSettings.setVisibility(View.GONE);
        } else {
            appSettings.setClickable(true);
            appSettings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(mAppRow.settingsIntent);
                }
            });
        }
        contentParent.addView(bar, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        final int uid = intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
        final String pkg = intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        if (uid == -1 || TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg + ", "
                    + Settings.EXTRA_APP_UID + " was " + uid);
            toastAndFinish();
            return;
        }

        if (DEBUG) Log.d(TAG, "Load details for pkg=" + pkg + " uid=" + uid);
        final PackageManager pm = getPackageManager();
        final PackageInfo info = findPackageInfo(pm, pkg, uid);
        if (info == null) {
            Log.w(TAG, "Failed to find package info: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg
                    + ", " + Settings.EXTRA_APP_UID + " was " + uid);
            toastAndFinish();
            return;
        }

        addPreferencesFromResource(R.xml.app_notification_settings);
        mBlock = (SwitchPreference) findPreference(KEY_BLOCK);
        mPriority = (SwitchPreference) findPreference(KEY_PRIORITY);
        mSensitive = (SwitchPreference) findPreference(KEY_SENSITIVE);

        final boolean secure = new LockPatternUtils(getActivity()).isSecure();
        final boolean enabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();
        if (!secure || !enabled || !allowPrivate) {
            getPreferenceScreen().removePreference(mSensitive);
        }

        mAppRow = NotificationAppList.loadAppRow(pm, info.applicationInfo, mBackend);
        if (intent.hasExtra(EXTRA_HAS_SETTINGS_INTENT)) {
            // use settings intent from extra
            if (intent.getBooleanExtra(EXTRA_HAS_SETTINGS_INTENT, false)) {
                mAppRow.settingsIntent = intent.getParcelableExtra(EXTRA_SETTINGS_INTENT);
            }
        } else {
            // load settings intent
            ArrayMap<String, AppRow> rows = new ArrayMap<String, AppRow>();
            rows.put(mAppRow.pkg, mAppRow);
            NotificationAppList.collectConfigActivities(getPackageManager(), rows);
        }

        mBlock.setChecked(mAppRow.banned);
        mPriority.setChecked(mAppRow.priority);
        if (mSensitive != null) {
            mSensitive.setChecked(mAppRow.sensitive);
        }

        mBlock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean block = (Boolean) newValue;
                return mBackend.setNotificationsBanned(pkg, uid, block);
            }
        });

        mPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean priority = (Boolean) newValue;
                return mBackend.setHighPriority(pkg, uid, priority);
            }
        });

        if (mSensitive != null) {
            mSensitive.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean sensitive = (Boolean) newValue;
                    return mBackend.setSensitive(pkg, uid, sensitive);
                }
            });
        }

        // Users cannot block notifications from system/signature packages
        if (Utils.isSystemPackage(pm, info)) {
            getPreferenceScreen().removePreference(mBlock);
            mPriority.setDependency(null); // don't have it depend on a preference that's gone
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
}
