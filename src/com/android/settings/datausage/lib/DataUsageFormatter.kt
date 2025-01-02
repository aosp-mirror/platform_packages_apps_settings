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

package com.android.settings.datausage.lib

import android.content.Context
import android.content.res.Resources
import android.text.format.Formatter
import com.android.settingslib.spaprivileged.framework.common.BytesFormatter

class DataUsageFormatter(private val context: Context) {

    /** Formats the data usage. */
    fun formatDataUsage(sizeBytes: Long): String =
        BytesFormatter(context).format(sizeBytes, BytesFormatter.UseCase.DataUsage)

    companion object {
        /**
         * Gets the display unit of the given bytes.
         *
         * Similar to MeasureFormat.getUnitDisplayName(), but with the expected result for the bytes
         * in Settings, and align with other places in Settings.
         */
        fun Resources.getBytesDisplayUnit(bytes: Long): String =
            Formatter.formatBytes(this, bytes, Formatter.FLAG_IEC_UNITS).units
    }
}
