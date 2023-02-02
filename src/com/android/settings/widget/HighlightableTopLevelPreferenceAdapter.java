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
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.window.embedding.ActivityEmbeddingController;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.homepage.SettingsHomepageActivity;

/**
 *  Adapter for highlighting top level preferences
 */
public class HighlightableTopLevelPreferenceAdapter extends PreferenceGroupAdapter implements
        SettingsHomepageActivity.HomepageLoadedListener {

    private static final String TAG = "HighlightableTopLevelAdapter";

    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 100L;
    private static final int RES_NORMAL_BACKGROUND =
            R.drawable.homepage_selectable_item_background;
    private static final int RES_HIGHLIGHTED_BACKGROUND =
            R.drawable.homepage_highlighted_item_background;

    private final int mTitleColorNormal;
    private final int mTitleColorHighlight;
    private final int mSummaryColorNormal;
    private final int mSummaryColorHighlight;
    private final int mIconColorNormal;
    private final int mIconColorHighlight;

    private final SettingsHomepageActivity mHomepageActivity;
    private final RecyclerView mRecyclerView;
    private String mHighlightKey;
    private int mHighlightPosition = RecyclerView.NO_POSITION;
    private int mScrollPosition = RecyclerView.NO_POSITION;
    private boolean mHighlightNeeded;
    private boolean mScrolled;
    private SparseArray<PreferenceViewHolder> mViewHolders;

    public HighlightableTopLevelPreferenceAdapter(SettingsHomepageActivity homepageActivity,
            PreferenceGroup preferenceGroup, RecyclerView recyclerView, String key,
            boolean scrollNeeded) {
        super(preferenceGroup);
        mRecyclerView = recyclerView;
        mHighlightKey = key;
        mScrolled = !scrollNeeded;
        mViewHolders = new SparseArray<>();
        mHomepageActivity = homepageActivity;
        Context context = preferenceGroup.getContext();
        mTitleColorNormal = Utils.getColorAttrDefaultColor(context,
                android.R.attr.textColorPrimary);
        mTitleColorHighlight = context.getColor(R.color.accent_select_primary_text);
        mSummaryColorNormal = Utils.getColorAttrDefaultColor(context,
                android.R.attr.textColorSecondary);
        mSummaryColorHighlight = context.getColor(R.color.accent_select_secondary_text);
        mIconColorNormal = Utils.getHomepageIconColor(context);
        mIconColorHighlight = Utils.getHomepageIconColorHighlight(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        mViewHolders.put(position, holder);
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
        if (mRecyclerView == null) {
            return;
        }

        final int previousPosition = mHighlightPosition;
        if (TextUtils.isEmpty(mHighlightKey)) {
            // De-highlight previous preference.
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

        // Scroll before highlight if needed.
        final boolean highlightNeeded = isHighlightNeeded();
        if (highlightNeeded) {
            mScrollPosition = position;
            scroll();
        }

        // Turn on/off highlight when screen split mode is changed.
        if (highlightNeeded != mHighlightNeeded) {
            Log.d(TAG, "Highlight needed change: " + highlightNeeded);
            mHighlightNeeded = highlightNeeded;
            mHighlightPosition = position;
            notifyItemChanged(position);
            if (!highlightNeeded) {
                // De-highlight to prevent a flicker
                removeHighlightAt(previousPosition);
            }
            return;
        }

        if (position == mHighlightPosition) {
            return;
        }

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
        mHighlightKey = key;
        mScrolled = !scrollNeeded;
        requestHighlight();
    }

    @Override
    public void onHomepageLoaded() {
        scroll();
    }

    private void scroll() {
        if (mScrolled || mScrollPosition < 0) {
            return;
        }

        if (mHomepageActivity.addHomepageLoadedListener(this)) {
            return;
        }

        // Only when the recyclerView is loaded, it can be scrolled
        final View view = mRecyclerView.getChildAt(mScrollPosition);
        if (view == null) {
            mRecyclerView.postDelayed(() -> scroll(), DELAY_HIGHLIGHT_DURATION_MILLIS);
            return;
        }

        mScrolled = true;
        Log.d(TAG, "Scroll to position " + mScrollPosition);
        // Scroll to the top to reset the position.
        mRecyclerView.nestedScrollBy(0, -mRecyclerView.getHeight());

        final int scrollY = view.getTop();
        if (scrollY > 0) {
            mRecyclerView.nestedScrollBy(0, scrollY);
        }
    }

    private void removeHighlightAt(int position) {
        if (position >= 0) {
            // De-highlight the existing preference view holder at an early stage
            final PreferenceViewHolder holder = mViewHolders.get(position);
            if (holder != null) {
                removeHighlightBackground(holder);
            }
            notifyItemChanged(position);
        }
    }

    private void addHighlightBackground(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.setBackgroundResource(RES_HIGHLIGHTED_BACKGROUND);
        ((TextView) v.findViewById(android.R.id.title)).setTextColor(mTitleColorHighlight);
        ((TextView) v.findViewById(android.R.id.summary)).setTextColor(mSummaryColorHighlight);
        final Drawable drawable = ((ImageView) v.findViewById(android.R.id.icon)).getDrawable();
        if (drawable != null) {
            drawable.setTint(mIconColorHighlight);
        }
    }

    private void removeHighlightBackground(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.setBackgroundResource(RES_NORMAL_BACKGROUND);
        ((TextView) v.findViewById(android.R.id.title)).setTextColor(mTitleColorNormal);
        ((TextView) v.findViewById(android.R.id.summary)).setTextColor(mSummaryColorNormal);
        final Drawable drawable = ((ImageView) v.findViewById(android.R.id.icon)).getDrawable();
        if (drawable != null) {
            drawable.setTint(mIconColorNormal);
        }
    }

    private boolean isHighlightNeeded() {
        return ActivityEmbeddingController.getInstance(mHomepageActivity)
                .isActivityEmbedded(mHomepageActivity);
    }
}
