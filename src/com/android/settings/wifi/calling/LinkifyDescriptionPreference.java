/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.core.text.util.LinkifyCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** A preference which supports linkify text as a description in the summary **/
public class LinkifyDescriptionPreference extends Preference {

    public LinkifyDescriptionPreference(Context context) {
        this(context, null);
    }

    public LinkifyDescriptionPreference(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView == null || summaryView.getVisibility() != View.VISIBLE) {
            return;
        }

        final CharSequence summary = getSummary();
        if (TextUtils.isEmpty(summary)) {
            return;
        }

        summaryView.setMaxLines(Integer.MAX_VALUE);

        final SpannableString spannableSummary = new SpannableString(summary);
        if (spannableSummary.getSpans(0, spannableSummary.length(), ClickableSpan.class)
                .length > 0) {
            summaryView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        LinkifyCompat.addLinks(summaryView,
                Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
    }
}
