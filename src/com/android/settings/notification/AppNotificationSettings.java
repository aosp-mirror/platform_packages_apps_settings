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

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.FooterPreference;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedSwitchPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_IMPORTANCE = "allow_sound";

    private List<NotificationChannelGroup> mChannelGroupList;
    private List<PreferenceCategory> mChannelGroups = new ArrayList();
    private RestrictedSwitchPreference mImportanceToggle;
    private LayoutPreference mBlockBar;
    private FooterPreference mDeletedChannels;
    private SwitchBar mSwitchBar;

    private boolean mShowLegacyChannelConfig = false;

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
            mShowLegacyChannelConfig = false;
        }

        addPreferencesFromResource(R.xml.app_notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);

        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mBlockedDesc = (FooterPreference) getPreferenceScreen().findPreference(KEY_BLOCKED_DESC);

        setupBlock();
        setupBadge();
        // load settings intent
        ArrayMap<String, AppRow> rows = new ArrayMap<String, AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mChannelGroupList = mBackend.getChannelGroups(mPkg, mUid).getList();
                Collections.sort(mChannelGroupList, mChannelGroupComparator);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (getHost() == null) {
                    return;
                }
                populateChannelList();
            }
        }.execute();

        final Activity activity = getActivity();
        final Preference pref = FeatureFactory.getFactory(activity)
                .getApplicationFeatureProvider(activity)
                .newAppHeaderController(this /* fragment */, null /* appHeader */)
                .setIcon(mAppRow.icon)
                .setLabel(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setButtonActions(AppHeaderController.ActionType.ACTION_APP_INFO,
                        AppHeaderController.ActionType.ACTION_NOTIF_PREFERENCE)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);

        updateDependents(mAppRow.banned);
    }

    private void populateChannelList() {
        if (mChannelGroupList.isEmpty()) {
            PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
            groupCategory.setTitle(R.string.notification_channels);
            getPreferenceScreen().addPreference(groupCategory);
            mChannelGroups.add(groupCategory);

            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            groupCategory.addPreference(empty);

        } else if (mChannelGroupList.size() == 1 &&
                mChannelGroupList.get(0).getChannels().get(0).getId()
                        .equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
            // Legacy app using only default channel. Hoist default channel settings to main panel.
            mShowLegacyChannelConfig = true;
            mChannel = mChannelGroupList.get(0).getChannels().get(0);
            populateDefaultChannelPrefs();

        } else {
            for (NotificationChannelGroup group : mChannelGroupList) {
                PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
                if (group.getId() == null) {
                    groupCategory.setTitle(mChannelGroupList.size() > 1
                            ? R.string.notification_channels_other
                            : R.string.notification_channels);
                } else {
                    groupCategory.setTitle(group.getName());
                }
                groupCategory.setKey(group.getId());
                groupCategory.setOrderingAsAdded(true);
                getPreferenceScreen().addPreference(groupCategory);
                mChannelGroups.add(groupCategory);

                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, mChannelComparator);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupCategory, channel);
                }
            }

            if (mAppRow.settingsIntent != null) {
                Preference intentPref = new Preference(getPrefContext());
                intentPref.setIntent(mAppRow.settingsIntent);
                intentPref.setTitle(mContext.getString(R.string.app_settings_link));
                getPreferenceScreen().addPreference(intentPref);
            }

            int deletedChannelCount = mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid);
            if (deletedChannelCount > 0) {
                mDeletedChannels = new FooterPreference(getPrefContext());
                mDeletedChannels.setSelectable(false);
                mDeletedChannels.setTitle(getResources().getQuantityString(
                        R.plurals.deleted_channels, deletedChannelCount, deletedChannelCount));
                mDeletedChannels.setEnabled(false);
                getPreferenceScreen().addPreference(mDeletedChannels);
            }
        }
        updateDependents(mAppRow.banned);
    }

    private void populateSingleChannelPrefs(PreferenceCategory groupCategory,
            final NotificationChannel channel) {
        MasterSwitchPreference channelPref = new MasterSwitchPreference(
                getPrefContext());
        channelPref.setSwitchEnabled(mSuspendedAppsAdmin == null && !mAppRow.systemApp);
        channelPref.setKey(channel.getId());
        channelPref.setTitle(channel.getName());
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        channelPref.setSummary(getImportanceSummary(channel.getImportance()));
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, true);
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
                        int importance = value ?  IMPORTANCE_LOW : IMPORTANCE_NONE;
                        channel.setImportance(importance);
                        channel.lockFields(
                                NotificationChannel.USER_LOCKED_IMPORTANCE);
                        channelPref.setSummary(getImportanceSummary(channel.getImportance()));
                        mBackend.updateChannel(mPkg, mUid, channel);

                        return true;
                    }
                });
        groupCategory.addPreference(channelPref);
    }

    private void populateDefaultChannelPrefs() {
        addPreferencesFromResource(R.xml.legacy_channel_notification_settings);
        mPriority = (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        mImportanceToggle = (RestrictedSwitchPreference) findPreference(KEY_IMPORTANCE);

        if (mPkgInfo != null && mChannel != null) {
            setupPriorityPref(mChannel.canBypassDnd());
            setupVisOverridePref(mChannel.getLockscreenVisibility());
            setupImportanceToggle();
        }
        mSwitchBar.setChecked(!mAppRow.banned
                && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }

    // 'allow sound'
    private void setupImportanceToggle() {
        mImportanceToggle.setDisabledByAdmin(mSuspendedAppsAdmin);
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
                updateDependents(!isChecked);
            }
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(-500);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.app_notifications_off_desc);
    }

    private void setupBadge() {
        mBadge.setDisabledByAdmin(mSuspendedAppsAdmin);
        mBadge.setChecked(mAppRow.showBadge);
        mBadge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean value = (Boolean) newValue;
                mBackend.setShowBadge(mPkg, mUid, value);
                return true;
            }
        });
    }

    private void updateDependents(boolean banned) {
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
            setVisible(mPriority, !banned);
            setVisible(mVisibilityOverride, !banned);
        }
        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }
    }

    private Comparator<NotificationChannel> mChannelComparator =
            new Comparator<NotificationChannel>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(NotificationChannel left, NotificationChannel right) {
            if (left.isDeleted() != right.isDeleted()) {
                return Boolean.compare(left.isDeleted(), right.isDeleted());
            }
            return left.getId().compareTo(right.getId());
        }
    };

    private Comparator<NotificationChannelGroup> mChannelGroupComparator =
            new Comparator<NotificationChannelGroup>() {
                private final Collator sCollator = Collator.getInstance();

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
