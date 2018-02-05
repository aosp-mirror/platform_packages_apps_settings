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

package com.android.settings.widget;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;

import com.android.settings.R;

public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

    @VisibleForTesting
    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    private static final long HIGHLIGHT_DURATION = 5000L;

    private final int mHighlightColor;
    private final int mNormalBackgroundRes;
    private final String mHighlightKey;

    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup, String key,
            boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        final Context context = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true /* resolveRefs */);
        mNormalBackgroundRes = outValue.resourceId;
        mHighlightColor = context.getColor(R.color.preference_highligh_color);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        if (position == mHighlightPosition) {
            v.setBackgroundColor(mHighlightColor);
            v.setTag(R.id.preference_highlighted, true);
            v.postDelayed(() -> {
                mHighlightPosition = RecyclerView.NO_POSITION;
                removeHighlightBackground(v);
            }, HIGHLIGHT_DURATION);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            removeHighlightBackground(v);
        }
    }

    public void requestHighlight(View root, RecyclerView recyclerView) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        root.postDelayed(() -> {
            final int position = getPreferenceAdapterPosition(mHighlightKey);
            if (position < 0) {
                return;
            }
            mHighlightRequested = true;
            recyclerView.getLayoutManager().scrollToPosition(position);
            mHighlightPosition = position;
            notifyItemChanged(position);
        }, DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    private void removeHighlightBackground(View v) {
        v.setBackgroundResource(mNormalBackgroundRes);
        v.setTag(R.id.preference_highlighted, false);
    }
}
