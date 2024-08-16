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

import android.content.Context
import android.content.Intent
import com.android.settings.activityembedding.ActivityEmbeddingUtils
import com.android.settings.activityembedding.EmbeddedDeepLinkUtils.tryStartMultiPaneDeepLink
import com.android.settingslib.spa.framework.util.SESSION_EXTERNAL
import com.android.settingslib.spa.framework.util.appendSpaParams

data class SpaDestination(
    val destination: String,
    val highlightMenuKey: String?,
) {
    fun startFromExportedActivity(context: Context) {
        val intent = Intent(context, SpaActivity::class.java)
            .appendSpaParams(
                destination = destination,
                sessionName = SESSION_EXTERNAL,
            )
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context) ||
            !context.tryStartMultiPaneDeepLink(intent, highlightMenuKey)
        ) {
            context.startActivity(intent)
        }
    }
}
