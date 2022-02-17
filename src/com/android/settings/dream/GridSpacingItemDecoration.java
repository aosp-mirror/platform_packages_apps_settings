/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.DimenRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView item decorator to be used with {@link GridLayoutManager} for applying padding to
 * only the inner elements of the grid.
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int mSpacing;
    private final boolean mRtl;

    public GridSpacingItemDecoration(Context context, @DimenRes int spacingId) {
        mSpacing = context.getResources().getDimensionPixelSize(spacingId);
        mRtl = context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        final RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        if (!(layoutManager instanceof GridLayoutManager)) {
            return;
        }

        final int spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
        final int position = parent.getChildAdapterPosition(view);
        final int column = position % spanCount;

        final int startPadding = column * mSpacing / spanCount;
        final int endPadding = mSpacing - (column + 1) * mSpacing / spanCount;

        outRect.left = mRtl ? endPadding : startPadding;
        outRect.right = mRtl ? startPadding : endPadding;
        if (position >= spanCount) {
            outRect.top = mSpacing;
        }
    }
}
