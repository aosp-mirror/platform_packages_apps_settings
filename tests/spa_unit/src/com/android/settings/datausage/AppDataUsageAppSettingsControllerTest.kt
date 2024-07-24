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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppDataUsageAppSettingsControllerTest {
    private val packageManager = mock<PackageManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn packageManager
    }

    private val controller = AppDataUsageAppSettingsController(context, KEY)

    private val preference = PreferenceCategory(context).apply { key = KEY }

    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
    }

    @Test
    fun onViewCreated_noSettingsActivity_hidePreference(): Unit = runBlocking {
        controller.init(listOf(PACKAGE_NAME), USER_ID)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun onViewCreated_hasSettingsActivity_showPreference(): Unit = runBlocking {
        packageManager.stub {
            on {
                resolveActivityAsUser(
                    argThat {
                        action == Intent.ACTION_MANAGE_NETWORK_USAGE && getPackage() == PACKAGE_NAME
                    },
                    eq(0),
                    eq(USER_ID),
                )
            } doReturn ResolveInfo()
        }
        controller.init(listOf(PACKAGE_NAME), USER_ID)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isTrue()
    }

    private companion object {
        const val KEY = "test_key"
        const val PACKAGE_NAME = "package.name"
        const val USER_ID = 0
    }
}
