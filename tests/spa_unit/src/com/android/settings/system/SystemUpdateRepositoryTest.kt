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
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SystemUpdateRepositoryTest {
    private val mockPackageManager = mock<PackageManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn mockPackageManager
    }

    private val repository = SystemUpdateRepository(context)

    @Test
    fun getSystemUpdateIntent_noResolveActivity_returnNull() {
        val intent = repository.getSystemUpdateIntent()

        assertThat(intent).isNull()
    }

    @Test
    fun getSystemUpdateIntent_hasResolveActivity_returnIntent() {
        mockPackageManager.stub {
            on {
                resolveActivity(
                    argThat { action == Settings.ACTION_SYSTEM_UPDATE_SETTINGS },
                    eq(PackageManager.MATCH_SYSTEM_ONLY),
                )
            } doReturn RESOLVE_INFO
        }

        val intent = repository.getSystemUpdateIntent()

        assertThat(intent?.component?.packageName).isEqualTo(PACKAGE_NAME)
        assertThat(intent?.component?.className).isEqualTo(ACTIVITY_NAME)
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val ACTIVITY_NAME = "ActivityName"
        val RESOLVE_INFO = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
                name = ACTIVITY_NAME
            }
        }
    }
}
