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

import android.app.Notification;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import static com.android.settings.notification.RestrictedDropDownPreference.RestrictedItem;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

abstract public class NotificationSettingsBase extends SettingsPreferenceFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String TUNER_SETTING = "show_importance_slider";

    protected static final String KEY_BYPASS_DND = "bypass_dnd";
    protected static final String KEY_VISIBILITY_OVERRIDE = "visibility_override";
    protected static final String KEY_IMPORTANCE = "importance";
    protected static final String KEY_BLOCK = "block";
    protected static final String KEY_SILENT = "silent";

    protected PackageManager mPm;
    protected UserManager mUm;
    protected final NotificationBackend mBackend = new NotificationBackend();
    protected Context mContext;
    protected boolean mCreated;
    protected int mUid;
    protected int mUserId;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected ImportanceSeekBarPreference mImportance;
    protected RestrictedSwitchPreference mPriority;
    protected RestrictedDropDownPreference mVisibilityOverride;
    protected RestrictedSwitchPreference mBlock;
    protected RestrictedSwitchPreference mSilent;
    protected EnforcedAdmin mSuspendedAppsAdmin;
    protected boolean mShowSlider = false;

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
        mUm = (UserManager) mContext.getSystemService(Context.USER_SERVICE);

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
        mPkgInfo = findPackageInfo(mPkg, mUid);
        if (mPkgInfo == null) {
            Log.w(TAG, "Failed to find package info: " + Settings.EXTRA_APP_PACKAGE + " was " + mPkg
                    + ", " + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }

        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
        mShowSlider = Settings.Secure.getInt(getContentResolver(), TUNER_SETTING, 0) == 1;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((mUid != -1 && getPackageManager().getPackagesForUid(mUid) == null)) {
            // App isn't around anymore, must have been removed.
            finish();
            return;
        }
        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
        if (mImportance != null) {
            mImportance.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
        if (mPriority != null) {
            mPriority.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
        if (mBlock != null) {
            mBlock.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
        if (mSilent != null) {
            mSilent.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
        if (mVisibilityOverride != null) {
            mVisibilityOverride.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
    }

    protected void setupImportancePrefs(boolean notBlockable, boolean notSilenceable,
                                        int importance, boolean banned) {
        if (mShowSlider && !notSilenceable) {
            setVisible(mBlock, false);
            setVisible(mSilent, false);
            mImportance.setDisabledByAdmin(mSuspendedAppsAdmin);
            mImportance.setMinimumProgress(
                    notBlockable ? Ranking.IMPORTANCE_MIN : Ranking.IMPORTANCE_NONE);
            mImportance.setMax(Ranking.IMPORTANCE_MAX);
            mImportance.setProgress(importance);
            mImportance.setAutoOn(importance == Ranking.IMPORTANCE_UNSPECIFIED);
            mImportance.setCallback(new ImportanceSeekBarPreference.Callback() {
                @Override
                public void onImportanceChanged(int progress, boolean fromUser) {
                    if (fromUser) {
                        mBackend.setImportance(mPkg, mUid, progress);
                    }
                    updateDependents(progress);
                }
            });
        } else {
            setVisible(mImportance, false);
            if (notBlockable) {
                setVisible(mBlock, false);
            } else {
                boolean blocked = importance == Ranking.IMPORTANCE_NONE || banned;
                mBlock.setChecked(blocked);
                mBlock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final boolean blocked = (Boolean) newValue;
                        final int importance =
                                blocked ? Ranking.IMPORTANCE_NONE : Ranking.IMPORTANCE_UNSPECIFIED;
                        mBackend.setImportance(mPkgInfo.packageName, mUid, importance);
                        updateDependents(importance);
                        return true;
                    }
                });
            }
            if (notSilenceable) {
                setVisible(mSilent, false);
            } else {
                mSilent.setChecked(importance == Ranking.IMPORTANCE_LOW);
                mSilent.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final boolean silenced = (Boolean) newValue;
                        final int importance =
                                silenced ? Ranking.IMPORTANCE_LOW : Ranking.IMPORTANCE_UNSPECIFIED;
                        mBackend.setImportance(mPkgInfo.packageName, mUid, importance);
                        updateDependents(importance);
                        return true;
                    }
                });
            }
            updateDependents(banned ? Ranking.IMPORTANCE_NONE : importance);
        }
    }

    protected void setupPriorityPref(boolean priority) {
        mPriority.setDisabledByAdmin(mSuspendedAppsAdmin);
        mPriority.setChecked(priority);
        mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                return mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, bypassZenMode);
            }
        });
    }

    protected void setupVisOverridePref(int sensitive) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        mVisibilityOverride.clearRestrictedItems();
        if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
            final String summaryShowEntry =
                    getString(R.string.lock_screen_notifications_summary_show);
            final String summaryShowEntryValue = Integer.toString(Ranking.VISIBILITY_NO_OVERRIDE);
            entries.add(summaryShowEntry);
            values.add(summaryShowEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS
                            | DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
        }

        final String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide);
        final String summaryHideEntryValue = Integer.toString(Notification.VISIBILITY_PRIVATE);
        entries.add(summaryHideEntry);
        values.add(summaryHideEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        entries.add(getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(Notification.VISIBILITY_SECRET));
        mVisibilityOverride.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mVisibilityOverride.setEntryValues(values.toArray(new CharSequence[values.size()]));

        if (sensitive == Ranking.VISIBILITY_NO_OVERRIDE) {
            mVisibilityOverride.setValue(Integer.toString(getGlobalVisibility()));
        } else {
            mVisibilityOverride.setValue(Integer.toString(sensitive));
        }
        mVisibilityOverride.setSummary("%s");

        mVisibilityOverride.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int sensitive = Integer.parseInt((String) newValue);
                if (sensitive == getGlobalVisibility()) {
                    sensitive = Ranking.VISIBILITY_NO_OVERRIDE;
                }
                mBackend.setVisibilityOverride(mPkgInfo.packageName, mUid, sensitive);
                return true;
            }
        });
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry,
            CharSequence entryValue, int keyguardNotificationFeatures) {
        EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                mContext, keyguardNotificationFeatures, mUserId);
        if (admin != null) {
            RestrictedItem item = new RestrictedItem(entry, entryValue, admin);
            mVisibilityOverride.addRestrictedItem(item);
        }
    }

    private int getGlobalVisibility() {
        int globalVis = Ranking.VISIBILITY_NO_OVERRIDE;
        if (!getLockscreenNotificationsEnabled()) {
            globalVis = Notification.VISIBILITY_SECRET;
        } else if (!getLockscreenAllowPrivateNotifications()) {
            globalVis = Notification.VISIBILITY_PRIVATE;
        }
        return globalVis;
    }

    protected boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    protected boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
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
