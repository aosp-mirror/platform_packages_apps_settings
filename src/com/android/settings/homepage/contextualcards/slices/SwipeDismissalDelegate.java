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

import android.graphics.Canvas;
import android.view.View;

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

    private final SwipeDismissalDelegate.Listener mListener;

    public SwipeDismissalDelegate(SwipeDismissalDelegate.Listener listener) {
        mListener = listener;
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
        if (viewHolder.getItemViewType() == SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH
                || viewHolder.getItemViewType()
                == SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH) {// Here we are making sure
            // the current displayed view is the initial view of
            // either slice full card or half card, and only allow swipe on these two types.
            if (viewHolder.itemView.findViewById(R.id.dismissal_view).getVisibility()
                    == View.VISIBLE) {
                // Disable swiping when we are in the dismissal view
                return 0;
            }
            return makeMovementFlags(0 /*dragFlags*/,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT /*swipeFlags*/);
        }
        return 0;
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
    public void clearView(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder) {
        final View view = getSwipeableView(viewHolder);
        getDefaultUIUtil().clearView(view);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        final View view = getSwipeableView(viewHolder);
        final View iconStart = viewHolder.itemView.findViewById(R.id.dismissal_icon_start);
        final View iconEnd = viewHolder.itemView.findViewById(R.id.dismissal_icon_end);

        if (dX > 0) {
            iconStart.setVisibility(View.VISIBLE);
            iconEnd.setVisibility(View.GONE);
        } else if (dX < 0) {
            iconStart.setVisibility(View.GONE);
            iconEnd.setVisibility(View.VISIBLE);
        }
        getDefaultUIUtil().onDraw(c, recyclerView, view, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * Get the foreground view from the {@link android.widget.FrameLayout} as we only swipe
     * the foreground out in {@link SwipeDismissalDelegate#onChildDraw} and gets the view
     * beneath revealed.
     *
     * @return The foreground view.
     */
    private View getSwipeableView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH) {
            return ((SliceHalfCardRendererHelper.HalfCardViewHolder) viewHolder).content;
        }
        return ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView;
    }
}