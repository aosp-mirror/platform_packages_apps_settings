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

package com.android.settings.datausage

import android.content.Context
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.AppPreferenceRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AppDataUsageListControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repository = mock<AppPreferenceRepository> {
        on { loadAppPreferences(any()) } doAnswer {
            val uids = it.arguments[0] as List<*>
            uids.map { Preference(context) }
        }
    }

    private val controller = AppDataUsageListController(
        context = context,
        preferenceKey = KEY,
        repository = repository,
    )

    private val preference = PreferenceCategory(context).apply { key = KEY }

    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
    }

    @Test
    fun onViewCreated_singleUid_hidePreference(): Unit = runBlocking {
        controller.init(listOf(UID_0))
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun onViewCreated_twoUid_showPreference(): Unit = runBlocking {
        controller.init(listOf(UID_0, UID_1))
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isTrue()
        assertThat(preference.preferenceCount).isEqualTo(2)
    }

    private companion object {
        const val KEY = "test_key"
        const val UID_0 = 10000
        const val UID_1 = 10001
    }
}
