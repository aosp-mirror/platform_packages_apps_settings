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

package com.android.settings.spa.search

import android.util.Base64
import android.util.Log
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey

private const val TAG = "SpaSearchLandingKeyExt"

fun SpaSearchLandingKey.encodeToString(): String =
    Base64.encodeToString(toByteArray(), Base64.DEFAULT)

fun decodeToSpaSearchLandingKey(input: String): SpaSearchLandingKey? =
    try {
        SpaSearchLandingKey.parseFrom(Base64.decode(input, Base64.DEFAULT))
    } catch (e: Exception) {
        Log.w(TAG, "SpaSearchLandingKey ($input) invalid", e)
        null
    }
