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

package com.android.settings.spa

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.SpaBridgeActivity.Companion.META_DATA_KEY_DESTINATION
import com.android.settings.spa.SpaBridgeActivity.Companion.getDestination
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SpaBridgeActivityTest {
    private val mockPackageManager = mock<PackageManager> {
        on { getActivityInfo(eq(COMPONENT_NAME), any<ComponentInfoFlags>()) } doReturn
            ActivityInfo().apply {
                metaData = bundleOf(META_DATA_KEY_DESTINATION to DESTINATION)
            }
    }

    private val activity = mock<Activity> {
        on { componentName } doReturn COMPONENT_NAME
        on { packageManager } doReturn mockPackageManager
    }

    @Test
    fun getDestination() {
        val destination = activity.getDestination()

        assertThat(destination).isEqualTo(DESTINATION)
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val ACTIVITY_NAME = "ActivityName"
        val COMPONENT_NAME = ComponentName(PACKAGE_NAME, ACTIVITY_NAME)
        const val DESTINATION = "Destination"
    }
}
