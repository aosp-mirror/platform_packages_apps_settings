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

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.google.android.material.appbar.AppBarLayout;

public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

    private static final String TAG = "HighlightableAdapter";
    @VisibleForTesting
    static final long DELAY_COLLAPSE_DURATION_MILLIS = 300L;
    @VisibleForTesting
    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    private static final long HIGHLIGHT_DURATION = 15000L;
    private static final long HIGHLIGHT_FADE_OUT_DURATION = 500L;
    private static final long HIGHLIGHT_FADE_IN_DURATION = 200L;

    @VisibleForTesting
    final int mHighlightColor;
    @VisibleForTesting
    boolean mFadeInAnimated;

    private final int mNormalBackgroundRes;
    private final String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;


    /**
     * Tries to override initial expanded child count.
     * <p/>
     * Initial expanded child count will be ignored if:
     * 1. fragment contains request to highlight a particular row.
     * 2. count value is invalid.
     */
    public static void adjustInitialExpandedChildCount(SettingsPreferenceFragment host) {
        if (host == null) {
            return;
        }
        final PreferenceScreen screen = host.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final Bundle arguments = host.getArguments();
        if (arguments != null) {
            final String highlightKey = arguments.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(highlightKey)) {
                // Has highlight row - expand everything
                screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
                return;
            }
        }

        final int initialCount = host.getInitialExpandedChildCount();
        if (initialCount <= 0) {
            return;
        }
        screen.setInitialExpandedChildrenCount(initialCount);
    }

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
        mHighlightColor = context.getColor(R.color.preference_highlight_color);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        if (position == mHighlightPosition
                && (mHighlightKey != null
                && TextUtils.equals(mHighlightKey, getItem(position).getKey()))) {
            // This position should be highlighted. If it's highlighted before - skip animation.
            addHighlightBackground(holder, !mFadeInAnimated);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // View with highlight is reused for a view that should not have highlight
            removeHighlightBackground(holder, false /* animate */);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view.
     * note: Before highlighting a setting, screen collapses tool bar with an animation.
     */
    public void requestHighlight(View root, RecyclerView recyclerView, AppBarLayout appBarLayout) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Collapse app bar after 300 milliseconds.
        if (appBarLayout != null) {
            root.postDelayed(() -> {
                appBarLayout.setExpanded(false, true);
            }, DELAY_COLLAPSE_DURATION_MILLIS);
        }

        // Remove the animator as early as possible to avoid a RecyclerView crash.
        recyclerView.setItemAnimator(null);
        // Scroll to correct position after 600 milliseconds.
        root.postDelayed(() -> {
            mHighlightRequested = true;
            recyclerView.smoothScrollToPosition(position);
            mHighlightPosition = position;
        }, DELAY_HIGHLIGHT_DURATION_MILLIS);

        // Highlight preference after 900 milliseconds.
        root.postDelayed(() -> {
            notifyItemChanged(position);
        }, DELAY_COLLAPSE_DURATION_MILLIS + DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.postDelayed(() -> {
            mHighlightPosition = RecyclerView.NO_POSITION;
            removeHighlightBackground(holder, true /* animate */);
        }, HIGHLIGHT_DURATION);
    }

    private void addHighlightBackground(PreferenceViewHolder holder, boolean animate) {
        final View v = holder.itemView;
        v.setTag(R.id.preference_highlighted, true);
        if (!animate) {
            v.setBackgroundColor(mHighlightColor);
            Log.d(TAG, "AddHighlight: Not animation requested - setting highlight background");
            requestRemoveHighlightDelayed(holder);
            return;
        }
        mFadeInAnimated = true;
        final int colorFrom = mNormalBackgroundRes;
        final int colorTo = mHighlightColor;
        final ValueAnimator fadeInLoop = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorFrom, colorTo);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        fadeInLoop.start();
        Log.d(TAG, "AddHighlight: starting fade in animation");
        holder.setIsRecyclable(false);
        requestRemoveHighlightDelayed(holder);
    }

    private void removeHighlightBackground(PreferenceViewHolder holder, boolean animate) {
        final View v = holder.itemView;
        if (!animate) {
            v.setTag(R.id.preference_highlighted, false);
            v.setBackgroundResource(mNormalBackgroundRes);
            Log.d(TAG, "RemoveHighlight: No animation requested - setting normal background");
            return;
        }

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // Not highlighted, no-op
            Log.d(TAG, "RemoveHighlight: Not highlighted - skipping");
            return;
        }
        int colorFrom = mHighlightColor;
        int colorTo = mNormalBackgroundRes;

        v.setTag(R.id.preference_highlighted, false);
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        colorAnimation.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Animation complete - the background is now white. Change to mNormalBackgroundRes
                // so it is white and has ripple on touch.
                v.setBackgroundResource(mNormalBackgroundRes);
                holder.setIsRecyclable(true);
            }
        });
        colorAnimation.start();
        Log.d(TAG, "Starting fade out animation");
    }
}
