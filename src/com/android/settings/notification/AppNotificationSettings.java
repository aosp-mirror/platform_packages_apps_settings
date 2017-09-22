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

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static String KEY_GENERAL_CATEGORY = "categories";
    private static String KEY_DELETED = "deleted";

    private List<NotificationChannelGroup> mChannelGroupList;
    private List<PreferenceCategory> mChannelGroups = new ArrayList();
    private FooterPreference mDeletedChannels;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
            mChannelGroups.clear();
            mDeletedChannels = null;
            mShowLegacyChannelConfig = false;
        }

        addPreferencesFromResource(R.xml.notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);
        setupBlock();
        addHeaderPref();

        mShowLegacyChannelConfig = mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid);
        if (mShowLegacyChannelConfig) {
            mChannel = mBackend.getChannel(
                    mAppRow.pkg, mAppRow.uid, NotificationChannel.DEFAULT_CHANNEL_ID);
            populateDefaultChannelPrefs();
        } else {
            addPreferencesFromResource(R.xml.upgraded_app_notification_settings);
            setupBadge();
            // Load channel settings
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... unused) {
                    mChannelGroupList = mBackend.getGroups(mPkg, mUid).getList();
                    Collections.sort(mChannelGroupList, mChannelGroupComparator);
                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {
                    if (getHost() == null) {
                        return;
                    }
                    populateList();
                    addAppLinkPref();
                }
            }.execute();
        }

        updateDependents(mAppRow.banned);
    }

    private void addHeaderPref() {
        ArrayMap<String, AppRow> rows = new ArrayMap<>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this /* fragment */, null /* header */)
                .setRecyclerView(getListView(), getLifecycle())
                .setIcon(mAppRow.icon)
                .setLabel(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE)
                .done(activity, getPrefContext());
        pref.setKey(KEY_HEADER);
        getPreferenceScreen().addPreference(pref);
    }

    private void populateList() {
        if (!mChannelGroups.isEmpty()) {
            // If there's anything in mChannelGroups, we've called populateChannelList twice.
            // Clear out existing channels and log.
            Log.w(TAG, "Notification channel group posted twice to settings - old size " +
                    mChannelGroups.size() + ", new size " + mChannelGroupList.size());
            for (Preference p : mChannelGroups) {
                getPreferenceScreen().removePreference(p);
            }
        }
        if (mChannelGroupList.isEmpty()) {
            PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
            groupCategory.setTitle(R.string.notification_channels);
            groupCategory.setKey(KEY_GENERAL_CATEGORY);
            getPreferenceScreen().addPreference(groupCategory);
            mChannelGroups.add(groupCategory);

            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            groupCategory.addPreference(empty);
        } else {
            populateGroupList();
            int deletedChannelCount = mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid);
            if (deletedChannelCount > 0 &&
                    getPreferenceScreen().findPreference(KEY_DELETED) == null) {
                mDeletedChannels = new FooterPreference(getPrefContext());
                mDeletedChannels.setSelectable(false);
                mDeletedChannels.setTitle(getResources().getQuantityString(
                        R.plurals.deleted_channels, deletedChannelCount, deletedChannelCount));
                mDeletedChannels.setEnabled(false);
                mDeletedChannels.setKey(KEY_DELETED);
                mDeletedChannels.setOrder(ORDER_LAST);
                getPreferenceScreen().addPreference(mDeletedChannels);
            }
        }
        updateDependents(mAppRow.banned);
    }

    private void populateGroupList() {
        PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
        groupCategory.setTitle(R.string.notification_channels);
        groupCategory.setKey(KEY_GENERAL_CATEGORY);
        groupCategory.setOrderingAsAdded(true);
        getPreferenceScreen().addPreference(groupCategory);
        mChannelGroups.add(groupCategory);
        for (NotificationChannelGroup group : mChannelGroupList) {
            final List<NotificationChannel> channels = group.getChannels();
            int N = channels.size();
            // app defined groups with one channel and channels with no group display the channel
            // name and no summary and link directly to the channel page unless the group is blocked
            if ((group.getId() == null || N < 2) && !group.isBlocked()) {
                Collections.sort(channels, mChannelComparator);
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupCategory, channel, "");
                }
            } else {
                populateGroupPreference(groupCategory, group, N);
            }
        }
    }

    void populateGroupPreference(PreferenceGroup parent,
            final NotificationChannelGroup group, int channelCount) {
        MasterSwitchPreference groupPref = new MasterSwitchPreference(
                getPrefContext());
        groupPref.setSwitchEnabled(mSuspendedAppsAdmin == null
                && isChannelGroupBlockable(group));
        groupPref.setKey(group.getId());
        groupPref.setTitle(group.getName());
        groupPref.setChecked(!group.isBlocked());
        groupPref.setSummary(getResources().getQuantityString(
                R.plurals.notification_group_summary, channelCount, channelCount));
        Bundle groupArgs = new Bundle();
        groupArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        groupArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        groupArgs.putString(Settings.EXTRA_CHANNEL_GROUP_ID, group.getId());
        Intent groupIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                ChannelGroupNotificationSettings.class.getName(),
                groupArgs, null, R.string.notification_group_title, null, false,
                getMetricsCategory());
        groupPref.setIntent(groupIntent);

        groupPref.setOnPreferenceChangeListener(
                (preference, o) -> {
                    boolean value = (Boolean) o;
                    group.setBlocked(!value);
                    mBackend.updateChannelGroup(mPkg, mUid, group);

                    return true;
                });
        parent.addPreference(groupPref);
    }

    void setupBadge() {
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mBadge.setDisabledByAdmin(mSuspendedAppsAdmin);
        if (mChannel == null) {
            mBadge.setChecked(mAppRow.showBadge);
        } else {
            mBadge.setChecked(mChannel.canShowBadge());
        }
        mBadge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean value = (Boolean) newValue;
                if (mChannel == null) {
                    mBackend.setShowBadge(mPkg, mUid, value);
                } else {
                    mChannel.setShowBadge(value);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_SHOW_BADGE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                }
                return true;
            }
        });
    }

    protected void setupBlock() {
        View switchBarContainer = LayoutInflater.from(
                getPrefContext()).inflate(R.layout.styled_switch_bar, null);
        mSwitchBar = switchBarContainer.findViewById(R.id.switch_bar);
        mSwitchBar.show();
        mSwitchBar.setDisabledByAdmin(mSuspendedAppsAdmin);
        mSwitchBar.setChecked(!mAppRow.banned);
        mSwitchBar.addOnSwitchChangeListener(new SwitchBar.OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                if (mShowLegacyChannelConfig && mChannel != null) {
                    final int importance = isChecked ? IMPORTANCE_UNSPECIFIED : IMPORTANCE_NONE;
                    mImportanceToggle.setChecked(importance == IMPORTANCE_UNSPECIFIED);
                    mChannel.setImportance(importance);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                }
                mBackend.setNotificationsEnabledForPackage(mPkgInfo.packageName, mUid, isChecked);
                mAppRow.banned = true;
                updateDependents(!isChecked);
            }
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(ORDER_FIRST);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.app_notifications_off_desc);
    }

    protected void updateDependents(boolean banned) {
        for (PreferenceCategory category : mChannelGroups) {
            setVisible(category, !banned);
        }
        if (mDeletedChannels != null) {
            setVisible(mDeletedChannels, !banned);
        }
        setVisible(mBlockedDesc, banned);
        setVisible(mBadge, !banned);
        if (mShowLegacyChannelConfig) {
            setVisible(mImportanceToggle, !banned);
            setVisible(mPriority, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                    || (checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                    && mDndVisualEffectsSuppressed));
            setVisible(mVisibilityOverride, !banned &&
                    checkCanBeVisible(NotificationManager.IMPORTANCE_LOW) && isLockScreenSecure());
        }
        if (mAppLink != null) {
            setVisible(mAppLink, !banned);
        }
        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }
    }


    private Comparator<NotificationChannelGroup> mChannelGroupComparator =
            new Comparator<NotificationChannelGroup>() {

                @Override
                public int compare(NotificationChannelGroup left, NotificationChannelGroup right) {
                    // Non-grouped channels (in placeholder group with a null id) come last
                    if (left.getId() == null && right.getId() != null) {
                        return 1;
                    } else if (right.getId() == null && left.getId() != null) {
                        return -1;
                    }
                    return left.getId().compareTo(right.getId());
                }
            };
}
