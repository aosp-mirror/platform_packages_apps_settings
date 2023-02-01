/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.pm.PackageManager.ComponentInfoFlags
import android.os.Bundle
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity

/**
 * Activity used as a bridge to [SpaActivity].
 *
 * Since [SpaActivity] is not exported, [SpaActivity] could not be the target activity of
 * <activity-alias>, otherwise all its pages will be exported.
 * So need this bridge activity to sit in the middle of <activity-alias> and [SpaActivity].
 */
class SpaBridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getDestination()?.let { destination ->
            startSpaActivity(destination)
        }
        finish()
    }

    companion object {
        fun Activity.getDestination(): String? =
            packageManager.getActivityInfo(
                componentName, ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            ).metaData.getString(META_DATA_KEY_DESTINATION)

        private const val META_DATA_KEY_DESTINATION = "com.android.settings.spa.DESTINATION"
    }
}
