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

package com.android.settings.spa.app.specialaccess

import android.content.Context
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BackupTasksAppsPreferenceControllerTest {

    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val matchedPreference = Preference(context).apply { key = preferenceKey }

    private val misMatchedPreference = Preference(context).apply { key = testPreferenceKey }

    private val controller = BackupTasksAppsPreferenceController(context, preferenceKey)

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PERFORM_BACKUP_TASKS_IN_SETTINGS)
    fun getAvailabilityStatus_enableBackupTasksApps_returnAvailable() {
        assertThat(controller.isAvailable).isTrue()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_PERFORM_BACKUP_TASKS_IN_SETTINGS)
    fun getAvailableStatus_disableBackupTasksApps_returnConditionallyUnavailable() {
        assertThat(controller.isAvailable).isFalse()
    }

    @Test
    fun handlePreferenceTreeClick_keyMatched_returnTrue() {
        assertThat(controller.handlePreferenceTreeClick(matchedPreference)).isTrue()
    }

    @Test
    fun handlePreferenceTreeClick_keyMisMatched_returnFalse() {
        assertThat(controller.handlePreferenceTreeClick(misMatchedPreference)).isFalse()
    }

    companion object {
        private const val preferenceKey: String = "backup_tasks_apps"
        private const val testPreferenceKey: String = "test_key"
    }
}