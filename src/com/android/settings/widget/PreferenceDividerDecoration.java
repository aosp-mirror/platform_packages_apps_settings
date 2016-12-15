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

package com.android.settings.widget;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class PreferenceDividerDecoration extends RecyclerView.ItemDecoration {

    private Drawable mDivider;
    private int mDividerHeight;

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
        for (int childViewIndex = 0; childViewIndex < childCount; childViewIndex++) {
            final View view = parent.getChildAt(childViewIndex);
            if (shouldDrawDividerAbove(view, parent)) {
                int top = (int) ViewCompat.getY(view);
                mDivider.setBounds(0, top, width, top + mDividerHeight);
                mDivider.draw(c);
            }
        }
    }

    private boolean shouldDrawDividerAbove(View view, RecyclerView parent) {
        final RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null || !(adapter instanceof PreferenceGroupAdapter)) {
            return false;
        }
        final PreferenceGroupAdapter prefAdapter = (PreferenceGroupAdapter) adapter;
        final int adapterPosition = parent.getChildAdapterPosition(view);
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        final Preference pref = prefAdapter.getItem(adapterPosition);
        if (pref instanceof PreferenceCategory) {
            return adapterPosition != 0;
        }
        return pref instanceof FooterPreference;
    }
}
