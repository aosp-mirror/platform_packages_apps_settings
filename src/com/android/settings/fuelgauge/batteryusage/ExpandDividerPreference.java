/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** A preference for expandable section divider. */
public class ExpandDividerPreference extends Preference {
    private static final String TAG = "ExpandDividerPreference";
    @VisibleForTesting
    static final String PREFERENCE_KEY = "expandable_divider";

    @VisibleForTesting
    TextView mTextView;
    @VisibleForTesting
    ImageView mImageView;
    private OnExpandListener mOnExpandListener;

    private boolean mIsExpanded = false;
    private String mTitleContent = null;

    /** A callback listener for expand state is changed by users. */
    public interface OnExpandListener {
        /** Callback function for expand state is changed by users. */
        void onExpand(boolean isExpanded);
    }

    public ExpandDividerPreference(Context context) {
        this(context, /*attrs=*/ null);
    }

    public ExpandDividerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_expand_divider);
        setKey(PREFERENCE_KEY);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mTextView = (TextView) view.findViewById(R.id.expand_title);
        mImageView = (ImageView) view.findViewById(R.id.expand_icon);
        refreshState();
    }

    @Override
    public void onClick() {
        setIsExpanded(!mIsExpanded);
        if (mOnExpandListener != null) {
            mOnExpandListener.onExpand(mIsExpanded);
        }
    }

    void setTitle(final String titleContent) {
        mTitleContent = titleContent;
        refreshState();
    }

    void setIsExpanded(boolean isExpanded) {
        mIsExpanded = isExpanded;
        refreshState();
    }

    void setOnExpandListener(OnExpandListener listener) {
        mOnExpandListener = listener;
    }

    private void refreshState() {
        if (mImageView != null) {
            mImageView.setImageResource(mIsExpanded
                    ? R.drawable.ic_settings_expand_less
                    : R.drawable.ic_settings_expand_more);
        }
        if (mTextView != null) {
            mTextView.setText(mTitleContent);
        }
    }
}
