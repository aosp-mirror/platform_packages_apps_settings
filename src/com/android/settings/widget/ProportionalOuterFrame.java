/*
 * Copyright (C) 2011 Google Inc.
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

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.settings.R;

/**
 * Used as the outer frame of all setup wizard pages that need to adjust their margins based
 * on the total size of the available display. (e.g. side margins set to 10% of total width.)
 */
public class ProportionalOuterFrame extends RelativeLayout {
    public ProportionalOuterFrame(Context context) {
        super(context);
    }

    public ProportionalOuterFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProportionalOuterFrame(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Set our margins and title area height proportionally to the available display size
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        final Resources res = getContext().getResources();
        final float titleHeight = res.getFraction(R.dimen.setup_title_height, 1, 1);
        final float sideMargin = res.getFraction(R.dimen.setup_border_width, 1, 1);
        final int bottom = res.getDimensionPixelSize(R.dimen.setup_margin_bottom);
        setPaddingRelative((int) (parentWidth * sideMargin), 0,
                (int) (parentWidth * sideMargin), bottom);
        final View title = findViewById(R.id.title_area);
        if (title != null) {
            title.setMinimumHeight((int) (parentHeight * titleHeight));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
