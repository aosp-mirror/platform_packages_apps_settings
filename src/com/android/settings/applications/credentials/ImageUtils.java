/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

/** Handles resizing of images for CredMan settings. */
public class ImageUtils {

    /**
     * Utility class to resize icons to match default icon size. Code is mostly borrowed from
     * Launcher and ActivityPicker.
     */
    public static class IconResizer {
        private final int mIconWidth;
        private final int mIconHeight;

        private final DisplayMetrics mMetrics;
        private final Rect mOldBounds = new Rect();
        private final Canvas mCanvas = new Canvas();

        public IconResizer(int width, int height, DisplayMetrics metrics) {
            mCanvas.setDrawFilter(
                    new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));

            mMetrics = metrics;
            mIconWidth = width;
            mIconHeight = height;
        }

        /**
         * Returns a Drawable representing the thumbnail of the specified Drawable. The size of the
         * thumbnail is defined by the dimension android.R.dimen.app_icon_size.
         *
         * <p>This method is not thread-safe and should be invoked on the UI thread only.
         *
         * @param icon The icon to get a thumbnail of.
         * @return A thumbnail for the specified icon or the icon itself if the thumbnail could not
         *     be created.
         */
        public Drawable createIconThumbnail(Drawable icon) {
            int width = mIconWidth;
            int height = mIconHeight;

            if (icon == null) {
                return new EmptyDrawable(width, height);
            }

            try {
                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                } else if (icon instanceof BitmapDrawable) {
                    // Ensure the bitmap has a density.
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                        bitmapDrawable.setTargetDensity(mMetrics);
                    }
                }
                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();

                if (iconWidth > 0 && iconHeight > 0) {
                    if (width < iconWidth || height < iconHeight) {
                        final float ratio = (float) iconWidth / iconHeight;

                        if (iconWidth > iconHeight) {
                            height = (int) (width / ratio);
                        } else if (iconHeight > iconWidth) {
                            width = (int) (height * ratio);
                        }

                        final Bitmap.Config c =
                                icon.getOpacity() != PixelFormat.OPAQUE
                                        ? Bitmap.Config.ARGB_8888
                                        : Bitmap.Config.RGB_565;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);

                        // Copy the old bounds to restore them later
                        // If we were to do oldBounds = icon.getBounds(),
                        // the call to setBounds() that follows would
                        // change the same instance and we would lose the
                        // old bounds.
                        mOldBounds.set(icon.getBounds());
                        final int x = (mIconWidth - width) / 2;
                        final int y = (mIconHeight - height) / 2;
                        icon.setBounds(x, y, x + width, y + height);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);

                        // Create the new resized drawable.
                        icon = createBitmapDrawable(thumb);
                    } else if (iconWidth < width && iconHeight < height) {
                        final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);
                        mOldBounds.set(icon.getBounds());

                        // Set the bounds for the new icon.
                        final int x = (width - iconWidth) / 2;
                        final int y = (height - iconHeight) / 2;
                        icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);

                        // Create the new resized drawable.
                        icon = createBitmapDrawable(thumb);
                    }
                }

            } catch (Throwable t) {
                icon = new EmptyDrawable(width, height);
            }

            return icon;
        }

        private BitmapDrawable createBitmapDrawable(Bitmap thumb) {
            BitmapDrawable icon = new BitmapDrawable(thumb);
            icon.setTargetDensity(mMetrics);
            mCanvas.setBitmap(null);
            return icon;
        }
    }

    public static class EmptyDrawable extends Drawable {
        private final int mWidth;
        private final int mHeight;

        EmptyDrawable(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }

        @Override
        public int getMinimumWidth() {
            return mWidth;
        }

        @Override
        public int getMinimumHeight() {
            return mHeight;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {}

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(@NonNull ColorFilter cf) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
