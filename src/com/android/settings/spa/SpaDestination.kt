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
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import com.android.settings.SettingsActivity.META_DATA_KEY_HIGHLIGHT_MENU_KEY

data class SpaDestination(
    val destination: String,
    val highlightMenuKey: String?,
) {
    companion object {
        fun Activity.getDestination(
            destinationFactory: (String) -> String? = { it },
        ): SpaDestination? {
            val metaData = packageManager.getActivityInfo(
                componentName,
                PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            ).metaData
            val destination = metaData.getString(META_DATA_KEY_DESTINATION)
            if (destination.isNullOrBlank()) return null
            val finalDestination = destinationFactory(destination)
            if (finalDestination.isNullOrBlank()) return null
            return SpaDestination(
                destination = finalDestination,
                highlightMenuKey = metaData.getString(META_DATA_KEY_HIGHLIGHT_MENU_KEY),
            )
        }

        @VisibleForTesting
        const val META_DATA_KEY_DESTINATION = "com.android.settings.spa.DESTINATION"
    }
}
