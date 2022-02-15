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

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

/** Grid layout manager that calculates the number of columns for the screen size. */
public final class AutoFitGridLayoutManager extends GridLayoutManager {
    private final float mColumnWidth;

    public AutoFitGridLayoutManager(Context context) {
        super(context, /* spanCount= */ 1);
        this.mColumnWidth = context
                .getResources()
                .getDimensionPixelSize(R.dimen.dream_item_min_column_width);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        final int totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
        final int spanCount = Math.max(1, (int) (totalSpace / mColumnWidth));
        setSpanCount(spanCount);
        super.onLayoutChildren(recycler, state);
    }
}
