/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.wifi.qrcode;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A customize square {@link FrameLayout}.
 * This is used for camera preview. Choose the smaller size of both dimensions as length and width.
 */
public class QrPreviewLayout extends FrameLayout {
    public QrPreviewLayout(Context context) {
        super(context);
    }

    public QrPreviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QrPreviewLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Choose the smaller size of the two dimensions.
        if (MeasureSpec.getSize(widthMeasureSpec) > MeasureSpec.getSize(heightMeasureSpec)) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }
}
