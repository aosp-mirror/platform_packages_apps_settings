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

package com.android.settings.spa

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SpaDestinationTest {

    private val activity = mock<Activity>()

    @Test
    fun startFromExportedActivity() {
        val spaDestination = SpaDestination(destination = DESTINATION, highlightMenuKey = null)

        spaDestination.startFromExportedActivity(activity)

        verify(activity).startActivity(argThat {
            component!!.className == SpaActivity::class.qualifiedName
            getStringExtra(KEY_DESTINATION) == DESTINATION
        })
    }

    private companion object {
        const val DESTINATION = "Destination"
    }
}
