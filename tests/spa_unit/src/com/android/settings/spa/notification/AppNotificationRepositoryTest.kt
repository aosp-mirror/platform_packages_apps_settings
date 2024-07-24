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
import android.app.INotificationManager
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_NONE
import android.app.NotificationManager.IMPORTANCE_UNSPECIFIED
import android.app.usage.IUsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.userId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppNotificationRepositoryTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManagers: IPackageManagers

    @Mock
    private lateinit var usageStatsManager: IUsageStatsManager

    @Mock
    private lateinit var notificationManager: INotificationManager

    private lateinit var repository: AppNotificationRepository

    @Before
    fun setUp() {
        repository = AppNotificationRepository(
            context,
            packageManagers,
            usageStatsManager,
            notificationManager,
        )
    }

    private fun mockOnlyHasDefaultChannel(): NotificationChannel {
        whenever(notificationManager.onlyHasDefaultChannel(APP.packageName, APP.uid))
            .thenReturn(true)
        val channel =
            NotificationChannel(NotificationChannel.DEFAULT_CHANNEL_ID, null, IMPORTANCE_DEFAULT)
        whenever(
            notificationManager.getNotificationChannelForPackage(
                APP.packageName, APP.uid, NotificationChannel.DEFAULT_CHANNEL_ID, null, true
            )
        ).thenReturn(channel)
        return channel
    }

    private fun mockIsEnabled(app: ApplicationInfo, enabled: Boolean) {
        whenever(notificationManager.areNotificationsEnabledForPackage(app.packageName, app.uid))
            .thenReturn(enabled)
    }

    private fun mockChannelCount(app: ApplicationInfo, count: Int) {
        whenever(
            notificationManager.getNumNotificationChannelsForPackage(
                app.packageName,
                app.uid,
                false,
            )
        ).thenReturn(count)
    }

    private fun mockBlockedChannelCount(app: ApplicationInfo, count: Int) {
        whenever(notificationManager.getBlockedChannelCount(app.packageName, app.uid))
            .thenReturn(count)
    }

    private fun mockSentCount(app: ApplicationInfo, sentCount: Int) {
        val events = (1..sentCount).map {
            UsageEvents.Event().apply {
                mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION
            }
        }
        whenever(
            usageStatsManager.queryEventsForPackageForUser(
                any(), any(), eq(app.userId), eq(app.packageName), any()
            )
        ).thenReturn(UsageEvents(events, arrayOf()))
    }

    @Test
    fun getAggregatedUsageEvents() = runTest {
        val events = listOf(
            UsageEvents.Event().apply {
                mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION
                mPackage = PACKAGE_NAME
                mTimeStamp = 2
            },
            UsageEvents.Event().apply {
                mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION
                mPackage = PACKAGE_NAME
                mTimeStamp = 3
            },
            UsageEvents.Event().apply {
                mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION
                mPackage = PACKAGE_NAME
                mTimeStamp = 6
            },
        )
        whenever(usageStatsManager.queryEventsForUser(any(), any(), eq(USER_ID), any()))
            .thenReturn(UsageEvents(events, arrayOf()))

        val usageEvents = repository.getAggregatedUsageEvents(flowOf(USER_ID)).first()

        assertThat(usageEvents).containsExactly(
            PACKAGE_NAME, NotificationSentState(lastSent = 6, sentCount = 3),
        )
    }

    @Test
    fun isEnabled() {
        mockIsEnabled(app = APP, enabled = true)

        val isEnabled = repository.isEnabled(APP)

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun isChangeable_importanceLocked() {
        whenever(notificationManager.isImportanceLocked(APP.packageName, APP.uid)).thenReturn(true)

        val isChangeable = repository.isChangeable(APP)

        assertThat(isChangeable).isFalse()
    }

    @Test
    fun isChangeable_appTargetS() {
        val targetSApp = ApplicationInfo().apply {
            targetSdkVersion = Build.VERSION_CODES.S
        }

        val isChangeable = repository.isChangeable(targetSApp)

        assertThat(isChangeable).isTrue()
    }

    @Test
    fun isChangeable_appTargetTiramisuWithoutNotificationPermission() {
        val targetTiramisuApp = ApplicationInfo().apply {
            targetSdkVersion = Build.VERSION_CODES.TIRAMISU
        }
        with(packageManagers) {
            whenever(targetTiramisuApp.hasRequestPermission(Manifest.permission.POST_NOTIFICATIONS))
                .thenReturn(false)
        }

        val isChangeable = repository.isChangeable(targetTiramisuApp)

        assertThat(isChangeable).isFalse()
    }

    @Test
    fun isChangeable_appTargetTiramisuWithNotificationPermission() {
        val targetTiramisuApp = ApplicationInfo().apply {
            targetSdkVersion = Build.VERSION_CODES.TIRAMISU
        }
        with(packageManagers) {
            whenever(targetTiramisuApp.hasRequestPermission(Manifest.permission.POST_NOTIFICATIONS))
                .thenReturn(true)
        }

        val isChangeable = repository.isChangeable(targetTiramisuApp)

        assertThat(isChangeable).isTrue()
    }

    @Test
    fun setEnabled_toTrueWhenOnlyHasDefaultChannel() {
        val channel = mockOnlyHasDefaultChannel()

        repository.setEnabled(app = APP, enabled = true)

        verify(notificationManager)
            .updateNotificationChannelForPackage(APP.packageName, APP.uid, channel)
        assertThat(channel.importance).isEqualTo(IMPORTANCE_UNSPECIFIED)
    }

    @Test
    fun setEnabled_toFalseWhenOnlyHasDefaultChannel() {
        val channel = mockOnlyHasDefaultChannel()

        repository.setEnabled(app = APP, enabled = false)

        verify(notificationManager)
            .updateNotificationChannelForPackage(APP.packageName, APP.uid, channel)
        assertThat(channel.importance).isEqualTo(IMPORTANCE_NONE)
    }

    @Test
    fun setEnabled_toTrueWhenNotOnlyHasDefaultChannel() {
        whenever(notificationManager.onlyHasDefaultChannel(APP.packageName, APP.uid))
            .thenReturn(false)

        repository.setEnabled(app = APP, enabled = true)

        verify(notificationManager)
            .setNotificationsEnabledForPackage(APP.packageName, APP.uid, true)
    }

    @Test
    fun getNotificationSummary_notEnabled() {
        mockIsEnabled(app = APP, enabled = false)

        val summary = repository.getNotificationSummary(APP)

        assertThat(summary).isEqualTo(context.getString(R.string.notifications_disabled))
    }

    @Test
    fun getNotificationSummary_noChannel() {
        mockIsEnabled(app = APP, enabled = true)
        mockChannelCount(app = APP, count = 0)
        mockSentCount(app = APP, sentCount = 1)

        val summary = repository.getNotificationSummary(APP)

        assertThat(summary).isEqualTo("About 1 notification per week")
    }

    @Test
    fun getNotificationSummary_allChannelsBlocked() {
        mockIsEnabled(app = APP, enabled = true)
        mockChannelCount(app = APP, count = 2)
        mockBlockedChannelCount(app = APP, count = 2)

        val summary = repository.getNotificationSummary(APP)

        assertThat(summary).isEqualTo(context.getString(R.string.notifications_disabled))
    }

    @Test
    fun getNotificationSummary_noChannelBlocked() {
        mockIsEnabled(app = APP, enabled = true)
        mockChannelCount(app = APP, count = 2)
        mockSentCount(app = APP, sentCount = 2)
        mockBlockedChannelCount(app = APP, count = 0)

        val summary = repository.getNotificationSummary(APP)

        assertThat(summary).isEqualTo("About 2 notifications per week")
    }

    @Test
    fun getNotificationSummary_someChannelsBlocked() {
        mockIsEnabled(app = APP, enabled = true)
        mockChannelCount(app = APP, count = 2)
        mockSentCount(app = APP, sentCount = 3)
        mockBlockedChannelCount(app = APP, count = 1)

        val summary = repository.getNotificationSummary(APP)

        assertThat(summary).isEqualTo("About 3 notifications per week / 1 category turned off")
    }

    @Test
    fun calculateFrequencySummary_daily() {
        val summary = repository.calculateFrequencySummary(4)

        assertThat(summary).isEqualTo("About 1 notification per day")
    }

    @Test
    fun calculateFrequencySummary_weekly() {
        val summary = repository.calculateFrequencySummary(3)

        assertThat(summary).isEqualTo("About 3 notifications per week")
    }

    private companion object {
        const val USER_ID = 0
        const val PACKAGE_NAME = "package.name"
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = 123
        }
    }
}