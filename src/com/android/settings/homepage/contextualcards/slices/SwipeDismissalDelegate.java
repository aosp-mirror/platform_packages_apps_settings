/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;

public class SwipeDismissalDelegate extends ItemTouchHelper.Callback {

    private static final String TAG = "SwipeDismissalDelegate";

    public interface Listener {
        void onSwiped(int position);
    }

    private final Context mContext;
    private final SwipeDismissalDelegate.Listener mListener;
    private final Drawable mIconDelete;
    private final Paint mBgPaint;
    private final int mBgCornerRadius;

    public SwipeDismissalDelegate(Context context, SwipeDismissalDelegate.Listener listener) {
        mContext = context;
        mListener = listener;
        mIconDelete = mContext.getDrawable(R.drawable.ic_delete);
        mBgPaint = new Paint();
        mBgPaint.setColor(mContext.getColor(R.color.homepage_card_dismissal_background));
        mBgCornerRadius = mContext.getResources()
                .getDimensionPixelSize(R.dimen.homepage_card_corner_radius);
    }

    /**
     * Determine whether the ability to drag or swipe should be enabled or not.
     *
     * Only allow swipe on {@link ContextualCard} built with view type
     * {@link SliceContextualCardRenderer#VIEW_TYPE_FULL_WIDTH} or
     * {@link SliceContextualCardRenderer#VIEW_TYPE_HALF_WIDTH}.
     *
     * When the dismissal view is displayed, the swipe will also be disabled.
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder) {
        switch (viewHolder.getItemViewType()) {
            case SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH:
            case SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH:
                //TODO(b/129438972): Convert this to a regular view.
                final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.view_flipper);

                // As we are using ViewFlipper to switch between the initial view and
                // dismissal view, here we are making sure the current displayed view is the
                // initial view of either slice full card or half card, and only allow swipe on
                // these two types.
                if (viewFlipper.getCurrentView().getId() != getInitialViewId(viewHolder)) {
                    // Disable swiping when we are in the dismissal view
                    return 0;
                }
                return makeMovementFlags(0 /*dragFlags*/,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT /*swipeFlags*/);
            default:
                return 0;
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mListener.onSwiped(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        final View itemView = viewHolder.itemView;
        final int iconMargin = mContext.getResources()
                .getDimensionPixelSize(R.dimen.homepage_card_dismissal_side_margin);
        final int iconTop =
                itemView.getTop() + (itemView.getHeight() - mIconDelete.getIntrinsicHeight()) / 2;
        final int iconBottom = iconTop + mIconDelete.getIntrinsicHeight();

        if (dX > 0) { //swipe to the right
            final int iconLeft = itemView.getLeft() + iconMargin;
            final int iconRight = iconLeft + mIconDelete.getIntrinsicWidth();
            final RectF rect = new RectF(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + mBgCornerRadius, itemView.getBottom());
            mIconDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            c.drawRoundRect(rect, mBgCornerRadius, mBgCornerRadius, mBgPaint);
        } else if (dX < 0) {
            final int iconRight = itemView.getRight() - iconMargin;
            final int iconLeft = iconRight - mIconDelete.getIntrinsicWidth();
            final RectF rect = new RectF(itemView.getRight() + ((int) dX), itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            mIconDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            c.drawRoundRect(rect, mBgCornerRadius, mBgCornerRadius, mBgPaint);
        }
        mIconDelete.draw(c);
    }

    private int getInitialViewId(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH) {
            return R.id.content;
        }
        return R.id.slice_view;
    }
}