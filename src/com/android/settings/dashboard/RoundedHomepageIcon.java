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
 * limitations under the License.
 */

package com.android.settings.dashboard;

import static androidx.annotation.VisibleForTesting.NONE;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

public class RoundedHomepageIcon extends LayerDrawable {

    @VisibleForTesting(otherwise = NONE)
    int mBackgroundColor = -1;

    public RoundedHomepageIcon(Context context, Drawable foreground) {
        super(new Drawable[] {
                context.getDrawable(R.drawable.ic_homepage_generic_background),
                foreground
        });
        final int insetPx = context.getResources()
                .getDimensionPixelSize(R.dimen.dashboard_tile_foreground_image_inset);
        setLayerInset(1 /* index */, insetPx, insetPx, insetPx, insetPx);
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        getDrawable(0).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }
}
