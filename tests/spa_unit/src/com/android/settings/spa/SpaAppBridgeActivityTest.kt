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

import android.content.Intent
import android.net.Uri
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.SpaAppBridgeActivity.Companion.getDestinationForApp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpaAppBridgeActivityTest {
    @Test
    fun getDestinationForApp_hasPackageName() {
        val intent = Intent().apply {
            data = Uri.parse("package:${PACKAGE_NAME}")
        }

        val destination = getDestinationForApp(DESTINATION, intent)

        assertThat(destination).isEqualTo("$DESTINATION/$PACKAGE_NAME/${UserHandle.myUserId()}")
    }

    @Test
    fun getDestinationForApp_noPackageName() {
        val intent = Intent()

        val destination = getDestinationForApp(DESTINATION, intent)

        assertThat(destination).isNull()
    }

    private companion object {
        const val DESTINATION = "Destination"
        const val PACKAGE_NAME = "package.name"
    }
}
