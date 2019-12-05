/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A custom {@link android.widget.TextView} preference that shows html text with a custom tag
 * filter.
 */
public final class HtmlTextPreference extends StaticTextPreference {

    private boolean mDividerAllowedAbove = false;
    private int mFlag = Html.FROM_HTML_MODE_COMPACT;
    private Html.ImageGetter mImageGetter;
    private Html.TagHandler mTagHandler;
    private List<String> mUnsupportedTagList;

    HtmlTextPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(mDividerAllowedAbove);

        final TextView summaryView = holder.itemView.findViewById(android.R.id.summary);
        if (summaryView != null && !TextUtils.isEmpty(getSummary())) {
            final String filteredText = getFilteredText(getSummary().toString());
            summaryView.setText(Html.fromHtml(filteredText, mFlag, mImageGetter, mTagHandler));
        }
    }

    /**
     * Sets divider whether to show in preference above.
     *
     * @param allowed true will be drawn on above this item
     */
    public void setDividerAllowedAbove(boolean allowed) {
        if (allowed != mDividerAllowedAbove) {
            mDividerAllowedAbove = allowed;
            notifyChanged();
        }
    }

    /**
     * Sets the flag to which text format to be applied.
     *
     * @param flag to indicate that html text format
     */
    public void setFlag(int flag) {
        if (flag != mFlag) {
            mFlag = flag;
            notifyChanged();
        }
    }

    /**
     * Sets image getter and help to load corresponding images when parsing.
     *
     * @param imageGetter to load image by image tag content
     */
    public void setImageGetter(Html.ImageGetter imageGetter) {
        if (imageGetter != null && !imageGetter.equals(mImageGetter)) {
            mImageGetter = imageGetter;
            notifyChanged();
        }
    }

    /**
     * Sets tag handler to handle the unsupported tags.
     *
     * @param tagHandler the handler for unhandled tags
     */
    public void setTagHandler(Html.TagHandler tagHandler) {
        if (tagHandler != null && !tagHandler.equals(mTagHandler)) {
            mTagHandler = tagHandler;
            notifyChanged();
        }
    }

    /**
     * Sets unsupported tag list, the text will be filtered though this list in advanced.
     *
     * @param unsupportedTagList the list of unsupported tags
     */
    public void setUnsupportedTagList(List<String> unsupportedTagList) {
        if (unsupportedTagList != null && !unsupportedTagList.equals(mUnsupportedTagList)) {
            mUnsupportedTagList = unsupportedTagList;
            notifyChanged();
        }
    }

    private String getFilteredText(String text) {
        if (mUnsupportedTagList == null) {
            return text;
        }

        int i = 1;
        for (String tag : mUnsupportedTagList) {
            if (!TextUtils.isEmpty(text)) {
                final String index = String.valueOf(i++);
                final String targetStart1 = "(?i)<" + tag + " ";
                final String targetStart2 = "(?i)<" + tag + ">";
                final String replacementStart1 = "<unsupportedtag" + index + " ";
                final String replacementStart2 = "<unsupportedtag" + index + ">";
                final String targetEnd = "(?i)</" + tag + ">";
                final String replacementEnd = "</unsupportedtag" + index + ">";
                text = Pattern.compile(targetStart1).matcher(text).replaceAll(replacementStart1);
                text = Pattern.compile(targetStart2).matcher(text).replaceAll(replacementStart2);
                text = Pattern.compile(targetEnd).matcher(text).replaceAll(replacementEnd);
            }
        }
        return text;
    }
}
