/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static com.android.server.notification.Flags.notificationHideUnusedChannels;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelListPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "channels";
    private static final String KEY_GENERAL_CATEGORY = "categories";
    private static final String KEY_ZERO_CATEGORIES = "zeroCategories";
    public static final String ARG_FROM_SETTINGS = "fromSettings";

    private List<NotificationChannelGroup> mChannelGroupList;
    private PreferenceCategory mPreference;

    private boolean mShowAll;

    public ChannelListPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned) {
            return false;
        }
        if (mChannel != null) {
            if (mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)
                    || NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    boolean isIncludedInFilter() {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (PreferenceCategory) preference;
        // Load channel settings
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                if (notificationHideUnusedChannels()) {
                    if (mShowAll) {
                        mChannelGroupList = mBackend.getGroups(mAppRow.pkg, mAppRow.uid).getList();
                    } else {
                        mChannelGroupList = mBackend.getGroupsWithRecentBlockedFilter(mAppRow.pkg,
                                mAppRow.uid).getList();
                    }
                } else {
                    mChannelGroupList = mBackend.getGroups(mAppRow.pkg, mAppRow.uid).getList();
                }
                Collections.sort(mChannelGroupList, CHANNEL_GROUP_COMPARATOR);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                updateFullList(mPreference, mChannelGroupList);
            }
        }.execute();
    }

    protected void setShowAll(boolean showAll) {
        mShowAll = showAll;
    }

    /**
     * Update the preferences group to match the
     * @param groupPrefsList
     * @param channelGroups
     */
    void updateFullList(@NonNull PreferenceCategory groupPrefsList,
                @NonNull List<NotificationChannelGroup> channelGroups) {
        if (channelGroups.isEmpty()) {
            if (groupPrefsList.getPreferenceCount() == 1
                    && KEY_ZERO_CATEGORIES.equals(groupPrefsList.getPreference(0).getKey())) {
                // Ensure the titles are correct for the current language, but otherwise leave alone
                PreferenceGroup groupCategory = (PreferenceGroup) groupPrefsList.getPreference(0);
                groupCategory.setTitle(R.string.notification_channels);
                groupCategory.getPreference(0).setTitle(R.string.no_channels);
            } else {
                // Clear any contents and create the 'zero-categories' group.
                groupPrefsList.removeAll();

                PreferenceCategory groupCategory = new PreferenceCategory(mContext);
                groupCategory.setTitle(R.string.notification_channels);
                groupCategory.setKey(KEY_ZERO_CATEGORIES);
                groupPrefsList.addPreference(groupCategory);

                Preference empty = new Preference(mContext);
                empty.setTitle(R.string.no_channels);
                empty.setEnabled(false);
                groupCategory.addPreference(empty);
            }
        } else {
            updateGroupList(groupPrefsList, channelGroups);
        }
    }

    /**
     * Looks for the category for the given group's key at the expected index, if that doesn't
     * match, it checks all groups, and if it can't find that group anywhere, it creates it.
     */
    @NonNull
    private PreferenceCategory findOrCreateGroupCategoryForKey(
            @NonNull PreferenceCategory groupPrefsList, @Nullable String key, int expectedIndex) {
        if (key == null) {
            key = KEY_GENERAL_CATEGORY;
        }
        int preferenceCount = groupPrefsList.getPreferenceCount();
        if (expectedIndex < preferenceCount) {
            Preference preference = groupPrefsList.getPreference(expectedIndex);
            if (key.equals(preference.getKey())) {
                return (PreferenceCategory) preference;
            }
        }
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = groupPrefsList.getPreference(i);
            if (key.equals(preference.getKey())) {
                preference.setOrder(expectedIndex);
                return (PreferenceCategory) preference;
            }
        }
        PreferenceCategory groupCategory = new PreferenceCategory(mContext);
        groupCategory.setOrder(expectedIndex);
        groupCategory.setKey(key);
        groupPrefsList.addPreference(groupCategory);
        return groupCategory;
    }

    private void updateGroupList(@NonNull PreferenceCategory groupPrefsList,
            @NonNull List<NotificationChannelGroup> channelGroups) {
        // Update the list, but optimize for the most common case where the list hasn't changed.
        int numFinalGroups = channelGroups.size();
        int initialPrefCount = groupPrefsList.getPreferenceCount();
        List<PreferenceCategory> finalOrderedGroups = new ArrayList<>(numFinalGroups);
        for (int i = 0; i < numFinalGroups; i++) {
            NotificationChannelGroup group = channelGroups.get(i);
            PreferenceCategory groupCategory =
                    findOrCreateGroupCategoryForKey(groupPrefsList, group.getId(), i);
            finalOrderedGroups.add(groupCategory);
            updateGroupPreferences(group, groupCategory);
        }
        int postAddPrefCount = groupPrefsList.getPreferenceCount();
        // If any groups were inserted (into a non-empty list) or need to be removed, we need to
        // remove all groups and re-add them all.
        // This is required to ensure proper ordering of inserted groups, and it simplifies logic
        // at the cost of computation in the rare case that the list is changing.
        boolean hasInsertions = initialPrefCount != 0 && initialPrefCount != numFinalGroups;
        boolean requiresRemoval = postAddPrefCount != numFinalGroups;
        if (hasInsertions || requiresRemoval) {
            groupPrefsList.removeAll();
            for (PreferenceCategory group : finalOrderedGroups) {
                groupPrefsList.addPreference(group);
            }
        }
    }

    /**
     * Looks for the channel preference for the given channel's key at the expected index, if that
     * doesn't match, it checks all rows, and if it can't find that channel anywhere, it creates
     * the preference.
     */
    @NonNull
    private PrimarySwitchPreference findOrCreateChannelPrefForKey(
            @NonNull PreferenceGroup groupPrefGroup, @NonNull String key, int expectedIndex) {
        int preferenceCount = groupPrefGroup.getPreferenceCount();
        if (expectedIndex < preferenceCount) {
            Preference preference = groupPrefGroup.getPreference(expectedIndex);
            if (key.equals(preference.getKey())) {
                return (PrimarySwitchPreference) preference;
            }
        }
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = groupPrefGroup.getPreference(i);
            if (key.equals(preference.getKey())) {
                preference.setOrder(expectedIndex);
                return (PrimarySwitchPreference) preference;
            }
        }
        PrimarySwitchPreference channelPref = new PrimarySwitchPreference(mContext);
        channelPref.setOrder(expectedIndex);
        channelPref.setKey(key);
        groupPrefGroup.addPreference(channelPref);
        return channelPref;
    }

    private void updateGroupPreferences(@NonNull NotificationChannelGroup group,
            @NonNull PreferenceGroup groupPrefGroup) {
        int initialPrefCount = groupPrefGroup.getPreferenceCount();
        List<Preference> finalOrderedPrefs = new ArrayList<>();
        Preference appDefinedGroupToggle;
        if (group.getId() == null) {
            // For the 'null' group, set the "Other" title.
            groupPrefGroup.setTitle(R.string.notification_channels_other);
            appDefinedGroupToggle = null;
        } else {
            // For an app-defined group, set their name and create a row to toggle 'isBlocked'.
            groupPrefGroup.setTitle(group.getName());
            appDefinedGroupToggle = addOrUpdateGroupToggle(groupPrefGroup, group);
            finalOrderedPrefs.add(appDefinedGroupToggle);
        }
        // Here "empty" means having no channel rows; the group toggle is ignored for this purpose.
        boolean initiallyEmpty = groupPrefGroup.getPreferenceCount() == finalOrderedPrefs.size();

        // For each channel, add or update the preference object.
        final List<NotificationChannel> channels =
                group.isBlocked() ? Collections.emptyList() : group.getChannels();
        Collections.sort(channels, CHANNEL_COMPARATOR);
        for (NotificationChannel channel : channels) {
            if (!TextUtils.isEmpty(channel.getConversationId()) && !channel.isDemoted()) {
                // conversations get their own section
                continue;
            }
            // Get or create the row, and populate its current state.
            PrimarySwitchPreference channelPref = findOrCreateChannelPrefForKey(groupPrefGroup,
                    channel.getId(), /* expectedIndex */ finalOrderedPrefs.size());
            updateSingleChannelPrefs(channelPref, channel, group.isBlocked());
            finalOrderedPrefs.add(channelPref);
        }
        int postAddPrefCount = groupPrefGroup.getPreferenceCount();

        // If any channels were inserted (into a non-empty list) or need to be removed, we need to
        // remove all preferences and re-add them all.
        // This is required to ensure proper ordering of inserted channels, and it simplifies logic
        // at the cost of computation in the rare case that the list is changing.
        // As an optimization, keep the app-defined-group toggle. That way it doesn't "flicker"
        // (due to remove+add) when toggling the group.
        int numFinalGroups = finalOrderedPrefs.size();
        boolean hasInsertions = !initiallyEmpty && initialPrefCount != numFinalGroups;
        boolean requiresRemoval = postAddPrefCount != numFinalGroups;
        boolean keepGroupToggle =
                appDefinedGroupToggle != null && groupPrefGroup.getPreferenceCount() > 0
                        && groupPrefGroup.getPreference(0) == appDefinedGroupToggle
                        && finalOrderedPrefs.get(0) == appDefinedGroupToggle;
        if (hasInsertions || requiresRemoval) {
            if (keepGroupToggle) {
                while (groupPrefGroup.getPreferenceCount() > 1) {
                    groupPrefGroup.removePreference(groupPrefGroup.getPreference(1));
                }
            } else {
                groupPrefGroup.removeAll();
            }
            for (int i = (keepGroupToggle ? 1 : 0); i < finalOrderedPrefs.size(); i++) {
                groupPrefGroup.addPreference(finalOrderedPrefs.get(i));
            }
        }
    }

    /** Add or find and update the toggle for disabling the entire notification channel group. */
    private Preference addOrUpdateGroupToggle(@NonNull final PreferenceGroup parent,
            @NonNull final NotificationChannelGroup group) {
        boolean shouldAdd = false;
        final RestrictedSwitchPreference preference;
        if (parent.getPreferenceCount() > 0
                && parent.getPreference(0) instanceof RestrictedSwitchPreference) {
            preference = (RestrictedSwitchPreference) parent.getPreference(0);
        } else {
            shouldAdd = true;
            preference = new RestrictedSwitchPreference(mContext);
        }
        preference.setOrder(-1);
        preference.setTitle(mContext.getString(
                R.string.notification_switch_label, group.getName()));
        preference.setEnabled(mAdmin == null
                && isChannelGroupBlockable(group));
        preference.setChecked(!group.isBlocked());
        preference.setOnPreferenceClickListener(preference1 -> {
            final boolean allowGroup = ((TwoStatePreference) preference1).isChecked();
            group.setBlocked(!allowGroup);
            mBackend.updateChannelGroup(mAppRow.pkg, mAppRow.uid, group);

            onGroupBlockStateChanged(group);
            return true;
        });
        if (shouldAdd) {
            parent.addPreference(preference);
        }
        return preference;
    }

    /** Update the properties of the channel preference with the values from the channel object. */
    private void updateSingleChannelPrefs(@NonNull final PrimarySwitchPreference channelPref,
            @NonNull final NotificationChannel channel,
            final boolean groupBlocked) {
        channelPref.setSwitchEnabled(mAdmin == null
                && isChannelBlockable(channel)
                && isChannelConfigurable(channel)
                && !groupBlocked);
        if (channel.getImportance() > IMPORTANCE_LOW) {
            channelPref.setIcon(getAlertingIcon());
        } else {
            channelPref.setIcon(mContext.getDrawable(R.drawable.empty_icon));
        }
        channelPref.setIconSize(PrimarySwitchPreference.ICON_SIZE_SMALL);
        channelPref.setTitle(channel.getName());
        channelPref.setSummary(NotificationBackend.getSentSummary(
                mContext, mAppRow.sentByChannel.get(channel.getId()), false));
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        channelArgs.putBoolean(ARG_FROM_SETTINGS, true);
        channelPref.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setTitleRes(R.string.notification_channel_title)
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_APP_NOTIFICATION)
                .toIntent());

        channelPref.setOnPreferenceChangeListener(
                (preference, o) -> {
                    boolean value = (Boolean) o;
                    int importance = value
                            ? Math.max(channel.getOriginalImportance(), IMPORTANCE_LOW)
                            : IMPORTANCE_NONE;
                    channel.setImportance(importance);
                    channel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    PrimarySwitchPreference channelPref1 = (PrimarySwitchPreference) preference;
                    channelPref1.setIcon(R.drawable.empty_icon);
                    if (channel.getImportance() > IMPORTANCE_LOW) {
                        channelPref1.setIcon(getAlertingIcon());
                    }
                    mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, channel);

                    return true;
                });
    }

    private Drawable getAlertingIcon() {
        Drawable icon = mContext.getDrawable(R.drawable.ic_notifications_alert);
        icon.setTintList(Utils.getColorAccent(mContext));
        return icon;
    }

    protected void onGroupBlockStateChanged(NotificationChannelGroup group) {
        if (group == null) {
            return;
        }
        PreferenceGroup groupPrefGroup = mPreference.findPreference(group.getId());
        if (groupPrefGroup != null) {
            updateGroupPreferences(group, groupPrefGroup);
        }
    }
}
