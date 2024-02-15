/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.notification

import android.Manifest
import android.annotation.IntRange
import android.app.INotificationManager
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_NONE
import android.app.NotificationManager.IMPORTANCE_UNSPECIFIED
import android.app.usage.IUsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.IUserManager
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.android.settings.R
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.userId
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * This contains how often an app sends notifications and how recently it sent one.
 */
data class NotificationSentState(
    @IntRange(from = 0)
    var lastSent: Long = 0,

    @IntRange(from = 0)
    var sentCount: Int = 0,
)

interface IAppNotificationRepository {
    /** Gets the notification summary for the given application. */
    fun getNotificationSummary(app: ApplicationInfo): String
}

class AppNotificationRepository(
    private val context: Context,
    private val packageManagers: IPackageManagers = PackageManagers,
    private val usageStatsManager: IUsageStatsManager = IUsageStatsManager.Stub.asInterface(
        ServiceManager.getService(Context.USAGE_STATS_SERVICE)
    ),
    private val notificationManager: INotificationManager = INotificationManager.Stub.asInterface(
        ServiceManager.getService(Context.NOTIFICATION_SERVICE)
    ),
    private val userManager: IUserManager = IUserManager.Stub.asInterface(
            ServiceManager.getService(Context.USER_SERVICE)
    ),
) : IAppNotificationRepository {
    fun getAggregatedUsageEvents(userIdFlow: Flow<Int>): Flow<Map<String, NotificationSentState>> =
        userIdFlow.map { userId ->
            val aggregatedStats = mutableMapOf<String, NotificationSentState>()
            queryEventsForUser(userId).forEachNotificationEvent { event ->
                aggregatedStats.getOrPut(event.packageName, ::NotificationSentState).apply {
                    lastSent = max(lastSent, event.timeStamp)
                    sentCount++
                }
            }
            aggregatedStats
        }

    private fun queryEventsForUser(userId: Int): UsageEvents? {
        val now = System.currentTimeMillis()
        val startTime = now - TimeUnit.DAYS.toMillis(DAYS_TO_CHECK)
        return try {
            usageStatsManager.queryEventsForUser(startTime, now, userId, context.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed IUsageStatsManager.queryEventsForUser(): ", e)
            null
        }
    }

    fun isEnabled(app: ApplicationInfo): Boolean =
        notificationManager.areNotificationsEnabledForPackage(app.packageName, app.uid)

    fun isChangeable(app: ApplicationInfo): Boolean {
        if (notificationManager.isImportanceLocked(app.packageName, app.uid)) {
            return false
        }

        // If the app targets T but has not requested the permission, we cannot change the
        // permission state.
        return app.targetSdkVersion < Build.VERSION_CODES.TIRAMISU ||
            with(packageManagers) {
                app.hasRequestPermission(Manifest.permission.POST_NOTIFICATIONS)
            }
    }

    fun setEnabled(app: ApplicationInfo, enabled: Boolean): Boolean {
        if (onlyHasDefaultChannel(app)) {
            getChannel(app, NotificationChannel.DEFAULT_CHANNEL_ID)?.let { channel ->
                channel.importance = if (enabled) IMPORTANCE_UNSPECIFIED else IMPORTANCE_NONE
                updateChannel(app, channel)
            }
        }
        return try {
            notificationManager.setNotificationsEnabledForPackage(app.packageName, app.uid, enabled)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error calling INotificationManager", e)
            false
        }
    }

    fun isUserUnlocked(user: Int): Boolean {
        return try {
            userManager.isUserUnlocked(user)
        } catch (e: Exception) {
            Log.w(TAG, "Error calling UserManager", e)
            false
        }
    }

    override fun getNotificationSummary(app: ApplicationInfo): String {
        if (!isEnabled(app)) return context.getString(R.string.notifications_disabled)
        val channelCount = getChannelCount(app)
        if (channelCount == 0) {
            return calculateFrequencySummary(getSentCount(app))
        }
        val blockedChannelCount = getBlockedChannelCount(app)
        if (channelCount == blockedChannelCount) {
            return context.getString(R.string.notifications_disabled)
        }
        val frequencySummary = calculateFrequencySummary(getSentCount(app))
        if (blockedChannelCount == 0) return frequencySummary
        return context.getString(
            R.string.notifications_enabled_with_info,
            frequencySummary,
            context.formatString(
                R.string.notifications_categories_off, "count" to blockedChannelCount
            )
        )
    }

    private fun getSentCount(app: ApplicationInfo): Int {
        var sentCount = 0
        queryEventsForPackageForUser(app).forEachNotificationEvent { sentCount++ }
        return sentCount
    }

    private fun queryEventsForPackageForUser(app: ApplicationInfo): UsageEvents? {
        val now = System.currentTimeMillis()
        val startTime = now - TimeUnit.DAYS.toMillis(DAYS_TO_CHECK)
        return try {
            usageStatsManager.queryEventsForPackageForUser(
                startTime, now, app.userId, app.packageName, context.packageName
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed IUsageStatsManager.queryEventsForPackageForUser(): ", e)
            null
        }
    }

    private fun getChannelCount(app: ApplicationInfo): Int = try {
        notificationManager.getNumNotificationChannelsForPackage(app.packageName, app.uid, false)
    } catch (e: Exception) {
        Log.w(TAG, "Error calling INotificationManager", e)
        0
    }

    private fun getBlockedChannelCount(app: ApplicationInfo): Int = try {
        notificationManager.getBlockedChannelCount(app.packageName, app.uid)
    } catch (e: Exception) {
        Log.w(TAG, "Error calling INotificationManager", e)
        0
    }

    fun calculateFrequencySummary(sentCount: Int): String {
        val dailyFrequency = (sentCount.toFloat() / DAYS_TO_CHECK).roundToInt()
        return if (dailyFrequency > 0) {
            context.formatString(
                R.string.notifications_sent_daily,
                "count" to dailyFrequency,
            )
        } else {
            context.formatString(
                R.string.notifications_sent_weekly,
                "count" to sentCount,
            )
        }
    }

    private fun updateChannel(app: ApplicationInfo, channel: NotificationChannel) {
        notificationManager.updateNotificationChannelForPackage(app.packageName, app.uid, channel)
    }

    private fun onlyHasDefaultChannel(app: ApplicationInfo): Boolean =
        notificationManager.onlyHasDefaultChannel(app.packageName, app.uid)

    private fun getChannel(app: ApplicationInfo, channelId: String): NotificationChannel? =
        notificationManager.getNotificationChannelForPackage(
            app.packageName, app.uid, channelId, null, true
        )

    companion object {
        private const val TAG = "AppNotificationsRepo"

        private const val DAYS_TO_CHECK = 7L

        private fun UsageEvents?.forEachNotificationEvent(action: (UsageEvents.Event) -> Unit) {
            this ?: return
            val event = UsageEvents.Event()
            while (getNextEvent(event)) {
                if (event.eventType == UsageEvents.Event.NOTIFICATION_INTERRUPTION) {
                    action(event)
                }
            }
        }
    }
}
