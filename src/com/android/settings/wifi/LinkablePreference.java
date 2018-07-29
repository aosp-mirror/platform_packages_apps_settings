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

package com.android.settings.wifi;

import android.content.Context;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.LinkifyUtils;
import com.android.settingslib.R;

/**
 * A preference with a title that can have linkable content on click.
 */
public class LinkablePreference extends Preference {

    private LinkifyUtils.OnClickListener mClickListener;
    private CharSequence mContentTitle;
    private CharSequence mContentDescription;


    public LinkablePreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setIcon(R.drawable.ic_info_outline_24dp);
        setSelectable(false);
    }

    public LinkablePreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, TypedArrayUtils.getAttr(
                ctx, R.attr.footerPreferenceStyle, android.R.attr.preferenceStyle));
    }

    public LinkablePreference(Context ctx) {
        this(ctx, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView == null) {
            return;
        }
        textView.setSingleLine(false);

        if (mContentTitle == null || mClickListener == null) {
            return;
        }

        StringBuilder contentBuilder = new StringBuilder().append(mContentTitle);
        if (mContentDescription != null) {
            contentBuilder.append("\n\n");
            contentBuilder.append(mContentDescription);
        }

        boolean linked = LinkifyUtils.linkify(textView, contentBuilder, mClickListener);
        if (linked && mContentTitle != null) {
            Spannable spannableContent = (Spannable) textView.getText();
            spannableContent.setSpan(
                    new TextAppearanceSpan(getContext(), android.R.style.TextAppearance_Small),
                    0,
                    mContentTitle.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            textView.setText(spannableContent);
            textView.setMovementMethod(new LinkMovementMethod());
        }
    }

    /**
     * Sets the linkable text for the Preference title.
     *
     * @param contentTitle text to set the Preference title.
     * @param contentDescription description text to append underneath title, can be null.
     * @param clickListener OnClickListener for the link portion of the text.
     */
    public void setText(
            CharSequence contentTitle,
            @Nullable CharSequence contentDescription,
            LinkifyUtils.OnClickListener clickListener) {
        mContentTitle = contentTitle;
        mContentDescription = contentDescription;
        mClickListener = clickListener;
        // sets the title so that the title TextView is not hidden in super.onBindViewHolder()
        super.setTitle(contentTitle);
    }

    /**
     * Sets the title of the LinkablePreference. resets linkable text for reusability.
     */
    @Override
    public void setTitle(int titleResId) {
        mContentTitle = null;
        mContentDescription = null;
        super.setTitle(titleResId);
    }

    /**
     * Sets the title of the LinkablePreference. resets linkable text for reusability.
     */
    @Override
    public void setTitle(CharSequence title) {
        mContentTitle = null;
        mContentDescription = null;
        super.setTitle(title);
    }
}
