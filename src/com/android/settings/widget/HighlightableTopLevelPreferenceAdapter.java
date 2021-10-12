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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.Utils;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;

/**
 *  Adapter for highlighting top level preferences
 */
public class HighlightableTopLevelPreferenceAdapter extends PreferenceGroupAdapter {

    private static final String TAG = "HighlightableTopLevelAdapter";

    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 100L;

    @VisibleForTesting
    final int mHighlightColor;
    final int mTitleColorNormal;
    final int mTitleColorHighlight;
    final int mSummaryColorNormal;
    final int mSummaryColorHighlight;
    final int mIconColorNormal;
    final int mIconColorHighlight;

    private final Context mContext;
    private final RecyclerView mRecyclerView;
    private final int mNormalBackgroundRes;
    private String mHighlightKey;
    private String mPreviousHighlightKey;
    private int mHighlightPosition = RecyclerView.NO_POSITION;
    private boolean mHighlightNeeded;
    private boolean mScrolled;

    public HighlightableTopLevelPreferenceAdapter(PreferenceGroup preferenceGroup,
            RecyclerView recyclerView, String key) {
        super(preferenceGroup);
        mRecyclerView = recyclerView;
        mHighlightKey = key;
        mContext = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true /* resolveRefs */);
        mNormalBackgroundRes = outValue.resourceId;
        mHighlightColor = Utils.getColorAttrDefaultColor(mContext,
                com.android.internal.R.attr.colorAccentSecondaryVariant);
        mTitleColorNormal = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorPrimary);
        mTitleColorHighlight = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorPrimaryInverse);
        mSummaryColorNormal = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorSecondary);
        mSummaryColorHighlight = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorSecondaryInverse);
        mIconColorNormal = Utils.getHomepageIconColor(mContext);
        mIconColorHighlight = Utils.getHomepageIconColorHighlight(mContext);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        if (!isHighlightNeeded()) {
            removeHighlightBackground(holder);
            return;
        }

        if (position == mHighlightPosition
                && mHighlightKey != null
                && TextUtils.equals(mHighlightKey, getItem(position).getKey())) {
            // This position should be highlighted.
            addHighlightBackground(holder);
        } else {
            removeHighlightBackground(holder);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view.
     */
    public void requestHighlight() {
        if (mRecyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }

        if (TextUtils.isEmpty(mHighlightKey)) {
            // De-highlight previous preference.
            final int previousPosition = mHighlightPosition;
            mHighlightPosition = RecyclerView.NO_POSITION;
            mScrolled = true;
            if (previousPosition >= 0) {
                notifyItemChanged(previousPosition);
            }
            return;
        }

        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        final boolean highlightNeeded = isHighlightNeeded();
        if (highlightNeeded) {
            scrollToPositionIfNeeded(position);
        }

        // Turn on/off highlight when screen split mode is changed.
        if (highlightNeeded != mHighlightNeeded) {
            Log.d(TAG, "Highlight change needed: " + highlightNeeded);
            mHighlightNeeded = highlightNeeded;
            mHighlightPosition = position;
            notifyItemChanged(position);
            return;
        }

        if (position == mHighlightPosition) {
            return;
        }

        final int previousPosition = mHighlightPosition;
        mHighlightPosition = position;
        Log.d(TAG, "Request highlight position " + position);
        Log.d(TAG, "Is highlight needed: " + highlightNeeded);
        if (!highlightNeeded) {
            return;
        }

        // Highlight preference.
        notifyItemChanged(position);

        // De-highlight previous preference.
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
    }

    /**
     * A function that highlights a setting by specifying a preference key. Usually used whenever a
     * preference is clicked.
     */
    public void highlightPreference(String key, boolean scrollNeeded) {
        mPreviousHighlightKey = mHighlightKey;
        mHighlightKey = key;
        mScrolled = !scrollNeeded;
        requestHighlight();
    }

    /**
     * A function that restores the previous highlighted setting.
     */
    public void restorePreviousHighlight() {
        mHighlightKey = mPreviousHighlightKey;
        requestHighlight();
    }

    private void scrollToPositionIfNeeded(int position) {
        if (mScrolled || position < 0) {
            return;
        }

        // Only when the recyclerView is loaded, it can be scrolled
        final View view = mRecyclerView.getChildAt(position);
        if (view == null) {
            mRecyclerView.postDelayed(() -> scrollToPositionIfNeeded(position),
                    DELAY_HIGHLIGHT_DURATION_MILLIS);
            return;
        }

        mScrolled = true;
        Log.d(TAG, "Scroll to position " + position);
        // Scroll to the top to reset the position.
        mRecyclerView.nestedScrollBy(0, -mRecyclerView.getHeight());

        final int scrollY = view.getTop();
        if (scrollY > 0) {
            mRecyclerView.nestedScrollBy(0, scrollY);
        }
    }

    private void addHighlightBackground(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.setBackgroundColor(mHighlightColor);
        ((TextView) v.findViewById(android.R.id.title)).setTextColor(mTitleColorHighlight);
        ((TextView) v.findViewById(android.R.id.summary)).setTextColor(mSummaryColorHighlight);
        final Drawable drawable = ((ImageView) v.findViewById(android.R.id.icon)).getDrawable();
        if (drawable != null) {
            drawable.setTint(mIconColorHighlight);
        }
    }

    private void removeHighlightBackground(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.setBackgroundResource(mNormalBackgroundRes);
        ((TextView) v.findViewById(android.R.id.title)).setTextColor(mTitleColorNormal);
        ((TextView) v.findViewById(android.R.id.summary)).setTextColor(mSummaryColorNormal);
        final Drawable drawable = ((ImageView) v.findViewById(android.R.id.icon)).getDrawable();
        if (drawable != null) {
            drawable.setTint(mIconColorNormal);
        }
    }

    private boolean isHighlightNeeded() {
        return ActivityEmbeddingUtils.isTwoPaneResolution(mContext);
    }
}
