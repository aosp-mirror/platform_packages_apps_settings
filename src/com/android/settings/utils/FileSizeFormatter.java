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

import android.content.Context;
import android.icu.text.DecimalFormat;
import android.icu.text.MeasureFormat;
import android.icu.text.NumberFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Utility class to aid in formatting file sizes always with the same unit. This is modified from
 * android.text.format.Formatter to fit this purpose.
 */
public final class FileSizeFormatter {
    public static final long KILOBYTE_IN_BYTES = 1000;
    public static final long MEGABYTE_IN_BYTES = KILOBYTE_IN_BYTES * 1000;
    public static final long GIGABYTE_IN_BYTES = MEGABYTE_IN_BYTES * 1000;

    private static class RoundedBytesResult {
        public final float value;
        public final MeasureUnit units;
        public final int fractionDigits;
        public final long roundedBytes;

        private RoundedBytesResult(
                float value, MeasureUnit units, int fractionDigits, long roundedBytes) {
            this.value = value;
            this.units = units;
            this.fractionDigits = fractionDigits;
            this.roundedBytes = roundedBytes;
        }
    }

    private static Locale localeFromContext(@NonNull Context context) {
        return context.getResources().getConfiguration().locale;
    }

    private static String bidiWrap(@NonNull Context context, String source) {
        final Locale locale = localeFromContext(context);
        if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            return BidiFormatter.getInstance(true /* RTL*/).unicodeWrap(source);
        } else {
            return source;
        }
    }

    private static NumberFormat getNumberFormatter(Locale locale, int fractionDigits) {
        final NumberFormat numberFormatter = NumberFormat.getInstance(locale);
        numberFormatter.setMinimumFractionDigits(fractionDigits);
        numberFormatter.setMaximumFractionDigits(fractionDigits);
        numberFormatter.setGroupingUsed(false);
        if (numberFormatter instanceof DecimalFormat) {
            // We do this only for DecimalFormat, since in the general NumberFormat case, calling
            // setRoundingMode may throw an exception.
            numberFormatter.setRoundingMode(BigDecimal.ROUND_HALF_UP);
        }
        return numberFormatter;
    }

    private static String formatMeasureShort(Locale locale, NumberFormat numberFormatter,
            float value, MeasureUnit units) {
        final MeasureFormat measureFormatter = MeasureFormat.getInstance(
                locale, MeasureFormat.FormatWidth.SHORT, numberFormatter);
        return measureFormatter.format(new Measure(value, units));
    }

    private static String formatRoundedBytesResult(
            @NonNull Context context, @NonNull RoundedBytesResult input) {
        final Locale locale = localeFromContext(context);
        final NumberFormat numberFormatter = getNumberFormatter(locale, input.fractionDigits);
        return formatMeasureShort(locale, numberFormatter, input.value, input.units);
    }

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
     * @param unit The unit used for formatting.
     * @param mult Amount of bytes in the unit.
     * @return formatted string with the number
     */
    public static String formatFileSize(
            @Nullable Context context, long sizeBytes, MeasureUnit unit, long mult) {
        if (context == null) {
            return "";
        }
        final RoundedBytesResult res = formatBytes(sizeBytes, unit, mult);
        return bidiWrap(context, formatRoundedBytesResult(context, res));
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
    private static RoundedBytesResult formatBytes(
            long sizeBytes, MeasureUnit unit, long mult) {
        final boolean isNegative = (sizeBytes < 0);
        float result = isNegative ? -sizeBytes : sizeBytes;
        result = result / mult;
        // Note we calculate the rounded long by ourselves, but still let String.format()
        // compute the rounded value. String.format("%f", 0.1) might not return "0.1" due to
        // floating point errors.
        final int roundFactor;
        final int roundDigits;
        if (mult == 1) {
            roundFactor = 1;
            roundDigits = 0;
        } else if (result < 1) {
            roundFactor = 100;
            roundDigits = 2;
        } else if (result < 10) {
            roundFactor = 10;
            roundDigits = 1;
        } else { // 10 <= result < 100
            roundFactor = 1;
            roundDigits = 0;
        }

        if (isNegative) {
            result = -result;
        }

        // Note this might overflow if abs(result) >= Long.MAX_VALUE / 100, but that's like 80PB so
        // it's okay (for now)...
        final long roundedBytes = (((long) Math.round(result * roundFactor)) * mult / roundFactor);

        return new RoundedBytesResult(result, unit, roundDigits, roundedBytes);
    }
}
