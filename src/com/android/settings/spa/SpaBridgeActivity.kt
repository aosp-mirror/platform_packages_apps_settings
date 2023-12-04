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
import com.android.settings.activityembedding.ActivityEmbeddingUtils
import com.android.settings.activityembedding.EmbeddedDeepLinkUtils.tryStartMultiPaneDeepLink
import com.android.settings.spa.SpaDestination.Companion.getDestination
import com.android.settingslib.spa.framework.util.SESSION_EXTERNAL
import com.android.settingslib.spa.framework.util.appendSpaParams

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
        startSpaActivityFromBridge()
        finish()
    }

    companion object {
        fun Activity.startSpaActivityFromBridge(destinationFactory: (String) -> String? = { it }) {
            val (destination, highlightMenuKey) = getDestination(destinationFactory) ?: return
            val intent = Intent(this, SpaActivity::class.java)
                .appendSpaParams(
                    destination = destination,
                    sessionName = SESSION_EXTERNAL,
                )
            if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this) ||
                !tryStartMultiPaneDeepLink(intent, highlightMenuKey)
            ) {
                startActivity(intent)
            }
        }
    }
}
