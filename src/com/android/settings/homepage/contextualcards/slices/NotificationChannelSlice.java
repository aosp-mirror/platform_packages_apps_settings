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

package com.android.settings.homepage.contextualcards.slices;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.notification.NotificationSettingsBase.ARG_FROM_SETTINGS;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ChannelNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.NotificationsSentState;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NotificationChannelSlice implements CustomSliceable {

    /**
     * Recently app condition:
     * App was installed between 3 and 7 days ago.
     */
    @VisibleForTesting
    static final long DURATION_START_DAYS = TimeUnit.DAYS.toMillis(7);
    @VisibleForTesting
    static final long DURATION_END_DAYS = TimeUnit.DAYS.toMillis(3);

    /**
     * Notification count condition:
     * App has sent at least ~10 notifications.
     */
    @VisibleForTesting
    static final int MIN_NOTIFICATION_SENT_COUNT = 10;

    /**
     * Limit rows when the number of notification channel is more than {@link
     * #DEFAULT_EXPANDED_ROW_COUNT}.
     */
    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 3;

    private static final String TAG = "NotifChannelSlice";
    private static final String PACKAGE_NAME = "package_name";
    private static final String PACKAGE_UID = "package_uid";
    private static final String CHANNEL_ID = "channel_id";

    /**
     * Sort notification channel with weekly average sent count by descending.
     *
     * Note:
     * When the sent count of notification channels is the same, follow the sorting mechanism from
     * {@link com.android.settings.notification.NotificationSettingsBase#mChannelComparator}.
     * Since slice view only shows displayable notification channels, so those deleted ones are
     * excluded from the comparison here.
     */
    private static final Comparator<NotificationChannelState> CHANNEL_STATE_COMPARATOR =
            (left, right) -> {
                final NotificationsSentState leftState = left.getNotificationsSentState();
                final NotificationsSentState rightState = right.getNotificationsSentState();
                if (rightState.avgSentWeekly != leftState.avgSentWeekly) {
                    return rightState.avgSentWeekly - leftState.avgSentWeekly;
                }

                final NotificationChannel leftChannel = left.getNotificationChannel();
                final NotificationChannel rightChannel = right.getNotificationChannel();
                if (TextUtils.equals(leftChannel.getId(), NotificationChannel.DEFAULT_CHANNEL_ID)) {
                    return 1;
                } else if (TextUtils.equals(rightChannel.getId(),
                        NotificationChannel.DEFAULT_CHANNEL_ID)) {
                    return -1;
                }

                return leftChannel.getId().compareTo(rightChannel.getId());
            };

    private final Context mContext;
    @VisibleForTesting
    NotificationBackend mNotificationBackend;
    private String mPackageName;
    private int mUid;

    public NotificationChannelSlice(Context context) {
        mContext = context;
        mNotificationBackend = new NotificationBackend();
    }

    @Override
    public Slice getSlice() {
        final ListBuilder listBuilder =
                new ListBuilder(mContext, getUri(), ListBuilder.INFINITY).setAccentColor(-1);
        /**
         * Get package which is satisfied with:
         * 1. Recently installed.
         * 2. Multiple channels.
         * 3. Sent at least ~10 notifications.
         */
        // TODO(b/123065955): Review latency of NotificationChannelSlice
        final List<PackageInfo> multiChannelPackages = getMultiChannelPackages(
                getRecentlyInstalledPackages());
        final PackageInfo packageInfo = getMaxSentNotificationsPackage(multiChannelPackages);

        // Return a header with IsError flag, if package is not found.
        if (packageInfo == null) {
            return listBuilder.setHeader(getNoSuggestedAppHeader())
                    .setIsError(true).build();
        }

        // Save eligible package name and its uid, they will be used in getIntent().
        mPackageName = packageInfo.packageName;
        mUid = getApplicationUid(mPackageName);

        // Add notification channel header.
        final IconCompat icon = getApplicationIcon(mPackageName);
        final CharSequence title = mContext.getString(R.string.manage_app_notification,
                Utils.getApplicationLabel(mContext, mPackageName));
        listBuilder.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(getSubTitle(mPackageName, mUid))
                .setPrimaryAction(getPrimarySliceAction(icon, title, getIntent())));

        // Get rows by notification channel.
        final List<ListBuilder.RowBuilder> rows = getNotificationChannelRows(packageInfo, icon);

        // Get displayable notification channel count.
        final int channelCount = Math.min(rows.size(), DEFAULT_EXPANDED_ROW_COUNT);

        // According to the displayable channel count to add rows.
        for (int i = 0; i < channelCount; i++) {
            listBuilder.addRow(rows.get(i));
        }

        return listBuilder.build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.NOTIFICATION_CHANNEL_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
        final String packageName = intent.getStringExtra(PACKAGE_NAME);
        final int uid = intent.getIntExtra(PACKAGE_UID, -1);
        final String channelId = intent.getStringExtra(CHANNEL_ID);
        final PackageInfo packageInfo = getPackageInfo(packageName);
        final NotificationBackend.AppRow appRow = mNotificationBackend.loadAppRow(mContext,
                mContext.getPackageManager(), packageInfo);

        final List<NotificationChannel> notificationChannels = getEnabledChannels(packageName, uid,
                appRow);
        for (NotificationChannel channel : notificationChannels) {
            if (TextUtils.equals(channel.getId(), channelId)) {
                final int importance = newState ? IMPORTANCE_LOW : IMPORTANCE_NONE;
                channel.setImportance(importance);
                channel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                mNotificationBackend.updateChannel(packageName, uid, channel);
                return;
            }
        }
    }

    @Override
    public Intent getIntent() {
        final Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, mPackageName);
        args.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);

        return new SubSettingLauncher(mContext)
                .setDestination(AppNotificationSettings.class.getName())
                .setTitleRes(R.string.notifications_title)
                .setArguments(args)
                .setSourceMetricsCategory(MetricsProto.MetricsEvent.SLICE)
                .toIntent();
    }

    @VisibleForTesting
    IconCompat getApplicationIcon(String packageName) {
        final Drawable drawable;
        try {
            drawable = mContext.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "No such package to get application icon.");
            return null;
        }

        return IconCompat.createWithBitmap(Utils.drawableToBitmap(drawable));
    }

    @VisibleForTesting
    int getApplicationUid(String packageName) {
        final ApplicationsState.AppEntry appEntry =
                ApplicationsState.getInstance((Application) mContext.getApplicationContext())
                        .getEntry(packageName, UserHandle.myUserId());

        return appEntry.info.uid;
    }

    private SliceAction buildRowSliceAction(NotificationChannel channel, IconCompat icon) {
        final Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPackageName);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        channelArgs.putBoolean(ARG_FROM_SETTINGS, true);

        final Intent channelIntent = new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setTitleRes(R.string.notification_channel_title)
                .setSourceMetricsCategory(MetricsProto.MetricsEvent.SLICE)
                .toIntent();

        return SliceAction.createDeeplink(
                PendingIntent.getActivity(mContext, channel.hashCode(), channelIntent, 0), icon,
                ListBuilder.ICON_IMAGE, channel.getName());
    }

    private ListBuilder.HeaderBuilder getNoSuggestedAppHeader() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_homepage_apps);
        final CharSequence titleNoSuggestedApp = mContext.getString(R.string.no_suggested_app);
        final SliceAction primarySliceActionForNoSuggestedApp = getPrimarySliceAction(icon,
                titleNoSuggestedApp, getAppAndNotificationPageIntent());

        return new ListBuilder.HeaderBuilder()
                .setTitle(titleNoSuggestedApp)
                .setPrimaryAction(primarySliceActionForNoSuggestedApp);
    }

    private List<ListBuilder.RowBuilder> getNotificationChannelRows(PackageInfo packageInfo,
            IconCompat icon) {
        final List<ListBuilder.RowBuilder> notificationChannelRows = new ArrayList<>();
        final NotificationBackend.AppRow appRow = mNotificationBackend.loadAppRow(mContext,
                mContext.getPackageManager(), packageInfo);
        final List<NotificationChannel> enabledChannels = getEnabledChannels(mPackageName, mUid,
                appRow);

        for (NotificationChannel channel : enabledChannels) {
            notificationChannelRows.add(new ListBuilder.RowBuilder()
                    .setTitle(channel.getName())
                    .setSubtitle(NotificationBackend.getSentSummary(
                            mContext, appRow.sentByChannel.get(channel.getId()), false))
                    .setPrimaryAction(buildRowSliceAction(channel, icon))
                    .addEndItem(SliceAction.createToggle(getToggleIntent(channel.getId()),
                            null /* actionTitle */, channel.getImportance() != IMPORTANCE_NONE)));
        }

        return notificationChannelRows;
    }

    private PendingIntent getToggleIntent(String channelId) {
        // Send broadcast to enable/disable channel.
        final Intent intent = new Intent(getUri().toString())
                .setClass(mContext, SliceBroadcastReceiver.class)
                .putExtra(PACKAGE_NAME, mPackageName)
                .putExtra(PACKAGE_UID, mUid)
                .putExtra(CHANNEL_ID, channelId);

        return PendingIntent.getBroadcast(mContext, intent.hashCode(), intent, 0);
    }

    private List<PackageInfo> getMultiChannelPackages(List<PackageInfo> packageInfoList) {
        final List<PackageInfo> multiChannelPackages = new ArrayList<>();

        if (packageInfoList.isEmpty()) {
            return multiChannelPackages;
        }

        for (PackageInfo packageInfo : packageInfoList) {
            final int channelCount = mNotificationBackend.getChannelCount(packageInfo.packageName,
                    getApplicationUid(packageInfo.packageName));
            if (channelCount > 1) {
                multiChannelPackages.add(packageInfo);
            }
        }

        // TODO(b/119831690): Filter the packages which doesn't have any configurable channel.
        return multiChannelPackages;
    }

    private List<PackageInfo> getRecentlyInstalledPackages() {
        final long startTime = System.currentTimeMillis() - DURATION_START_DAYS;
        final long endTime = System.currentTimeMillis() - DURATION_END_DAYS;

        // Get recently installed packages between 3 and 7 days ago.
        final List<PackageInfo> recentlyInstalledPackages = new ArrayList<>();
        final List<PackageInfo> installedPackages =
                mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : installedPackages) {
            // Not include system app.
            if (packageInfo.applicationInfo.isSystemApp()) {
                continue;
            }

            if (packageInfo.firstInstallTime >= startTime
                    && packageInfo.firstInstallTime <= endTime) {
                recentlyInstalledPackages.add(packageInfo);
            }
        }

        return recentlyInstalledPackages;
    }

    private SliceAction getPrimarySliceAction(IconCompat icon, CharSequence title, Intent intent) {
        return SliceAction.createDeeplink(
                PendingIntent.getActivity(mContext, intent.hashCode(), intent, 0),
                icon,
                ListBuilder.ICON_IMAGE,
                title);
    }

    private List<NotificationChannel> getEnabledChannels(String packageName, int uid,
            NotificationBackend.AppRow appRow) {
        final List<NotificationChannelGroup> channelGroupList =
                mNotificationBackend.getGroups(packageName, uid).getList();
        final List<NotificationChannel> channels = channelGroupList.stream()
                .flatMap(group -> group.getChannels().stream().filter(
                        channel -> isChannelEnabled(group, channel, appRow)))
                .collect(Collectors.toList());

        // Pack the notification channel with notification sent state for sorting.
        final List<NotificationChannelState> channelStates = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            NotificationsSentState sentState = appRow.sentByChannel.get(channel.getId());
            if (sentState == null) {
                sentState = new NotificationsSentState();
            }
            channelStates.add(new NotificationChannelState(sentState, channel));
        }

        // Sort the notification channels with notification sent count by descending.
        return channelStates.stream().sorted(CHANNEL_STATE_COMPARATOR).map(
                state -> state.getNotificationChannel()).collect(Collectors.toList());
    }

    private PackageInfo getMaxSentNotificationsPackage(List<PackageInfo> packageInfoList) {
        if (packageInfoList.isEmpty()) {
            return null;
        }

        // Get the package which has sent at least ~10 notifications and not turn off channels.
        int maxSentCount = 0;
        PackageInfo maxSentCountPackage = null;
        for (PackageInfo packageInfo : packageInfoList) {
            final NotificationBackend.AppRow appRow = mNotificationBackend.loadAppRow(mContext,
                    mContext.getPackageManager(), packageInfo);
            // Get sent notification count from app.
            final int sentCount = appRow.sentByApp.sentCount;
            if (!appRow.banned && sentCount >= MIN_NOTIFICATION_SENT_COUNT
                    && sentCount > maxSentCount) {
                maxSentCount = sentCount;
                maxSentCountPackage = packageInfo;
            }
        }

        return maxSentCountPackage;
    }

    private CharSequence getSubTitle(String packageName, int uid) {
        final int channelCount = mNotificationBackend.getChannelCount(packageName, uid);

        return mContext.getResources().getQuantityString(
                R.plurals.notification_channel_count_summary,
                channelCount, channelCount);
    }

    private Intent getAppAndNotificationPageIntent() {
        final String screenTitle = mContext.getText(R.string.app_and_notification_dashboard_title)
                .toString();

        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                AppAndNotificationDashboardFragment.class.getName(), "" /* key */,
                screenTitle,
                MetricsProto.MetricsEvent.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(getUri());
    }

    private PackageInfo getPackageInfo(String packageName) {
        try {
            return mContext.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "No such package to get package info.");
            return null;
        }
    }

    private boolean isChannelEnabled(NotificationChannelGroup group, NotificationChannel channel,
            NotificationBackend.AppRow appRow) {
        final RestrictedLockUtils.EnforcedAdmin suspendedAppsAdmin =
                RestrictedLockUtilsInternal.checkIfApplicationIsSuspended(mContext, mPackageName,
                        mUid);

        return suspendedAppsAdmin == null
                && isChannelBlockable(channel, appRow)
                && isChannelConfigurable(channel, appRow)
                && !group.isBlocked();
    }

    private boolean isChannelConfigurable(NotificationChannel channel,
            NotificationBackend.AppRow appRow) {
        if (channel != null && appRow != null) {
            return !TextUtils.equals(channel.getId(), appRow.lockedChannelId);
        }

        return false;
    }

    private boolean isChannelBlockable(NotificationChannel channel,
            NotificationBackend.AppRow appRow) {
        if (channel != null && appRow != null) {
            if (!appRow.systemApp) {
                return true;
            }

            return channel.isBlockableSystem()
                    || channel.getImportance() == IMPORTANCE_NONE;
        }

        return false;
    }

    /**
     * This class is used to sort notification channels according to notification sent count and
     * notification id in {@link NotificationChannelSlice#CHANNEL_STATE_COMPARATOR}.
     *
     * Include {@link NotificationsSentState#avgSentWeekly} and {@link NotificationChannel#getId()}
     * to get the number of notifications being sent and notification id.
     */
    private static class NotificationChannelState {

        final private NotificationsSentState mNotificationsSentState;
        final private NotificationChannel mNotificationChannel;

        public NotificationChannelState(NotificationsSentState notificationsSentState,
                NotificationChannel notificationChannel) {
            mNotificationsSentState = notificationsSentState;
            mNotificationChannel = notificationChannel;
        }

        public NotificationChannel getNotificationChannel() {
            return mNotificationChannel;
        }

        public NotificationsSentState getNotificationsSentState() {
            return mNotificationsSentState;
        }
    }
}
