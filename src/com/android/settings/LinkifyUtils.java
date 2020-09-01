/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * Utility class to create clickable links inside {@link TextView TextViews}.
 */
public class LinkifyUtils {
    private static final String PLACE_HOLDER_LINK_BEGIN = "LINK_BEGIN";
    private static final String PLACE_HOLDER_LINK_END = "LINK_END";

    private LinkifyUtils() {
    }

    /** Interface that handles the click event of the link */
    public interface OnClickListener {
        void onClick();
    }

    /**
     * Applies the text into the {@link TextView} and part of it a clickable link.
     * The text surrounded with "LINK_BEGIN" and "LINK_END" will become a clickable link. Only
     * supports at most one link.
     * @return true if the link has been successfully applied, or false if the original text
     *         contains no link place holders.
     */
    public static boolean linkify(TextView textView, StringBuilder text,
            final OnClickListener listener) {
        // Remove place-holders from the string and record their positions
        final int beginIndex = text.indexOf(PLACE_HOLDER_LINK_BEGIN);
        if (beginIndex == -1) {
            textView.setText(text);
            return false;
        }
        text.delete(beginIndex, beginIndex + PLACE_HOLDER_LINK_BEGIN.length());
        final int endIndex = text.indexOf(PLACE_HOLDER_LINK_END);
        if (endIndex == -1) {
            textView.setText(text);
            return false;
        }
        text.delete(endIndex, endIndex + PLACE_HOLDER_LINK_END.length());

        textView.setText(text.toString(), BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        Spannable spannableContent = (Spannable) textView.getText();
        ClickableSpan spannableLink = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                listener.onClick();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        spannableContent.setSpan(spannableLink, beginIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return true;
    }
}
