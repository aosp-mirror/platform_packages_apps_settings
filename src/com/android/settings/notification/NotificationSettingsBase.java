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

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.FooterPreference;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.ArrayList;
import java.util.List;

abstract public class NotificationSettingsBase extends SettingsPreferenceFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT
            = new Intent(Intent.ACTION_MAIN)
            .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES);

    protected static final int ORDER_FIRST = -500;
    protected static final int ORDER_LAST = 1000;

    protected static final String KEY_APP_LINK = "app_link";
    protected static final String KEY_HEADER = "header";
    protected static final String KEY_BLOCK = "block";
    protected static final String KEY_BADGE = "badge";
    protected static final String KEY_BYPASS_DND = "bypass_dnd";
    protected static final String KEY_VISIBILITY_OVERRIDE = "visibility_override";
    protected static final String KEY_BLOCKED_DESC = "block_desc";
    protected static final String KEY_ALLOW_SOUND = "allow_sound";

    protected PackageManager mPm;
    protected UserManager mUm;
    protected NotificationBackend mBackend = new NotificationBackend();
    protected LockPatternUtils mLockPatternUtils;
    protected NotificationManager mNm;
    protected Context mContext;
    protected boolean mCreated;
    protected int mUid;
    protected int mUserId;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected RestrictedSwitchPreference mBadge;
    protected RestrictedSwitchPreference mPriority;
    protected RestrictedDropDownPreference mVisibilityOverride;
    protected RestrictedSwitchPreference mImportanceToggle;
    protected LayoutPreference mBlockBar;
    protected SwitchBar mSwitchBar;
    protected FooterPreference mBlockedDesc;
    protected Preference mAppLink;

    protected EnforcedAdmin mSuspendedAppsAdmin;
    protected boolean mDndVisualEffectsSuppressed;

    protected NotificationChannel mChannel;
    protected NotificationBackend.AppRow mAppRow;
    protected boolean mShowLegacyChannelConfig = false;

    protected boolean mListeningToPackageRemove;

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
        mNm = NotificationManager.from(mContext);

        mPkg = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_NAME)
                ? args.getString(AppInfoBase.ARG_PACKAGE_NAME)
                : intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? args.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : intent.getIntExtra(Settings.EXTRA_APP_UID, -1);

        if (mUid < 0) {
            try {
                mUid = mPm.getPackageUid(mPkg, 0);
            } catch (NameNotFoundException e) {
            }
        }

        mPkgInfo = findPackageInfo(mPkg, mUid);

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            toastAndFinish();
            return;
        }

        mUserId = UserHandle.getUserId(mUid);
        startListeningToPackageRemove();
    }

    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }
        mAppRow = mBackend.loadAppRow(mContext, mPm, mPkgInfo);
        Bundle args = getArguments();
        mChannel = (args != null && args.containsKey(Settings.EXTRA_CHANNEL_ID)) ?
                mBackend.getChannel(mPkg, mUid, args.getString(Settings.EXTRA_CHANNEL_ID)) : null;

        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
        NotificationManager.Policy policy = mNm.getNotificationPolicy();
        mDndVisualEffectsSuppressed = policy == null ? false : policy.suppressedVisualEffects != 0;

        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
    }

    protected void setVisible(Preference p, boolean visible) {
        setVisible(getPreferenceScreen(), p, visible);
    }

    protected void setVisible(PreferenceGroup parent, Preference p, boolean visible) {
        final boolean isVisible = parent.findPreference(p.getKey()) != null;
        if (isVisible == visible) return;
        if (visible) {
            parent.addPreference(p);
        } else {
            parent.removePreference(p);
        }
    }

    protected void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    private List<ResolveInfo> queryNotificationConfigActivities() {
        if (DEBUG) Log.d(TAG, "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is "
                + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
        final List<ResolveInfo> resolveInfos = mPm.queryIntentActivities(
                APP_NOTIFICATION_PREFS_CATEGORY_INTENT,
                0 //PackageManager.MATCH_DEFAULT_ONLY
        );
        return resolveInfos;
    }

    protected void collectConfigActivities(ArrayMap<String, NotificationBackend.AppRow> rows) {
        final List<ResolveInfo> resolveInfos = queryNotificationConfigActivities();
        applyConfigActivities(rows, resolveInfos);
    }

    private void applyConfigActivities(ArrayMap<String, NotificationBackend.AppRow> rows,
            List<ResolveInfo> resolveInfos) {
        if (DEBUG) Log.d(TAG, "Found " + resolveInfos.size() + " preference activities"
                + (resolveInfos.size() == 0 ? " ;_;" : ""));
        for (ResolveInfo ri : resolveInfos) {
            final ActivityInfo activityInfo = ri.activityInfo;
            final ApplicationInfo appInfo = activityInfo.applicationInfo;
            final NotificationBackend.AppRow row = rows.get(appInfo.packageName);
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
            if (mChannel != null) {
                row.settingsIntent.putExtra(Notification.EXTRA_CHANNEL_ID, mChannel.getId());
            }
        }
    }

    private PackageInfo findPackageInfo(String pkg, int uid) {
        if (pkg == null || uid < 0) {
            return null;
        }
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

    protected void addAppLinkPref() {
        if (mAppRow.settingsIntent != null && mAppLink == null) {
            addPreferencesFromResource(R.xml.inapp_notification_settings);
            mAppLink = (Preference) findPreference(KEY_APP_LINK);
            mAppLink.setIntent(mAppRow.settingsIntent);
        }
    }

    protected void populateDefaultChannelPrefs() {
        if (mPkgInfo != null && mChannel != null) {
            addPreferencesFromResource(R.xml.legacy_channel_notification_settings);
            setupPriorityPref(mChannel.canBypassDnd());
            setupVisOverridePref(mChannel.getLockscreenVisibility());
            setupImportanceToggle();
            setupBadge();
        }
        mSwitchBar.setChecked(!mAppRow.banned
                && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }

    abstract void setupBadge();

    abstract void updateDependents(boolean banned);

    // 'allow sound'
    private void setupImportanceToggle() {
        mImportanceToggle = (RestrictedSwitchPreference) findPreference(KEY_ALLOW_SOUND);
        mImportanceToggle.setDisabledByAdmin(mSuspendedAppsAdmin);
        mImportanceToggle.setEnabled(isChannelConfigurable(mChannel)
                && !mImportanceToggle.isDisabledByAdmin());
        mImportanceToggle.setChecked(mChannel.getImportance() >= IMPORTANCE_DEFAULT
                || mChannel.getImportance() == IMPORTANCE_UNSPECIFIED);
        mImportanceToggle.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final int importance =
                                ((Boolean) newValue ? IMPORTANCE_UNSPECIFIED : IMPORTANCE_LOW);
                        mChannel.setImportance(importance);
                        mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                        mBackend.updateChannel(mPkg, mUid, mChannel);
                        updateDependents(mChannel.getImportance() == IMPORTANCE_NONE);
                        return true;
                    }
                });
    }

    protected void setupPriorityPref(boolean priority) {
        mPriority = (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mPriority.setDisabledByAdmin(mSuspendedAppsAdmin);
        mPriority.setEnabled(isChannelConfigurable(mChannel) && !mPriority.isDisabledByAdmin());
        mPriority.setChecked(priority);
        mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                mChannel.setBypassDnd(bypassZenMode);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_PRIORITY);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });
    }

    protected void setupVisOverridePref(int sensitive) {
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        mVisibilityOverride.clearRestrictedItems();
        if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
            final String summaryShowEntry =
                    getString(R.string.lock_screen_notifications_summary_show);
            final String summaryShowEntryValue =
                    Integer.toString(NotificationManager.VISIBILITY_NO_OVERRIDE);
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

        if (sensitive == NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE) {
            mVisibilityOverride.setValue(Integer.toString(getGlobalVisibility()));
        } else {
            mVisibilityOverride.setValue(Integer.toString(sensitive));
        }
        mVisibilityOverride.setSummary("%s");

        mVisibilityOverride.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int sensitive = Integer.parseInt((String) newValue);
                        if (sensitive == getGlobalVisibility()) {
                            sensitive = NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
                        }
                        mChannel.setLockscreenVisibility(sensitive);
                        mChannel.lockFields(NotificationChannel.USER_LOCKED_VISIBILITY);
                        mBackend.updateChannel(mPkg, mUid, mChannel);
                        return true;
                    }
                });
        mVisibilityOverride.setDisabledByAdmin(mSuspendedAppsAdmin);
    }

    protected void setupBlockDesc(int summaryResId) {
        mBlockedDesc = (FooterPreference) getPreferenceScreen().findPreference(
                KEY_BLOCKED_DESC);
        mBlockedDesc = new FooterPreference(getPrefContext());
        mBlockedDesc.setSelectable(false);
        mBlockedDesc.setTitle(summaryResId);
        mBlockedDesc.setEnabled(false);
        mBlockedDesc.setOrder(50);
        getPreferenceScreen().addPreference(mBlockedDesc);
    }

    protected boolean checkCanBeVisible(int minImportanceVisible) {
        int importance = mChannel.getImportance();
        if (importance == NotificationManager.IMPORTANCE_UNSPECIFIED) {
            return true;
        }
        return importance >= minImportanceVisible;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry,
            CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, mUserId);
        if (admin != null) {
            RestrictedDropDownPreference.RestrictedItem item =
                    new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
            mVisibilityOverride.addRestrictedItem(item);
        }
    }

    private int getGlobalVisibility() {
        int globalVis = NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
        if (!getLockscreenNotificationsEnabled()) {
            globalVis = Notification.VISIBILITY_SECRET;
        } else if (!getLockscreenAllowPrivateNotifications()) {
            globalVis = Notification.VISIBILITY_PRIVATE;
        }
        return globalVis;
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    protected boolean isLockScreenSecure() {
        if (mLockPatternUtils == null) {
            mLockPatternUtils = new LockPatternUtils(getActivity());
        }
        boolean lockscreenSecure = mLockPatternUtils.isSecure(UserHandle.myUserId());
        UserInfo parentUser = mUm.getProfileParent(UserHandle.myUserId());
        if (parentUser != null){
            lockscreenSecure |= mLockPatternUtils.isSecure(parentUser.id);
        }

        return lockscreenSecure;
    }

    protected boolean isChannelConfigurable(NotificationChannel channel) {
        return !channel.getId().equals(mAppRow.lockedChannelId);
    }

    protected boolean isChannelBlockable(boolean systemApp, NotificationChannel channel) {
        if (!mAppRow.systemApp) {
            return true;
        }

        return channel.isBlockableSystem()
                || channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
    }

    protected void startListeningToPackageRemove() {
        if (mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = true;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getContext().registerReceiver(mPackageRemovedReceiver, filter);
    }

    protected void stopListeningToPackageRemove() {
        if (!mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = false;
        getContext().unregisterReceiver(mPackageRemovedReceiver);
    }

    protected void onPackageRemoved() {
        getActivity().finishAndRemoveTask();
    }

    protected final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (mPkgInfo == null || TextUtils.equals(mPkgInfo.packageName, packageName)) {
                if (DEBUG) Log.d(TAG, "Package (" + packageName + ") removed. Removing"
                        + "NotificationSettingsBase.");
                onPackageRemoved();
            }
        }
    };

    boolean hasValidSound(NotificationChannel channel) {
        return channel.getSound() != null && !Uri.EMPTY.equals(channel.getSound());
    }
}
