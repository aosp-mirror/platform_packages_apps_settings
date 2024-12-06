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
package com.android.settings.deviceinfo.legal

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class LegalPreferenceTest {
    private val pkgManager = mock<PackageManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager(): PackageManager = pkgManager
        }

    private val copyrightPreference =
        LegalPreference("copyright", R.string.copyright_title, "android.settings.COPYRIGHT")

    @Test
    fun isAvailable_systemApp_shouldReturnTrue() {
        val testResolveInfos: MutableList<ResolveInfo> = ArrayList()
        testResolveInfos.add(getTestResolveInfo(/* isSystemApp= */ true))

        pkgManager.stub {
            on { queryIntentActivities(any(Intent::class.java), anyInt()) } doReturn
                testResolveInfos
        }

        assertThat(copyrightPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_nonSystemApp_shouldReturnFalse() {
        val testResolveInfos: MutableList<ResolveInfo> = ArrayList()
        testResolveInfos.add(getTestResolveInfo(/* isSystemApp= */ false))

        pkgManager.stub {
            on { queryIntentActivities(any(Intent::class.java), anyInt()) } doReturn
                testResolveInfos
        }

        assertThat(copyrightPreference.isAvailable(context)).isFalse()
    }

    private fun getTestResolveInfo(isSystemApp: Boolean): ResolveInfo {
        val testResolveInfo = ResolveInfo()
        val testAppInfo = ApplicationInfo()
        if (isSystemApp) {
            testAppInfo.flags = testAppInfo.flags or ApplicationInfo.FLAG_SYSTEM
        }

        testResolveInfo.activityInfo =
            ActivityInfo().apply {
                name = "TestActivityName"
                packageName = "TestPackageName"
                applicationInfo = testAppInfo
            }
        return testResolveInfo
    }
}
// LINT.ThenChange(CopyrightPreferenceControllerTest.java)
