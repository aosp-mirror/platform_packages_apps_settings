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

package com.android.settings.display;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.android.settings.R;

public class MessageBubbleBackground extends LinearLayout {
    private final  int mSnapWidthPixels;

    public MessageBubbleBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSnapWidthPixels = context.getResources().getDimensionPixelSize(
                R.dimen.conversation_bubble_width_snap);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthPadding = getPaddingLeft() + getPaddingRight();
        int bubbleWidth = getMeasuredWidth() - widthPadding;
        final int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - widthPadding;
        // Round up to next snapWidthPixels
        bubbleWidth = Math.min(maxWidth,
                (int) (Math.ceil(bubbleWidth / (float) mSnapWidthPixels) * mSnapWidthPixels));
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(bubbleWidth + widthPadding, MeasureSpec.EXACTLY),
                heightMeasureSpec);
    }
}
