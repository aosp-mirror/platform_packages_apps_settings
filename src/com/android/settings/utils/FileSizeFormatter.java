/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.utils;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.text.BidiFormatter;
import android.text.format.Formatter;

/**
 * Utility class to aid in formatting file sizes always with the same unit. This is modified from
 * android.text.format.Formatter to fit this purpose.
 */
public final class FileSizeFormatter {
    public static final long KILOBYTE_IN_BYTES = 1000;
    public static final long MEGABYTE_IN_BYTES = KILOBYTE_IN_BYTES * 1000;
    public static final long GIGABYTE_IN_BYTES = MEGABYTE_IN_BYTES * 1000;

    /**
     * Formats a content size to be in the form of bytes, kilobytes, megabytes, etc.
     *
     * <p>As of O, the prefixes are used in their standard meanings in the SI system, so kB = 1000
     * bytes, MB = 1,000,000 bytes, etc.
     *
     * <p class="note">In {@link android.os.Build.VERSION_CODES#N} and earlier, powers of 1024 are
     * used instead, with KB = 1024 bytes, MB = 1,048,576 bytes, etc.
     *
     * <p>If the context has a right-to-left locale, the returned string is wrapped in bidi
     * formatting characters to make sure it's displayed correctly if inserted inside a
     * right-to-left string. (This is useful in cases where the unit strings, like "MB", are
     * left-to-right, but the locale is right-to-left.)
     *
     * @param context Context to use to load the localized units
     * @param sizeBytes size value to be formatted, in bytes
     * @param suffix String id for the unit suffix.
     * @param mult Amount of bytes in the unit. * @return formatted string with the number
     */
    public static String formatFileSize(
            @Nullable Context context, long sizeBytes, int suffix, long mult) {
        if (context == null) {
            return "";
        }
        final Formatter.BytesResult res =
                formatBytes(context.getResources(), sizeBytes, suffix, mult);
        return BidiFormatter.getInstance()
                .unicodeWrap(context.getString(getFileSizeSuffix(context), res.value, res.units));
    }

    private static int getFileSizeSuffix(Context context) {
        final Resources res = context.getResources();
        return res.getIdentifier("fileSizeSuffix", "string", "android");
    }

    /**
     * A simplified version of the SettingsLib file size formatter. The primary difference is that
     * this version always assumes it is doing a "short file size" and allows for a suffix to be
     * provided.
     *
     * @param res Resources to fetch strings with.
     * @param sizeBytes File size in bytes to format.
     * @param suffix String id for the unit suffix.
     * @param mult Amount of bytes in the unit.
     */
    private static Formatter.BytesResult formatBytes(
            Resources res, long sizeBytes, int suffix, long mult) {
        final boolean isNegative = (sizeBytes < 0);
        float result = isNegative ? -sizeBytes : sizeBytes;
        result = result / mult;
        // Note we calculate the rounded long by ourselves, but still let String.format()
        // compute the rounded value. String.format("%f", 0.1) might not return "0.1" due to
        // floating point errors.
        final int roundFactor;
        final String roundFormat;
        if (mult == 1) {
            roundFactor = 1;
            roundFormat = "%.0f";
        } else if (result < 1) {
            roundFactor = 100;
            roundFormat = "%.2f";
        } else if (result < 10) {
            roundFactor = 10;
            roundFormat = "%.1f";
        } else { // 10 <= result < 100
            roundFactor = 1;
            roundFormat = "%.0f";
        }

        if (isNegative) {
            result = -result;
        }
        final String roundedString = String.format(roundFormat, result);

        // Note this might overflow if abs(result) >= Long.MAX_VALUE / 100, but that's like 80PB so
        // it's okay (for now)...
        final long roundedBytes = (((long) Math.round(result * roundFactor)) * mult / roundFactor);

        final String units = res.getString(suffix);

        return new Formatter.BytesResult(roundedString, units, roundedBytes);
    }
}
