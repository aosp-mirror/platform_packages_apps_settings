/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.settings.overlay.FeatureFactory;

public class DashboardDividerDecoration extends RecyclerView.ItemDecoration {

    private final DashboardFeatureProvider mDashboardFeatureProvider;

    private Drawable mDivider;
    private int mDividerHeight;

    public DashboardDividerDecoration(Context context) {
        mDashboardFeatureProvider = FeatureFactory.getFactory(context)
                .getDashboardFeatureProvider(context);
    }

    public void setDivider(Drawable divider) {
        if (divider != null) {
            mDividerHeight = divider.getIntrinsicHeight();
        } else {
            mDividerHeight = 0;
        }
        mDivider = divider;
    }

    public void setDividerHeight(int dividerHeight) {
        mDividerHeight = dividerHeight;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDivider == null) {
            return;
        }
        final int childCount = parent.getChildCount();
        final int width = parent.getWidth();
        for (int childViewIndex = 0; childViewIndex < childCount - 1; childViewIndex++) {
            final View view = parent.getChildAt(childViewIndex);
            if (shouldDrawDividerBelow(view, parent)) {
                int top = (int) ViewCompat.getY(view) + view.getHeight();
                mDivider.setBounds(0, top, width, top + mDividerHeight);
                mDivider.draw(c);
            }
        }
    }

    private boolean shouldDrawDividerBelow(View view, RecyclerView parent) {
        final RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null || !(adapter instanceof PreferenceGroupAdapter)) {
            return false;
        }
        final PreferenceGroupAdapter prefAdapter = (PreferenceGroupAdapter) adapter;
        final int adapterPosition = parent.getChildAdapterPosition(view);
        final Preference pref = prefAdapter.getItem(adapterPosition);
        final Preference nextPref = prefAdapter.getItem(adapterPosition + 1);
        if (nextPref == null) {
            return false;
        }

        return mDashboardFeatureProvider.getPriorityGroup(pref)
                != mDashboardFeatureProvider.getPriorityGroup(nextPref);
    }

}
