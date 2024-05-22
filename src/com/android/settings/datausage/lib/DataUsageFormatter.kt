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

import android.annotation.StringRes
import android.content.Context
import android.content.res.Resources
import android.icu.text.UnicodeSet
import android.icu.text.UnicodeSetSpanner
import android.text.BidiFormatter
import android.text.format.Formatter
import com.android.internal.R

class DataUsageFormatter(private val context: Context) {

    data class FormattedDataUsage(
        val displayText: String,
        val contentDescription: String,
    ) {
        fun format(context: Context, @StringRes resId: Int, vararg formatArgs: Any?) =
            FormattedDataUsage(
                displayText = context.getString(resId, displayText, *formatArgs),
                contentDescription = context.getString(resId, contentDescription, *formatArgs),
            )
    }

    /** Formats the data usage. */
    fun formatDataUsage(sizeBytes: Long): FormattedDataUsage {
        val result = Formatter.formatBytes(context.resources, sizeBytes, Formatter.FLAG_IEC_UNITS)
        return FormattedDataUsage(
            displayText = BidiFormatter.getInstance().unicodeWrap(
                context.getString(R.string.fileSizeSuffix, result.value, result.units)
            ),
            contentDescription = context.getString(
                R.string.fileSizeSuffix, result.value, result.unitsContentDescription
            ),
        )
    }

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
