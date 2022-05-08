/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datetime.timezone;

import android.annotation.StringRes;
import android.content.res.Resources;
import android.icu.text.CaseMap;
import android.icu.text.Edits;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;


public class SpannableUtil {
    private static final String TAG = "SpannableUtil";

    private static class SpannableFormattable implements Formattable {

        private final Spannable mSpannable;

        private SpannableFormattable(Spannable spannable) {
            this.mSpannable = spannable;
        }

        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            CharSequence s = handlePrecision(mSpannable, precision);
            s = handleWidth(s, width, (flags & FormattableFlags.LEFT_JUSTIFY) != 0);
            try {
                formatter.out().append(s);
            } catch (IOException e) {
                // The error should never occur because formatter.out() returns
                // SpannableStringBuilder which doesn't throw IOException.
                Log.e(TAG, "error in SpannableFormattable", e);
            }
        }

        private static CharSequence handlePrecision(CharSequence s, int precision) {
            if (precision != -1 && precision < s.length()) {
                return s.subSequence(0, precision);
            }
            return s;
        }

        private static CharSequence handleWidth(CharSequence s, int width, boolean isLeftJustify) {
            if (width == -1) {
                return s;
            }
            int diff = width - s.length();
            if (diff <= 0) {
                return s;
            }
            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (!isLeftJustify) {
                sb.append(" ".repeat(diff));
            }
            sb.append(s);
            if (isLeftJustify) {
                sb.append(" ".repeat(diff));
            }
            return sb;
        }
    }

    /**
     * {@class Resources} has no method to format string resource with {@class Spannable} a
     * rguments. It's a helper method for this purpose.
     */
    public static Spannable getResourcesText(Resources res, @StringRes int resId,
            Object... args) {
        final Locale locale = res.getConfiguration().getLocales().get(0);
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        // Formatter converts CharSequence to String by calling toString() if an arg isn't
        // Formattable. Wrap Spannable by SpannableFormattable to preserve Spannable objects.
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Spannable) {
                args[i] = new SpannableFormattable((Spannable) args[i]);
            }
        }
        new Formatter(builder, locale).format(res.getString(resId), args);
        return builder;
    }

    private static final CaseMap.Title TITLE_CASE_MAP =
            CaseMap.toTitle().sentences().noLowercase();

    /**
     * Titlecasing {@link CharSequence} and {@link Spannable} by using {@link CaseMap.Title}.
     */
    public static CharSequence titleCaseSentences(Locale locale, CharSequence src) {
        if (src instanceof Spannable) {
            return applyCaseMapToSpannable(locale, TITLE_CASE_MAP, (Spannable) src);
        } else {
            return TITLE_CASE_MAP.apply(locale, null, src);
        }
    }

    private static Spannable applyCaseMapToSpannable(Locale locale, CaseMap.Title caseMap,
            Spannable src) {
        Edits edits = new Edits();
        SpannableStringBuilder dest = new SpannableStringBuilder();
        caseMap.apply(locale, null, src, dest, edits);
        if (!edits.hasChanges()) {
            return src;
        }
        Edits.Iterator iterator = edits.getCoarseChangesIterator();
        List<int[]> changes = new ArrayList<>();
        while (iterator.next()) {
            int[] change = new int[] {
                iterator.sourceIndex(),       // 0
                iterator.oldLength(),         // 1
                iterator.destinationIndex(),  // 2
                iterator.newLength(),         // 3
            };
            changes.add(change);
        }
        // Replacement starts from the end to avoid shifting the source index during replacement
        Collections.reverse(changes);
        SpannableStringBuilder result = new SpannableStringBuilder(src);
        for (int[] c : changes) {
            result.replace(c[0], c[0] + c[1], dest, c[2], c[2] + c[3]);
        }
        return result;
    }
}
