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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.material.appbar.AppBarLayout;

public class HighlightablePreferenceGroupAdapter extends SettingsPreferenceGroupAdapter {

    private static final String TAG = "HighlightableAdapter";
    @VisibleForTesting static final long DELAY_COLLAPSE_DURATION_MILLIS = 300L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y = 300L;
    private static final long HIGHLIGHT_DURATION = 15000L;
    private static final long HIGHLIGHT_FADE_OUT_DURATION = 500L;
    private static final long HIGHLIGHT_FADE_IN_DURATION = 200L;

    @VisibleForTesting @DrawableRes final int mHighlightBackgroundRes;
    @VisibleForTesting boolean mFadeInAnimated;

    private final Context mContext;
    private final @DrawableRes int mNormalBackgroundRes;
    private final @Nullable String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    /**
     * Tries to override initial expanded child count.
     *
     * <p>Initial expanded child count will be ignored if: 1. fragment contains request to highlight
     * a particular row. 2. count value is invalid.
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

    public HighlightablePreferenceGroupAdapter(
            @NonNull PreferenceGroup preferenceGroup,
            @Nullable String key,
            boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        mContext = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        mNormalBackgroundRes = R.drawable.preference_background;
        mHighlightBackgroundRes = R.drawable.preference_background_highlighted;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        Preference preference = getItem(position);
        if (preference != null
                && position == mHighlightPosition
                && (mHighlightKey != null && TextUtils.equals(mHighlightKey, preference.getKey()))
                && v.isShown()) {
            // This position should be highlighted. If it's highlighted before - skip animation.
            v.requestAccessibilityFocus();
            addHighlightBackground(holder, !mFadeInAnimated, position);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // View with highlight is reused for a view that should not have highlight
            removeHighlightBackground(holder, false /* animate */, position);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view. note: Before highlighting a
     * setting, screen collapses tool bar with an animation.
     */
    public void requestHighlight(View root, RecyclerView recyclerView, AppBarLayout appBarLayout) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Highlight request accepted
        mHighlightRequested = true;
        // Collapse app bar after 300 milliseconds.
        if (appBarLayout != null) {
            root.postDelayed(
                    () -> appBarLayout.setExpanded(false, true),
                    DELAY_COLLAPSE_DURATION_MILLIS);
        }

        // Remove the animator as early as possible to avoid a RecyclerView crash.
        recyclerView.setItemAnimator(null);
        // Scroll to correct position after a short delay.
        root.postDelayed(
                () -> {
                    if (ensureHighlightPosition()) {
                        recyclerView.smoothScrollToPosition(mHighlightPosition);
                        highlightAndFocusTargetItem(recyclerView, mHighlightPosition);
                    }
                },
                AccessibilityUtil.isTouchExploreEnabled(mContext)
                        ? DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y
                        : DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    private void highlightAndFocusTargetItem(RecyclerView recyclerView, int highlightPosition) {
        ViewHolder target = recyclerView.findViewHolderForAdapterPosition(highlightPosition);
        if (target != null) { // view already visible
            notifyItemChanged(mHighlightPosition);
            target.itemView.requestFocus();
        } else { // otherwise we're about to scroll to that view (but we might not be scrolling yet)
            recyclerView.addOnScrollListener(
                    new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(
                                @NonNull RecyclerView recyclerView, int newState) {
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                notifyItemChanged(mHighlightPosition);
                                ViewHolder target =
                                        recyclerView.findViewHolderForAdapterPosition(
                                                highlightPosition);
                                if (target != null) {
                                    target.itemView.requestFocus();
                                }
                                recyclerView.removeOnScrollListener(this);
                            }
                        }
                    });
        }
    }

    /**
     * Make sure we highlight the real-wanted position in case of preference position already
     * changed when the delay time comes.
     */
    private boolean ensureHighlightPosition() {
        if (TextUtils.isEmpty(mHighlightKey)) {
            return false;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        final boolean allowHighlight = position >= 0;
        if (allowHighlight && mHighlightPosition != position) {
            Log.w(TAG, "EnsureHighlight: position has changed since last highlight request");
            // Make sure RecyclerView always uses latest correct position to avoid exceptions.
            mHighlightPosition = position;
        }
        return allowHighlight;
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(PreferenceViewHolder holder, int position) {
        final View v = holder.itemView;
        v.postDelayed(
                () -> {
                    mHighlightPosition = RecyclerView.NO_POSITION;
                    removeHighlightBackground(holder, true /* animate */, position);
                },
                HIGHLIGHT_DURATION);
    }

    private void addHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        v.setTag(R.id.preference_highlighted, true);
        final int backgroundFrom = getBackgroundRes(position, false);
        final int backgroundTo = getBackgroundRes(position, true);

        if (!animate) {
            v.setBackgroundResource(backgroundTo);
            Log.d(TAG, "AddHighlight: Not animation requested - setting highlight background");
            requestRemoveHighlightDelayed(holder, position);
            return;
        }
        mFadeInAnimated = true;

        // TODO(b/377561018): Fix fade-in animation
        final ValueAnimator fadeInLoop =
                ValueAnimator.ofObject(new ArgbEvaluator(), backgroundFrom, backgroundTo);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.addUpdateListener(
                animator -> v.setBackgroundResource((int) animator.getAnimatedValue()));
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        fadeInLoop.start();
        Log.d(TAG, "AddHighlight: starting fade in animation");
        holder.setIsRecyclable(false);
        requestRemoveHighlightDelayed(holder, position);
    }

    private void removeHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        int backgroundFrom = getBackgroundRes(position, true);
        int backgroundTo = getBackgroundRes(position, false);

        if (!animate) {
            v.setTag(R.id.preference_highlighted, false);
            v.setBackgroundResource(backgroundTo);
            Log.d(TAG, "RemoveHighlight: No animation requested - setting normal background");
            return;
        }

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // Not highlighted, no-op
            Log.d(TAG, "RemoveHighlight: Not highlighted - skipping");
            return;
        }

        v.setTag(R.id.preference_highlighted, false);
        // TODO(b/377561018): Fix fade-out animation
        final ValueAnimator colorAnimation =
                ValueAnimator.ofObject(new ArgbEvaluator(), backgroundFrom, backgroundTo);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        colorAnimation.addUpdateListener(
                animator -> v.setBackgroundResource((int) animator.getAnimatedValue()));
        colorAnimation.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        // Animation complete - the background needs to be the target background.
                        v.setBackgroundResource(backgroundTo);
                        holder.setIsRecyclable(true);
                    }
                });
        colorAnimation.start();
        Log.d(TAG, "Starting fade out animation");
    }

    private @DrawableRes int getBackgroundRes(int position, boolean isHighlighted) {
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            Log.d(TAG, "[Expressive Theme] get rounded background, highlight = " + isHighlighted);
            return getRoundCornerDrawableRes(position, false, isHighlighted);
        } else {
            return (isHighlighted) ? mHighlightBackgroundRes : mNormalBackgroundRes;
        }
    }
}
