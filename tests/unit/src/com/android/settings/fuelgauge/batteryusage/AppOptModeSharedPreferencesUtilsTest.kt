/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import com.android.settings.fuelgauge.BatteryOptimizeUtils
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_OPTIMIZED
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_RESTRICTED
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNKNOWN
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED
import com.android.settings.fuelgauge.batteryusage.AppOptModeSharedPreferencesUtils.UNLIMITED_EXPIRE_TIME
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class AppOptModeSharedPreferencesUtilsTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Spy private var context: Context = ApplicationProvider.getApplicationContext()

    @Spy
    private var testBatteryOptimizeUtils = spy(BatteryOptimizeUtils(context, UID, PACKAGE_NAME))

    private lateinit var featureFactory: FakeFeatureFactory

    @Before
    fun setup() {
        AppOptModeSharedPreferencesUtils.clearAll(context)
        featureFactory = FakeFeatureFactory.setupForTest()
        whenever(featureFactory.powerUsageFeatureProvider.isForceExpireAppOptimizationModeEnabled)
            .thenReturn(false)
    }

    @After
    fun tearDown() {
        AppOptModeSharedPreferencesUtils.clearAll(context)
    }

    @Test
    fun getAllEvents_emptyData_verifyEmptyList() {
        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun clearAll_withData_verifyCleared() {
        insertAppOptModeEventForTest(expirationTime = 1000L)
        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).hasSize(1)

        AppOptModeSharedPreferencesUtils.clearAll(context)

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun updateAppOptModeExpirationInternal_withoutExpirationTime_verifyEmptyList() {
        insertAppOptModeEventForTest(expirationTime = UNLIMITED_EXPIRE_TIME)

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun updateAppOptModeExpirationInternal_setOptimizedModeWithFlagEnabled_verifyData() {
        whenever(featureFactory.powerUsageFeatureProvider.isRestrictedModeOverwriteEnabled)
            .thenReturn(true)
        insertAppOptModeEventForTest(expirationTime = 1000L, mode = MODE_OPTIMIZED)

        val events = AppOptModeSharedPreferencesUtils.getAllEvents(context)

        assertThat(events).hasSize(1)
        assertAppOptimizationModeEventInfo(
            events[0],
            UID,
            PACKAGE_NAME,
            MODE_OPTIMIZED,
            expirationTime = 1000L
        )
    }

    @Test
    fun updateAppOptModeExpirationInternal_setOptimizedModeWithFlagDisabled_verifyData() {
        whenever(featureFactory.powerUsageFeatureProvider.isRestrictedModeOverwriteEnabled)
            .thenReturn(false)
        insertAppOptModeEventForTest(expirationTime = 1000L, mode = MODE_OPTIMIZED)

        val events = AppOptModeSharedPreferencesUtils.getAllEvents(context)

        assertThat(events).hasSize(1)
        assertAppOptimizationModeEventInfo(
            events[0],
            UID,
            PACKAGE_NAME,
            MODE_OPTIMIZED,
            expirationTime = 1000L
        )
    }

    @Test
    fun updateAppOptModeExpirationInternal_setRestrictedModeWithFlagEnabled_verifyData() {
        whenever(featureFactory.powerUsageFeatureProvider.isRestrictedModeOverwriteEnabled)
            .thenReturn(true)
        insertAppOptModeEventForTest(expirationTime = 1000L, mode = MODE_RESTRICTED)

        val events = AppOptModeSharedPreferencesUtils.getAllEvents(context)

        assertThat(events).hasSize(1)
        assertAppOptimizationModeEventInfo(
            events[0],
            UID,
            PACKAGE_NAME,
            MODE_RESTRICTED,
            expirationTime = 1000L
        )
    }

    @Test
    fun updateAppOptModeExpirationInternal_setRestrictedModeWithFlagDisabled_verifyEmptyList() {
        whenever(featureFactory.powerUsageFeatureProvider.isRestrictedModeOverwriteEnabled)
            .thenReturn(false)
        insertAppOptModeEventForTest(expirationTime = 1000L, mode = MODE_RESTRICTED)

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun deleteAppOptimizationModeEventByUid_uidNotContained_verifyData() {
        insertAppOptModeEventForTest(expirationTime = 1000L)
        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).hasSize(1)

        AppOptModeSharedPreferencesUtils.deleteAppOptimizationModeEventByUid(context, UNSET_UID)
        val events = AppOptModeSharedPreferencesUtils.getAllEvents(context)

        assertThat(events).hasSize(1)
        assertAppOptimizationModeEventInfo(
            events[0],
            UID,
            PACKAGE_NAME,
            MODE_OPTIMIZED,
            expirationTime = 1000L
        )
    }

    @Test
    fun deleteAppOptimizationModeEventByUid_uidExisting_verifyData() {
        insertAppOptModeEventForTest(expirationTime = 1000L)

        AppOptModeSharedPreferencesUtils.deleteAppOptimizationModeEventByUid(context, UID)

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun resetExpiredAppOptModeBeforeTimestamp_forceExpiredData_verifyEmptyList() {
        whenever(featureFactory.powerUsageFeatureProvider.isForceExpireAppOptimizationModeEnabled)
            .thenReturn(true)
        insertAppOptModeEventForTest(expirationTime = 1000L)

        AppOptModeSharedPreferencesUtils.resetExpiredAppOptModeBeforeTimestamp(
            context,
            queryTimestampMs = 999L
        )

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun resetExpiredAppOptModeBeforeTimestamp_noExpiredData_verifyData() {
        insertAppOptModeEventForTest(expirationTime = 1000L)

        AppOptModeSharedPreferencesUtils.resetExpiredAppOptModeBeforeTimestamp(
            context,
            queryTimestampMs = 999L
        )
        val events = AppOptModeSharedPreferencesUtils.getAllEvents(context)

        assertThat(events).hasSize(1)
        assertAppOptimizationModeEventInfo(
            events[0],
            UID,
            PACKAGE_NAME,
            MODE_OPTIMIZED,
            expirationTime = 1000L
        )
    }

    @Test
    fun resetExpiredAppOptModeBeforeTimestamp_hasExpiredData_verifyEmptyList() {
        insertAppOptModeEventForTest(expirationTime = 1000L)

        AppOptModeSharedPreferencesUtils.resetExpiredAppOptModeBeforeTimestamp(
            context,
            queryTimestampMs = 1001L
        )

        assertThat(AppOptModeSharedPreferencesUtils.getAllEvents(context)).isEmpty()
    }

    @Test
    fun updateBatteryOptimizationMode_updateToOptimizedMode_verifyAction() {
        whenever(testBatteryOptimizeUtils?.isOptimizeModeMutable).thenReturn(true)
        whenever(testBatteryOptimizeUtils?.getAppOptimizationMode(true))
            .thenReturn(MODE_UNRESTRICTED)

        val currentOptMode =
            AppOptModeSharedPreferencesUtils.updateBatteryOptimizationMode(
                context,
                UID,
                PACKAGE_NAME,
                MODE_OPTIMIZED,
                Action.EXTERNAL_UPDATE,
                testBatteryOptimizeUtils
            )

        verify(testBatteryOptimizeUtils)?.setAppUsageState(MODE_OPTIMIZED, Action.EXTERNAL_UPDATE)
        assertThat(currentOptMode).isEqualTo(MODE_UNRESTRICTED)
    }

    @Test
    fun updateBatteryOptimizationMode_optimizationModeNotChanged_verifyAction() {
        whenever(testBatteryOptimizeUtils?.isOptimizeModeMutable).thenReturn(false)
        whenever(testBatteryOptimizeUtils?.getAppOptimizationMode(true))
            .thenReturn(MODE_UNRESTRICTED)

        val currentOptMode =
            AppOptModeSharedPreferencesUtils.updateBatteryOptimizationMode(
                context,
                UID,
                PACKAGE_NAME,
                MODE_OPTIMIZED,
                Action.EXTERNAL_UPDATE,
                testBatteryOptimizeUtils
            )

        verify(testBatteryOptimizeUtils, never())?.setAppUsageState(anyInt(), any())
        assertThat(currentOptMode).isEqualTo(MODE_UNKNOWN)
    }

    @Test
    fun updateBatteryOptimizationMode_updateToSameOptimizationMode_verifyAction() {
        whenever(testBatteryOptimizeUtils?.isOptimizeModeMutable).thenReturn(true)
        whenever(testBatteryOptimizeUtils?.getAppOptimizationMode(true)).thenReturn(MODE_RESTRICTED)

        val currentOptMode =
            AppOptModeSharedPreferencesUtils.updateBatteryOptimizationMode(
                context,
                UID,
                PACKAGE_NAME,
                MODE_RESTRICTED,
                Action.EXTERNAL_UPDATE,
                testBatteryOptimizeUtils
            )

        verify(testBatteryOptimizeUtils)?.setAppUsageState(MODE_RESTRICTED, Action.EXTERNAL_UPDATE)
        assertThat(currentOptMode).isEqualTo(MODE_RESTRICTED)
    }

    private fun insertAppOptModeEventForTest(expirationTime: Long, mode: Int = MODE_OPTIMIZED) {
        whenever(testBatteryOptimizeUtils?.isOptimizeModeMutable).thenReturn(true)
        whenever(testBatteryOptimizeUtils?.getAppOptimizationMode(true)).thenReturn(mode)
        AppOptModeSharedPreferencesUtils.updateAppOptModeExpirationInternal(
            context,
            mutableListOf(UID),
            mutableListOf(PACKAGE_NAME),
            mutableListOf(mode),
            longArrayOf(expirationTime),
        ) { _: Int, _: String ->
            testBatteryOptimizeUtils
        }
    }

    companion object {
        const val UID: Int = 12345
        const val UNSET_UID: Int = 15432
        const val PACKAGE_NAME: String = "com.android.app"

        private fun assertAppOptimizationModeEventInfo(
            event: AppOptimizationModeEvent,
            uid: Int,
            packageName: String,
            resetOptimizationMode: Int,
            expirationTime: Long
        ) {
            assertThat(event.uid).isEqualTo(uid)
            assertThat(event.packageName).isEqualTo(packageName)
            assertThat(event.resetOptimizationMode).isEqualTo(resetOptimizationMode)
            assertThat(event.expirationTime).isEqualTo(expirationTime)
        }
    }
}
