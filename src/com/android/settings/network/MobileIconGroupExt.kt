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

package com.android.settings.network

import android.content.Context
import android.telephony.SubscriptionManager
import android.text.Html
import com.android.settingslib.SignalIcon

fun SignalIcon.MobileIconGroup.getSummaryForSub(context: Context, subId: Int): String =
    when (dataContentDescription) {
        0 -> ""
        else -> {
            SubscriptionManager.getResourcesForSubId(context, subId)
                .getString(dataContentDescription)
        }
    }

fun String.maybeToHtml(): CharSequence = when {
    contains(HTML_TAG) -> Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    else -> this
}

private const val HTML_TAG = "</"
