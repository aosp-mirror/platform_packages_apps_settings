/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

public class CircularIconsView extends LinearLayout {

    private static final float DISABLED_ITEM_ALPHA = 0.3f;

    record Icons(ImmutableList<Drawable> icons, int extraItems) { }

    private Executor mUiExecutor;
    private int mNumberOfCirclesThatFit;

    // Chronologically, fields will be set top-to-bottom.
    @Nullable private CircularIconSet<?> mIconSet;
    @Nullable private ListenableFuture<List<Drawable>> mPendingLoadIconsFuture;
    @Nullable private Icons mDisplayedIcons;

    public CircularIconsView(Context context) {
        super(context);
        setUiExecutor(context.getMainExecutor());
    }

    public CircularIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUiExecutor(context.getMainExecutor());
    }

    public CircularIconsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUiExecutor(context.getMainExecutor());
    }

    public CircularIconsView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setUiExecutor(context.getMainExecutor());
    }

    @VisibleForTesting
    void setUiExecutor(Executor uiExecutor) {
        mUiExecutor = uiExecutor;
    }

    <T> void setIcons(CircularIconSet<T> iconSet) {
        if (mIconSet != null && mIconSet.equals(iconSet)) {
            return;
        }

        mIconSet = checkNotNull(iconSet);
        cancelPendingTasks();
        if (getMeasuredWidth() != 0) {
            startLoadingIcons(iconSet);
        }
    }

    private void cancelPendingTasks() {
        mDisplayedIcons = null;
        if (mPendingLoadIconsFuture != null) {
            mPendingLoadIconsFuture.cancel(true);
            mPendingLoadIconsFuture = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int numFitting = getNumberOfCirclesThatFit();
        if (mNumberOfCirclesThatFit != numFitting) {
            // View has been measured for the first time OR its dimensions have changed since then.
            // Keep track, because we want to reload stuff if more (or less) items fit.
            mNumberOfCirclesThatFit = numFitting;

            if (mIconSet != null) {
                cancelPendingTasks();
                startLoadingIcons(mIconSet);
            }
        }
    }

    private int getNumberOfCirclesThatFit() {
        Resources res = getContext().getResources();
        int availableSpace = getMeasuredWidth();
        int iconHorizontalSpace = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
        return availableSpace / iconHorizontalSpace;
    }

    private void startLoadingIcons(CircularIconSet<?> iconSet) {
        int numCirclesThatFit = getNumberOfCirclesThatFit();

        List<ListenableFuture<Drawable>> iconFutures;
        int extraItems;
        if (iconSet.size() > numCirclesThatFit) {
            // Reserve one space for the (+xx) textview.
            int numIconsToShow = numCirclesThatFit - 1;
            if (numIconsToShow < 0) {
                numIconsToShow = 0;
            }
            iconFutures = iconSet.getIcons(numIconsToShow);
            extraItems = iconSet.size() - numIconsToShow;
        } else {
            // Fit exactly or with remaining space.
            iconFutures = iconSet.getIcons();
            extraItems = 0;
        }

        // Display icons when all are ready (more consistent than randomly loading).
        mPendingLoadIconsFuture = Futures.allAsList(iconFutures);
        FutureUtil.whenDone(
                mPendingLoadIconsFuture,
                icons -> setDrawables(new Icons(ImmutableList.copyOf(icons), extraItems)),
                mUiExecutor);
    }

    private void setDrawables(Icons icons) {
        mDisplayedIcons = icons;

        // Rearrange child views until we have <numImages> ImageViews...
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int numImages = icons.icons.size();
        int numImageViews = getChildCount(ImageView.class);
        if (numImages > numImageViews) {
            for (int i = 0; i < numImages - numImageViews; i++) {
                ImageView imageView = (ImageView) inflater.inflate(
                        R.layout.preference_circular_icons_item, this, false);
                addView(imageView, 0);
            }
        } else if (numImageViews > numImages) {
            for (int i = 0; i < numImageViews - numImages; i++) {
                removeViewAt(0);
            }
        }
        // ... plus 0/1 TextViews at the end.
        if (icons.extraItems > 0 && !(getLastChild() instanceof TextView)) {
            TextView plusView = (TextView) inflater.inflate(
                    R.layout.preference_circular_icons_plus_item, this, false);
            this.addView(plusView);
        } else if (icons.extraItems == 0 && (getLastChild() instanceof TextView)) {
            removeViewAt(getChildCount() - 1);
        }

        // Show images (and +n if needed).
        for (int i = 0; i < numImages; i++) {
            ImageView imageView = (ImageView) getChildAt(i);
            imageView.setImageDrawable(icons.icons.get(i));
        }
        if (icons.extraItems > 0) {
            TextView textView = (TextView) checkNotNull(getLastChild());
            textView.setText(getContext().getString(R.string.zen_mode_plus_n_items,
                    icons.extraItems));
        }

        applyEnabledDisabledAppearance(isEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        applyEnabledDisabledAppearance(isEnabled());
    }

    private void applyEnabledDisabledAppearance(boolean enabled) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setAlpha(enabled ? 1.0f : DISABLED_ITEM_ALPHA);
        }
    }

    private int getChildCount(Class<? extends View> childClass) {
        int count = 0;
        for (int i = 0; i < getChildCount(); i++) {
            if (childClass.isInstance(getChildAt(i))) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    private View getLastChild() {
        if (getChildCount() == 0) {
            return null;
        }
        return getChildAt(getChildCount() - 1);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    Icons getDisplayedIcons() {
        return mDisplayedIcons;
    }
}
