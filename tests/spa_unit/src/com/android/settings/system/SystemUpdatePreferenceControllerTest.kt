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

package com.android.settings.system

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.SystemUpdateManager
import android.os.UserManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SystemUpdatePreferenceControllerTest {
    private val mockUserManager = mock<UserManager>()
    private val mockSystemUpdateManager = mock<SystemUpdateManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { userManager } doReturn mockUserManager
        on { getSystemService(SystemUpdateManager::class.java) } doReturn mockSystemUpdateManager
    }

    private val resources = spy(context.resources) {
        on { getBoolean(R.bool.config_show_system_update_settings) } doReturn true
    }

    private val preference = Preference(context).apply { key = KEY }
    private val preferenceScreen = mock<PreferenceScreen> {
        onGeneric { findPreference(KEY) } doReturn preference
    }
    private val controller = SystemUpdatePreferenceController(context, KEY)

    @Before
    fun setUp() {
        whenever(context.resources).thenReturn(resources)
    }

    @Test
    fun updateNonIndexable_ifAvailable_shouldNotUpdate() {
        whenever(mockUserManager.isAdminUser).thenReturn(true)
        val keys = mutableListOf<String>()

        controller.updateNonIndexableKeys(keys)

        assertThat(keys).isEmpty()
    }

    @Test
    fun updateNonIndexable_ifNotAvailable_shouldUpdate() {
        whenever(mockUserManager.isAdminUser).thenReturn(false)
        val keys = mutableListOf<String>()

        controller.updateNonIndexableKeys(keys)

        assertThat(keys).containsExactly(KEY)
    }

    @Test
    fun displayPrefs_ifVisible_butNotAdminUser_shouldNotDisplay() {
        whenever(mockUserManager.isAdminUser).thenReturn(false)

        controller.displayPreference(preferenceScreen)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun displayPrefs_ifAdminUser_butNotVisible_shouldNotDisplay() {
        whenever(mockUserManager.isAdminUser).thenReturn(true)
        whenever(resources.getBoolean(R.bool.config_show_system_update_settings)).thenReturn(false)

        controller.displayPreference(preferenceScreen)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun displayPrefs_ifAvailable_shouldDisplay() {
        whenever(mockUserManager.isAdminUser).thenReturn(true)

        controller.displayPreference(preferenceScreen)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    fun updateState_systemUpdateStatusUnknown_shouldSetToAndroidVersion() {
        val bundle = Bundle().apply {
            putInt(SystemUpdateManager.KEY_STATUS, SystemUpdateManager.STATUS_UNKNOWN)
        }
        whenever(mockSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        SystemClock.sleep(100)

        assertThat(preference.summary).isEqualTo(
            context.getString(
                R.string.android_version_summary,
                Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY,
            )
        )
    }

    @Test
    fun updateState_systemUpdateStatusIdle_shouldSetToAndroidVersion() {
        val testReleaseName = "ANDROID TEST VERSION"
        val bundle = Bundle().apply {
            putInt(SystemUpdateManager.KEY_STATUS, SystemUpdateManager.STATUS_IDLE)
            putString(SystemUpdateManager.KEY_TITLE, testReleaseName)
        }
        whenever(mockSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        SystemClock.sleep(100)

        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.android_version_summary, testReleaseName))
    }

    @Test
    fun updateState_systemUpdateInProgress_shouldSetToUpdatePending() {
        val bundle = Bundle().apply {
            putInt(SystemUpdateManager.KEY_STATUS, SystemUpdateManager.STATUS_WAITING_DOWNLOAD)
        }
        whenever(mockSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        SystemClock.sleep(100)

        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.android_version_pending_update_summary))
    }

    private companion object {
        const val KEY = "test_key"
    }
}
