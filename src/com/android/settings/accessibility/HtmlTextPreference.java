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

/**
 * A custom {@link android.widget.TextView} preference that shows html text with a custom tag
 * filter.
 */
public final class HtmlTextPreference extends StaticTextPreference {

    private int mFlag = Html.FROM_HTML_MODE_COMPACT;
    private Html.ImageGetter mImageGetter;
    private Html.TagHandler mTagHandler;

    HtmlTextPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summaryView = holder.itemView.findViewById(android.R.id.summary);
        if (summaryView != null && !TextUtils.isEmpty(getSummary())) {
            summaryView.setText(
                    Html.fromHtml(getSummary().toString(), mFlag, mImageGetter, mTagHandler));
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
}
