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
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import com.android.settings.spa.SpaBridgeActivity.Companion.startSpaActivityFromBridge

/**
 * Activity used as a bridge to [SpaActivity] with package scheme for application usage.
 *
 * Since [SpaActivity] is not exported, [SpaActivity] could not be the target activity of
 * <activity-alias>, otherwise all its pages will be exported.
 * So need this bridge activity to sit in the middle of <activity-alias> and [SpaActivity].
 */
class SpaAppBridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startSpaActivityFromBridge { getDestinationForApp(it, intent) }
        finish()
    }

    companion object {
        fun getDestinationForApp(destinationPrefix: String, intent: Intent): String? {
            val packageName = intent.data?.schemeSpecificPart?.takeIf { Regex("^([a-zA-Z]\\w*\\.)*[a-zA-Z]\\w*$").matches(it) } ?: return null
            return "$destinationPrefix/$packageName/${UserHandle.myUserId()}"
        }
    }
}
