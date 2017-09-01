/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelGroupNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelGroupSettings";

    private static String KEY_DELETED = "deleted";

    private EntityHeaderController mHeaderPref;
    private List<Preference> mChannels = new ArrayList();
    private FooterPreference mDeletedChannels;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_CHANNEL_GROUP;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannelGroup == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or group");
            finish();
            return;
        }

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.notification_settings);
        setupBlock();
        addHeaderPref();
        addAppLinkPref();
        addFooterPref();
        populateChannelList();

        updateDependents(mChannelGroup.isBlocked());
    }

    @Override
    void setupBadge() {

    }

    private void populateChannelList() {
        if (!mChannels.isEmpty()) {
            // If there's anything in mChannels, we've called populateChannelList twice.
            // Clear out existing channels and log.
            Log.w(TAG, "Notification channel group posted twice to settings - old size " +
                    mChannels.size() + ", new size " + mChannels.size());
            for (Preference p : mChannels) {
                getPreferenceScreen().removePreference(p);
            }
        }
        if (mChannelGroup.getChannels().isEmpty()) {
            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            getPreferenceScreen().addPreference(empty);
            mChannels.add(empty);

        } else {
            final List<NotificationChannel> channels = mChannelGroup.getChannels();
            Collections.sort(channels, mChannelComparator);
            for (NotificationChannel channel : channels) {
                mChannels.add(populateSingleChannelPrefs(
                        getPreferenceScreen(), channel, getImportanceSummary(channel)));
            }

            int deletedChannelCount = mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid);
            if (deletedChannelCount > 0) {
                mDeletedChannels = new FooterPreference(getPrefContext());
                mDeletedChannels.setSelectable(false);
                mDeletedChannels.setTitle(getResources().getQuantityString(
                        R.plurals.deleted_channels, deletedChannelCount, deletedChannelCount));
                mDeletedChannels.setEnabled(false);
                mDeletedChannels.setKey(KEY_DELETED);
                mDeletedChannels.setOrder(ORDER_LAST);
                getPreferenceScreen().addPreference(mDeletedChannels);
                mChannels.add(mDeletedChannels);
            }
        }

        updateDependents(mAppRow.banned);
    }

    private void addHeaderPref() {
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
        final Activity activity = getActivity();
        mHeaderPref = EntityHeaderController
                .newInstance(activity, this /* fragment */, null /* header */)
                .setRecyclerView(getListView(), getLifecycle());
        final Preference pref = mHeaderPref
                .setIcon(mAppRow.icon)
                .setLabel(mChannelGroup.getName())
                .setSummary(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .setHasAppInfoLink(true)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    private void addFooterPref() {
        if (!TextUtils.isEmpty(mChannelGroup.getDescription())) {
            FooterPreference descPref = new FooterPreference(getPrefContext());
            descPref.setOrder(ORDER_LAST);
            descPref.setSelectable(false);
            descPref.setTitle(mChannelGroup.getDescription());
            getPreferenceScreen().addPreference(descPref);
            mChannels.add(descPref);
        }
    }

    private void setupBlock() {
        View switchBarContainer = LayoutInflater.from(
                getPrefContext()).inflate(R.layout.styled_switch_bar, null);
        mSwitchBar = switchBarContainer.findViewById(R.id.switch_bar);
        mSwitchBar.show();
        mSwitchBar.setDisabledByAdmin(mSuspendedAppsAdmin);
        mSwitchBar.setChecked(!mChannelGroup.isBlocked());
        mSwitchBar.addOnSwitchChangeListener((switchView, isChecked) -> {
            mChannelGroup.setBlocked(!isChecked);
            mBackend.updateChannelGroup(mPkg, mUid, mChannelGroup);
            updateDependents(!isChecked);
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(ORDER_FIRST);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (!isChannelGroupBlockable(mChannelGroup)) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.channel_group_notifications_off_desc);
    }

    protected void updateDependents(boolean banned) {
        for (Preference channel : mChannels) {
            setVisible(channel, !banned);
        }
        if (mAppLink != null) {
            setVisible(mAppLink, !banned);
        }
        setVisible(mBlockBar, isChannelGroupBlockable(mChannelGroup));
        setVisible(mBlockedDesc, mAppRow.banned || mChannelGroup.isBlocked());
    }
}
