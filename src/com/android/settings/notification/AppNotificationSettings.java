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

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_BLOCK = "block";

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private List<NotificationChannelGroup> mChannelGroupList;
    private List<PreferenceCategory> mChannelGroups = new ArrayList();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAppRow == null) return;
        if (!mDashboardFeatureProvider.isEnabled()) {
            AppHeader.createAppHeader(this, mAppRow.icon, mAppRow.label, mAppRow.pkg, mAppRow.uid,
                    mAppRow.settingsIntent);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            toastAndFinish();
            return;
        }
        final Activity activity = getActivity();
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(activity).getDashboardFeatureProvider(activity);

        addPreferencesFromResource(R.xml.app_notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);

        mBlock = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BLOCK);
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);

        if (mPkgInfo != null) {
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
                    populateChannelList();
                }
            }.execute();
        }
        if (mDashboardFeatureProvider.isEnabled()) {
            final Preference pref = FeatureFactory.getFactory(activity)
                    .getApplicationFeatureProvider(activity)
                    .newAppHeaderController(this /* fragment */, null /* appHeader */)
                    .setIcon(mAppRow.icon)
                    .setLabel(mAppRow.label)
                    .setPackageName(mAppRow.pkg)
                    .setUid(mAppRow.uid)
                    .setAppNotifPrefIntent(mAppRow.settingsIntent)
                    .setButtonActions(AppHeaderController.ActionType.ACTION_APP_INFO,
                            AppHeaderController.ActionType.ACTION_NOTIF_PREFERENCE)
                    .done(getPrefContext());
            getPreferenceScreen().addPreference(pref);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }
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
        } else {
            for (NotificationChannelGroup group : mChannelGroupList) {
                PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
                if (group.getName() == null) {
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
                    MasterSwitchPreference channelPref = new MasterSwitchPreference(
                            getPrefContext());
                    channelPref.setDisabledByAdmin(mSuspendedAppsAdmin);
                    channelPref.setKey(channel.getId());
                    channelPref.setTitle(channel.getName());
                    channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);

                    if (channel.isDeleted()) {
                        channelPref.setTitle(
                                getString(R.string.deleted_channel_name, channel.getName()));
                        channelPref.setEnabled(false);
                    } else {
                        channelPref.setSummary(getImportanceSummary(channel.getImportance()));
                        Bundle channelArgs = new Bundle();
                        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
                        channelArgs.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, true);
                        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
                        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
                        Intent channelIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                                ChannelNotificationSettings.class.getName(),
                                channelArgs, null, 0, null, false, getMetricsCategory());
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
                                        mBackend.updateChannel(mPkg, mUid, channel);

                                        return true;
                                    }
                                });
                    }
                    groupCategory.addPreference(channelPref);
                }
            }
        }
        updateDependents(mAppRow.banned);
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

    private void setupBlock() {
        if (mAppRow.systemApp) {
            setVisible(mBlock, false);
        } else {
            mBlock.setDisabledByAdmin(mSuspendedAppsAdmin);
            mBlock.setChecked(mAppRow.banned);
            mBlock.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference,
                                Object newValue) {
                            final boolean blocked = (Boolean) newValue;
                            mBackend.setNotificationsEnabledForPackage(mPkgInfo.packageName, mUid,
                                    !blocked);
                            updateDependents(blocked);
                            return true;
                        }
                    });
        }
    }

    private void updateDependents(boolean banned) {
        for (PreferenceCategory category : mChannelGroups) {
            setVisible(category, !banned);
        }
        setVisible(mBadge, !banned);
    }

    private Comparator<NotificationChannel> mChannelComparator =
            new Comparator<NotificationChannel>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(NotificationChannel left, NotificationChannel right) {
            if (left.isDeleted() != right.isDeleted()) {
                return Boolean.compare(left.isDeleted(), right.isDeleted());
            }
            if (!Objects.equals(left.getName(), right.getName())) {
                return sCollator.compare(left.getName().toString(), right.getName().toString());
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
                    // sort rest of the groups by name
                    if (!Objects.equals(left.getName(), right.getName())) {
                        return sCollator.compare(left.getName().toString(),
                                right.getName().toString());
                    }
                    return left.getId().compareTo(right.getId());
                }
            };
}
