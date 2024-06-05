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

package com.android.settings.datausage.lib

import android.util.Range

/**
 * Details data structure representing usage data in a period.
 */
data class NetworkUsageDetailsData(
    val range: Range<Long>,
    val totalUsage: Long,
    val foregroundUsage: Long,
    val backgroundUsage: Long,
) {
    companion object {
        val AllZero = NetworkUsageDetailsData(
            range = Range(0, 0),
            totalUsage = 0,
            foregroundUsage = 0,
            backgroundUsage = 0,
        )
    }
}
