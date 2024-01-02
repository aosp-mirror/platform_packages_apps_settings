/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.notification.app;

import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.TwoTargetPreference;

import com.google.common.annotations.VisibleForTesting;

public class RecentConversationPreference extends TwoTargetPreference {

    private OnClearClickListener mOnClearClickListener;
    private final Context mContext;
    private View mClearView;

    public interface OnClearClickListener {
        void onClear();
    }

    public RecentConversationPreference(Context context) {
        super(context);
        mContext = context;
    }

    public void setOnClearClickListener(
            OnClearClickListener onClearClickListener) {
        mOnClearClickListener = onClearClickListener;
    }

    @VisibleForTesting
    View getClearView() {
        return mClearView;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_clear;
    }

    @VisibleForTesting
    int getClearId() {
        return R.id.clear_button;
    }

    @VisibleForTesting
    boolean hasClearListener() {
        return mOnClearClickListener != null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final View widgetFrame = view.findViewById(android.R.id.widget_frame);
        widgetFrame.setVisibility(mOnClearClickListener != null ? View.VISIBLE : View.GONE);
        mClearView = view.findViewById(getClearId());
        mClearView.setContentDescription(
                mContext.getString(R.string.clear_conversation, getTitle()));

        mClearView.setOnClickListener(v -> {
            if (mOnClearClickListener != null) {
                mOnClearClickListener.onClear();
            }
        });
    }

}
