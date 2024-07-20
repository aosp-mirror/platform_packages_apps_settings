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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
import com.android.settingslib.Utils;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class CircularIconsPreference extends RestrictedPreference {

    private Executor mUiExecutor;
    @Nullable private LinearLayout mIconContainer;

    @Nullable private CircularIconSet<?> mIconSet;
    @Nullable private CircularIconSet<?> mPendingDisplayIconSet;
    @Nullable private ListenableFuture<List<Drawable>> mPendingLoadIconsFuture;

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
        CircularIconSet<?> pendingIconSet = mPendingDisplayIconSet;
        if (pendingIconSet != null) {
            mPendingDisplayIconSet = null;
            displayIconsInternal(pendingIconSet);
        }
    }

    void displayIcons(CircularIconSet<?> iconSet) {
        if (mIconSet != null && mIconSet.hasSameItemsAs(iconSet)) {
            return;
        }
        mIconSet = iconSet;
        displayIconsInternal(iconSet);
    }

    void displayIconsInternal(CircularIconSet<?> iconSet) {
        if (mIconContainer == null) {
            // Too soon, wait for bind.
            mPendingDisplayIconSet = iconSet;
            return;
        }
        mIconContainer.setVisibility(iconSet.size() != 0 ? View.VISIBLE : View.GONE);
        if (iconSet.size() == 0) {
            return;
        }
        if (mIconContainer.getMeasuredWidth() == 0) {
            // Too soon, wait for first measure to know width.
            mPendingDisplayIconSet = iconSet;
            mIconContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            checkNotNull(mIconContainer).getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            displayIconsIfPending();
                        }
                    }
            );
            return;
        }

        mIconContainer.setVisibility(View.VISIBLE);
        Resources res = getContext().getResources();
        int availableSpace = mIconContainer.getMeasuredWidth();
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

        displayIconsWhenReady(iconFutures, extraItems);
    }

    private void displayIconsWhenReady(List<ListenableFuture<Drawable>> iconFutures,
            int extraItems) {
        checkState(mIconContainer != null);
        if (mPendingLoadIconsFuture != null) {
            mPendingLoadIconsFuture.cancel(true);
        }

        // Rearrange child views until we have <numImages> ImageViews...
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int numImages = iconFutures.size();
        int numImageViews = getChildCount(mIconContainer, ImageView.class);
        if (numImages > numImageViews) {
            for (int i = 0; i < numImages - numImageViews; i++) {
                ImageView imageView = (ImageView) inflater.inflate(
                        R.layout.preference_circular_icons_item, mIconContainer, false);
                mIconContainer.addView(imageView, 0);
            }
        } else if (numImageViews > numImages) {
            for (int i = 0; i < numImageViews - numImages; i++) {
                mIconContainer.removeViewAt(0);
            }
        }
        // ... plus 0/1 TextViews at the end.
        if (extraItems > 0 && !(getLastChild(mIconContainer) instanceof TextView)) {
            // TODO: b/346551087 - Check TODO in preference_circular_icons_plus_item_background
            TextView plusView = (TextView) inflater.inflate(
                    R.layout.preference_circular_icons_plus_item, mIconContainer, false);
            mIconContainer.addView(plusView);
        } else if (extraItems == 0 && (getLastChild(mIconContainer) instanceof TextView)) {
            mIconContainer.removeViewAt(mIconContainer.getChildCount() - 1);
        }

        // Set up placeholders and extra items indicator.
        for (int i = 0; i < numImages; i++) {
            ImageView imageView = (ImageView) mIconContainer.getChildAt(i);
            imageView.setImageDrawable(getPlaceholderImage(getContext()));
        }
        if (extraItems > 0) {
            TextView textView = (TextView) checkNotNull(getLastChild(mIconContainer));
            textView.setText(getContext().getString(R.string.zen_mode_plus_n_items, extraItems));
        }

        // Display icons when all are ready (more consistent than randomly loading).
        mPendingLoadIconsFuture = Futures.allAsList(iconFutures);
        FutureUtil.whenDone(
                mPendingLoadIconsFuture,
                icons -> {
                    checkState(mIconContainer != null);
                    for (int i = 0; i < icons.size(); i++) {
                        ((ImageView) mIconContainer.getChildAt(i)).setImageDrawable(icons.get(i));
                    }
                },
                mUiExecutor);
    }

    private static Drawable getPlaceholderImage(Context context) {
        ShapeDrawable placeholder = new ShapeDrawable(new OvalShape());
        placeholder.setTintList(Utils.getColorAttr(context,
                com.android.internal.R.attr.materialColorSecondaryContainer));
        return placeholder;
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
    List<Drawable> getIcons() {
        if (mIconContainer == null) {
            return List.of();
        }
        ArrayList<Drawable> drawables = new ArrayList<>();
        for (int i = 0; i < getChildCount(mIconContainer, ImageView.class); i++) {
            drawables.add(((ImageView) mIconContainer.getChildAt(i)).getDrawable());
        }
        return drawables;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    String getPlusText() {
        if (mIconContainer == null) {
            return null;
        }
        View lastChild = getLastChild(mIconContainer);
        if (lastChild instanceof TextView tv) {
            return tv.getText() != null ? tv.getText().toString() : null;
        } else {
            return null;
        }
    }
}
