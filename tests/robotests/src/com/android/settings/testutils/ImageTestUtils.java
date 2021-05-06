/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.testutils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * Utilities for testing images within unit tests.
 */
public class ImageTestUtils {

    /** Converts the drawable object to bitmap. */
    @Nullable
    public static Bitmap drawableToBitmap(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        final Bitmap bitmap =
                Bitmap.createBitmap(
                        drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
