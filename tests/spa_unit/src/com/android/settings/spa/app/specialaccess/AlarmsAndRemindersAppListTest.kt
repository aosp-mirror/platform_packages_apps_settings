/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.compat.CompatChanges
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.PowerExemptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AlarmsAndRemindersAppListTest {
    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var powerExemptionManager: PowerExemptionManager

    @Mock
    private lateinit var packageManagers: IPackageManagers

    private lateinit var listModel: AlarmsAndRemindersAppListModel

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(CompatChanges::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(CompatChanges.isChangeEnabled(anyLong(), anyString(), any())).thenReturn(true)
        whenever(context.getSystemService(PowerExemptionManager::class.java))
            .thenReturn(powerExemptionManager)
        with(packageManagers) {
            whenever(APP.hasRequestPermission(Manifest.permission.SCHEDULE_EXACT_ALARM))
                .thenReturn(true)
        }
        listModel = AlarmsAndRemindersAppListModel(context, packageManagers)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun transformItem_recordHasCorrectApp() {
        val record = listModel.transformItem(APP)

        assertThat(record.app).isSameInstanceAs(APP)
    }

    @Test
    fun transformItem_whenNotRequestScheduleExactAlarm_recordHasCorrectState() {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(Manifest.permission.SCHEDULE_EXACT_ALARM))
                .thenReturn(false)
        }
        val record = listModel.transformItem(APP)

        assertThat(record.isTrumped).isFalse()
        assertThat(record.isChangeable).isFalse()
    }

    @Test
    fun transformItem_whenRequestUseExactAlarm_recordHasCorrectState() {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(Manifest.permission.USE_EXACT_ALARM))
                .thenReturn(true)
        }
        val record = listModel.transformItem(APP)

        assertThat(record.isTrumped).isTrue()
        assertThat(record.isChangeable).isFalse()
    }

    @Test
    fun transformItem_whenPowerAllowListed_recordHasCorrectState() {
        whenever(powerExemptionManager.isAllowListed(PACKAGE_NAME, true)).thenReturn(true)
        val record = listModel.transformItem(APP)

        assertThat(record.isTrumped).isTrue()
        assertThat(record.isChangeable).isFalse()
    }

    @Test
    fun transformItem_whenNotTrumped_recordHasCorrectState() {
        val record = listModel.transformItem(APP)

        assertThat(record.isTrumped).isFalse()
        assertThat(record.isChangeable).isTrue()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}