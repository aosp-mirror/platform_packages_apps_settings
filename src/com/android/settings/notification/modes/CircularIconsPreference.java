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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

public class CircularIconsPreference extends RestrictedPreference {

    private static final float DISABLED_ITEM_ALPHA = 0.3f;

    record LoadedIcons(ImmutableList<Drawable> icons, int extraItems) { }

    private Executor mUiExecutor;

    // Chronologically, fields will be set top-to-bottom.
    @Nullable private CircularIconSet<?> mIconSet;
    @Nullable private ListenableFuture<List<Drawable>> mPendingLoadIconsFuture;
    @Nullable private LoadedIcons mLoadedIcons;

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

    <T> void displayIcons(CircularIconSet<T> iconSet) {
        displayIcons(iconSet, null);
    }

    <T> void displayIcons(CircularIconSet<T> iconSet, @Nullable Equivalence<T> itemEquivalence) {
        if (mIconSet != null && mIconSet.hasSameItemsAs(iconSet, itemEquivalence)) {
            return;
        }
        mIconSet = iconSet;

        mLoadedIcons = null;
        if (mPendingLoadIconsFuture != null) {
            mPendingLoadIconsFuture.cancel(true);
            mPendingLoadIconsFuture = null;
        }

        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        LinearLayout iconContainer = checkNotNull(
                (LinearLayout) holder.findViewById(R.id.circles_container));
        bindIconContainer(iconContainer);
    }

    private void bindIconContainer(LinearLayout container) {
        if (mLoadedIcons != null) {
            // We have the icons ready to display already, show them.
            setDrawables(container, mLoadedIcons);
        } else if (mIconSet != null) {
            // We know what icons we want, but haven't yet loaded them.
            if (mIconSet.size() == 0) {
                container.setVisibility(View.GONE);
                return;
            }
            container.setVisibility(View.VISIBLE);
            if (container.getMeasuredWidth() != 0) {
                startLoadingIcons(container, mIconSet);
            } else {
                container.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                startLoadingIcons(container, mIconSet);
                            }
                        }
                );
            }
        }
    }

    private void startLoadingIcons(LinearLayout container, CircularIconSet<?> iconSet) {
        Resources res = getContext().getResources();
        int availableSpace = container.getMeasuredWidth();
        int iconHorizontalSpace = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
        int numIconsThatFit = availableSpace / iconHorizontalSpace;

        List<ListenableFuture<Drawable>> iconFutures;
        int extraItems;
        if (iconSet.size() > numIconsThatFit) {
            // Reserve one space for the (+xx) textview.
            int numIconsToShow = numIconsThatFit - 1;
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
                icons -> {
                    mLoadedIcons = new LoadedIcons(ImmutableList.copyOf(icons), extraItems);
                    notifyChanged(); // So that view is rebound and icons actually shown.
                },
                mUiExecutor);
    }

    private void setDrawables(LinearLayout container, LoadedIcons loadedIcons) {
        // Rearrange child views until we have <numImages> ImageViews...
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int numImages = loadedIcons.icons.size();
        int numImageViews = getChildCount(container, ImageView.class);
        if (numImages > numImageViews) {
            for (int i = 0; i < numImages - numImageViews; i++) {
                ImageView imageView = (ImageView) inflater.inflate(
                        R.layout.preference_circular_icons_item, container, false);
                container.addView(imageView, 0);
            }
        } else if (numImageViews > numImages) {
            for (int i = 0; i < numImageViews - numImages; i++) {
                container.removeViewAt(0);
            }
        }
        // ... plus 0/1 TextViews at the end.
        if (loadedIcons.extraItems > 0 && !(getLastChild(container) instanceof TextView)) {
            TextView plusView = (TextView) inflater.inflate(
                    R.layout.preference_circular_icons_plus_item, container, false);
            container.addView(plusView);
        } else if (loadedIcons.extraItems == 0 && (getLastChild(container) instanceof TextView)) {
            container.removeViewAt(container.getChildCount() - 1);
        }

        // Show images (and +n if needed).
        for (int i = 0; i < numImages; i++) {
            ImageView imageView = (ImageView) container.getChildAt(i);
            imageView.setImageDrawable(loadedIcons.icons.get(i));
        }
        if (loadedIcons.extraItems > 0) {
            TextView textView = (TextView) checkNotNull(getLastChild(container));
            textView.setText(getContext().getString(R.string.zen_mode_plus_n_items,
                    loadedIcons.extraItems));
        }

        // Apply enabled/disabled style.
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setAlpha(isEnabled() ? 1.0f : DISABLED_ITEM_ALPHA);
        }
    }

    private static int getChildCount(ViewGroup parent, Class<? extends View> childClass) {
        int count = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (childClass.isInstance(parent.getChildAt(i))) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    private static View getLastChild(ViewGroup parent) {
        if (parent.getChildCount() == 0) {
            return null;
        }
        return parent.getChildAt(parent.getChildCount() - 1);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    LoadedIcons getLoadedIcons() {
        return mLoadedIcons;
    }
}
