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

package com.android.settings.datausage.lib

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.IconDrawableFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppPreferenceRepositoryTest {
    private val packageManager = mock<PackageManager> {
        on { getPackagesForUid(UID) } doReturn arrayOf(PACKAGE_NAME)
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn packageManager
    }

    private val mockIconDrawableFactory = mock<IconDrawableFactory>()

    private val repository = AppPreferenceRepository(context, mockIconDrawableFactory)

    @Test
    fun loadAppPreferences_packageNotFound_returnEmpty() {
        packageManager.stub {
            on {
                getApplicationInfoAsUser(PACKAGE_NAME, 0, UserHandle.getUserId(UID))
            } doThrow PackageManager.NameNotFoundException()
        }

        val preferences = repository.loadAppPreferences(listOf(UID))

        assertThat(preferences).isEmpty()
    }

    @Test
    fun loadAppPreferences_packageFound_returnPreference() {
        val app = mock<ApplicationInfo> {
            on { loadLabel(any()) } doReturn LABEL
        }
        mockIconDrawableFactory.stub {
            on { getBadgedIcon(app) } doReturn UNBADGED_ICON
        }
        packageManager.stub {
            on {
                getApplicationInfoAsUser(PACKAGE_NAME, 0, UserHandle.getUserId(UID))
            } doReturn app
        }

        val preferences = repository.loadAppPreferences(listOf(UID))

        assertThat(preferences).hasSize(1)
        preferences[0].apply {
            assertThat(title).isEqualTo(LABEL)
            assertThat(icon).isNotNull()
            assertThat(isSelectable).isFalse()
        }
    }

    private companion object {
        const val UID = 10000
        const val PACKAGE_NAME = "package.name"
        const val LABEL = "Label"
        val UNBADGED_ICON = mock<Drawable>()
    }
}
