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
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.MasterCheckBoxPreference;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wrapper.NotificationChannelGroupWrapper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreference;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
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
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

abstract public class NotificationSettingsBase extends DashboardFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected PackageManager mPm;
    protected NotificationBackend mBackend = new NotificationBackend();
    protected NotificationManager mNm;
    protected Context mContext;

    protected int mUid;
    protected int mUserId;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected EnforcedAdmin mSuspendedAppsAdmin;
    protected NotificationChannelGroupWrapper mChannelGroup;
    protected NotificationChannel mChannel;
    protected NotificationBackend.AppRow mAppRow;

    protected boolean mShowLegacyChannelConfig = false;
    protected boolean mListeningToPackageRemove;

    protected List<NotificationPreferenceController> mControllers = new ArrayList<>();
    protected List<Preference> mDynamicPreferences = new ArrayList<>();
    protected ImportanceListener mImportanceListener = new ImportanceListener();

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
        if (mAppRow == null) {
            Log.w(TAG, "Can't load package");
            finish();
            return;
        }
        collectConfigActivities();
        Bundle args = getArguments();
        mChannel = (args != null && args.containsKey(Settings.EXTRA_CHANNEL_ID)) ?
                mBackend.getChannel(mPkg, mUid, args.getString(Settings.EXTRA_CHANNEL_ID)) : null;

        NotificationChannelGroup group = null;

        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);

        mShowLegacyChannelConfig = mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)
                || (mChannel != null
                && NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId()));

        if (mShowLegacyChannelConfig) {
            mChannel = mBackend.getChannel(
                    mAppRow.pkg, mAppRow.uid, NotificationChannel.DEFAULT_CHANNEL_ID);
        }
        if (mChannel != null && !TextUtils.isEmpty(mChannel.getGroup())) {
            group = mBackend.getGroup(mPkg, mUid, mChannel.getGroup());
            if (group != null) {
                mChannelGroup = new NotificationChannelGroupWrapper(group);
            }
        }
    }

    protected void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    protected void collectConfigActivities() {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES)
                .setPackage(mAppRow.pkg);
        final List<ResolveInfo> resolveInfos = mPm.queryIntentActivities(
                intent,
                0 //PackageManager.MATCH_DEFAULT_ONLY
        );
        if (DEBUG) {
            Log.d(TAG, "Found " + resolveInfos.size() + " preference activities"
                    + (resolveInfos.size() == 0 ? " ;_;" : ""));
        }
        for (ResolveInfo ri : resolveInfos) {
            final ActivityInfo activityInfo = ri.activityInfo;
            if (mAppRow.settingsIntent != null) {
                if (DEBUG) {
                    Log.d(TAG, "Ignoring duplicate notification preference activity ("
                            + activityInfo.name + ") for package "
                            + activityInfo.packageName);
                }
                continue;
            }
            mAppRow.settingsIntent = intent
                    .setPackage(null)
                    .setClassName(activityInfo.packageName, activityInfo.name)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (mChannel != null) {
                mAppRow.settingsIntent.putExtra(Notification.EXTRA_CHANNEL_ID, mChannel.getId());
            }
            if (mChannelGroup != null) {
                mAppRow.settingsIntent.putExtra(
                        Notification.EXTRA_CHANNEL_GROUP_ID, mChannelGroup.getGroup().getId());
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

    protected void populateGroupToggle(final PreferenceGroup parent,
            NotificationChannelGroup group) {
        RestrictedSwitchPreference preference = new RestrictedSwitchPreference(getPrefContext());
        preference.setTitle(R.string.notification_switch_label);
        preference.setEnabled(mSuspendedAppsAdmin == null
                && isChannelGroupBlockable(group));
        preference.setChecked(!group.isBlocked());
        preference.setOnPreferenceClickListener(preference1 -> {
            final boolean allowGroup = ((SwitchPreference) preference1).isChecked();
            group.setBlocked(!allowGroup);
            mBackend.updateChannelGroup(mAppRow.pkg, mAppRow.uid, group);

            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref instanceof MasterSwitchPreference) {
                    ((MasterSwitchPreference) pref).setSwitchEnabled(allowGroup);
                }
            }
            return true;
        });

        parent.addPreference(preference);
    }

    protected Preference populateSingleChannelPrefs(PreferenceGroup parent,
            final NotificationChannel channel, final boolean groupBlocked) {
        MasterCheckBoxPreference channelPref = new MasterCheckBoxPreference(
                getPrefContext());
        channelPref.setCheckBoxEnabled(mSuspendedAppsAdmin == null
                && isChannelBlockable(channel)
                && isChannelConfigurable(channel)
                && !groupBlocked);
        channelPref.setKey(channel.getId());
        channelPref.setTitle(channel.getName());
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        Intent channelIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                ChannelNotificationSettings.class.getName(),
                channelArgs, null, R.string.notification_channel_title, null, false,
                getMetricsCategory());
        channelPref.setIntent(channelIntent);

        channelPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object o) {
                        boolean value = (Boolean) o;
                        int importance = value ? IMPORTANCE_LOW : IMPORTANCE_NONE;
                        channel.setImportance(importance);
                        channel.lockFields(
                                NotificationChannel.USER_LOCKED_IMPORTANCE);
                        mBackend.updateChannel(mPkg, mUid, channel);

                        return true;
                    }
                });
        parent.addPreference(channelPref);
        return channelPref;
    }

    protected boolean isChannelConfigurable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            return !channel.getId().equals(mAppRow.lockedChannelId);
        }
        return false;
    }

    protected boolean isChannelBlockable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return channel.isBlockableSystem()
                    || channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
        }
        return false;
    }

    protected boolean isChannelGroupBlockable(NotificationChannelGroup group) {
        if (group != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return group.isBlocked();
        }
        return false;
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
                if (DEBUG) {
                    Log.d(TAG, "Package (" + packageName + ") removed. Removing"
                            + "NotificationSettingsBase.");
                }
                onPackageRemoved();
            }
        }
    };

    protected Comparator<NotificationChannel> mChannelComparator =
            (left, right) -> {
                if (left.isDeleted() != right.isDeleted()) {
                    return Boolean.compare(left.isDeleted(), right.isDeleted());
                }
                return left.getId().compareTo(right.getId());
            };

    protected class ImportanceListener {
        protected void onImportanceChanged() {
            final PreferenceScreen screen = getPreferenceScreen();
            for (NotificationPreferenceController controller : mControllers) {
                controller.displayPreference(screen);
            }
            updatePreferenceStates();

            boolean hideDynamicFields = false;
            if (mAppRow == null || mAppRow.banned) {
                hideDynamicFields = true;
            } else {
                if (mChannel != null) {
                    hideDynamicFields = mChannel.getImportance() == IMPORTANCE_NONE;
                } else if (mChannelGroup != null) {
                    hideDynamicFields = mChannelGroup.isBlocked();
                }
            }
            for (Preference preference : mDynamicPreferences) {
                setVisible(getPreferenceScreen(), preference, !hideDynamicFields);
            }
        }
    }
}
