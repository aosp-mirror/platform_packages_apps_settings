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

package com.android.settings.utils;

import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

/**
 * This class is used to add {@link View.OnClickListener} for the text been wrapped by
 * annotation.
 */
public class AnnotationSpan extends URLSpan {
    private final View.OnClickListener mClickListener;

    private AnnotationSpan(View.OnClickListener lsn) {
        super((String) null);
        mClickListener = lsn;
    }

    @Override
    public void onClick(View widget) {
        if (mClickListener != null) {
            mClickListener.onClick(widget);
        }
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
    }

    public static CharSequence linkify(CharSequence rawText, LinkInfo... linkInfos) {
        SpannableString msg = new SpannableString(rawText);
        Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
        SpannableStringBuilder builder = new SpannableStringBuilder(msg);
        for (Annotation annotation : spans) {
            final String key = annotation.getValue();
            int start = msg.getSpanStart(annotation);
            int end = msg.getSpanEnd(annotation);
            AnnotationSpan link = null;
            for (LinkInfo linkInfo : linkInfos) {
                if (linkInfo.annotation.equals(key)) {
                    link = new AnnotationSpan(linkInfo.listener);
                    break;
                }
            }
            if (link != null) {
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
            }
        }
        return builder;
    }

    /**
     * Data class to store the annotation and the click action
     */
    public static class LinkInfo {
        public final String annotation;
        public final View.OnClickListener listener;

        public LinkInfo(String annotation, View.OnClickListener listener) {
            this.annotation = annotation;
            this.listener = listener;
        }
    }
}
