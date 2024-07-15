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
import static com.google.common.base.Preconditions.checkState;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

public class CircularIconsPreference extends RestrictedPreference {

    private Executor mUiExecutor;
    @Nullable private LinearLayout mIconContainer;

    @Nullable private CircularIconSet<?> mPendingIconSet;
    @Nullable private ListenableFuture<?> mPendingLoadIconsFuture;

    public CircularIconsPreference(Context context) {
        super(context);
        init(context);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CircularIconsPreference(Context context, Executor uiExecutor) {
        this(context);
        mUiExecutor = uiExecutor;
    }

    public CircularIconsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularIconsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CircularIconsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mUiExecutor = context.getMainExecutor();
        setLayoutResource(R.layout.preference_circular_icons);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mIconContainer = checkNotNull((LinearLayout) holder.findViewById(R.id.circles_container));
        displayIconsIfPending();
    }

    private void displayIconsIfPending() {
        CircularIconSet<?> pendingIconSet = mPendingIconSet;
        if (pendingIconSet != null) {
            mPendingIconSet = null;
            displayIcons(pendingIconSet);
        }
    }

    void displayIcons(CircularIconSet<?> iconSet) {
        if (mIconContainer == null) {
            // Too soon, wait for bind.
            mPendingIconSet = iconSet;
            return;
        }
        mIconContainer.setVisibility(iconSet.size() != 0 ? View.VISIBLE : View.GONE);
        if (iconSet.size() == 0) {
            return;
        }
        if (mIconContainer.getMeasuredWidth() == 0) {
            // Too soon, wait for first measure to know width.
            mPendingIconSet = iconSet;
            ViewTreeObserver vto = mIconContainer.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(() ->
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            vto.removeOnGlobalLayoutListener(this);
                            displayIconsIfPending();
                        }
                    });
            return;
        }

        mIconContainer.setVisibility(View.VISIBLE);
        Resources res = getContext().getResources();
        int availableSpace = mIconContainer.getMeasuredWidth();
        int iconHorizontalSpace = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_size)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
        int numIconsThatFit = availableSpace / iconHorizontalSpace;

        List<ListenableFuture<Drawable>> iconFutures;
        int extraItems = 0;
        if (iconSet.size() > numIconsThatFit) {
            // Reserve one space for the (+xx) circle.
            int numIconsToShow = numIconsThatFit - 1;
            if (numIconsToShow < 0) {
                numIconsToShow = 0;
            }
            iconFutures = iconSet.getIcons(numIconsToShow);
            extraItems = iconSet.size() - numIconsToShow;
        } else {
            // Fit exactly or with remaining space.
            iconFutures = iconSet.getIcons();
        }

        displayIconsWhenReady(iconFutures, extraItems);
    }

    private void displayIconsWhenReady(List<ListenableFuture<Drawable>> iconFutures,
            int extraItems) {
        checkState(mIconContainer != null);
        if (mPendingLoadIconsFuture != null) {
            mPendingLoadIconsFuture.cancel(true);
        }

        int numCircles = iconFutures.size() + (extraItems > 0 ? 1 : 0);
        if (mIconContainer.getChildCount() > numCircles) {
            mIconContainer.removeViews(numCircles, mIconContainer.getChildCount() - numCircles);
        }
        for (int i = mIconContainer.getChildCount(); i < numCircles; i++) {
            ImageView imageView = (ImageView) LayoutInflater.from(getContext()).inflate(
                    R.layout.preference_circular_icons_item, mIconContainer, false);
            mIconContainer.addView(imageView);
        }

        // Set up placeholders and extra items indicator.
        for (int i = 0; i < iconFutures.size(); i++) {
            ImageView imageView = (ImageView) mIconContainer.getChildAt(i);
            // TODO: b/346551087 - proper color and shape, should be a gray circle.
            imageView.setImageDrawable(new ColorDrawable(Color.RED));
        }
        if (extraItems > 0) {
            ImageView imageView = (ImageView) mIconContainer.getChildAt(
                    mIconContainer.getChildCount() - 1);
            // TODO: b/346551087 - proper color and shape and number.
            imageView.setImageDrawable(new ColorDrawable(Color.BLUE));
        }

        // Display icons when all are ready (more consistent than randomly loading).
        mPendingLoadIconsFuture = Futures.allAsList(iconFutures);
        FutureUtil.whenDone(
                Futures.allAsList(iconFutures),
                icons -> {
                    checkState(mIconContainer != null);
                    for (int i = 0; i < icons.size(); i++) {
                        ((ImageView) mIconContainer.getChildAt(i)).setImageDrawable(icons.get(i));
                    }
                },
                mUiExecutor);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ImmutableList<ImageView> getIconViews() {
        if (mIconContainer == null) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<ImageView> imageViews = new ImmutableList.Builder<>();
        for (int i = 0; i < mIconContainer.getChildCount(); i++) {
            imageViews.add((ImageView) mIconContainer.getChildAt(i));
        }
        return imageViews.build();
    }
}
