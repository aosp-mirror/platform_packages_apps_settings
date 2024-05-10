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

package com.android.settings.accessibility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.Objects;

/** LayerDrawable that contains device icon as background and given icon as foreground. */
public class AccessibilityLayerDrawable extends LayerDrawable {

    private AccessibilityLayerDrawableState mState;

    /**
     * Creates a new layer drawable with the list of specified layers.
     *
     * @param layers a list of drawables to use as layers in this new drawable,
     *               must be non-null
     */
    private AccessibilityLayerDrawable(@NonNull Drawable[] layers) {
        super(layers);
    }

    /**
     * Create the {@link LayerDrawable} that contains device icon as background and given menu icon
     * with given {@code opacity} value as foreground.
     *
     * @param context the valid context used to get the icon
     * @param resId the resource ID of the given icon
     * @param opacity the opacity to apply to the given icon
     * @return the drawable that combines the device icon and the given icon
     */
    public static AccessibilityLayerDrawable createLayerDrawable(Context context, int resId,
            int opacity) {
        final Drawable bg = context.getDrawable(R.drawable.a11y_button_preview_base);
        final AccessibilityLayerDrawable basicDrawable = new AccessibilityLayerDrawable(
                new Drawable[]{bg, null});

        basicDrawable.updateLayerDrawable(context, resId, opacity);
        return basicDrawable;
    }

    /**
     * Update the drawable  with given {@code resId} drawable and {@code opacity}(alpha)
     * value at index 1 layer.
     *
     * @param context the valid context used to get the icon
     * @param resId the resource ID of the given icon
     * @param opacity the opacity to apply to the given icon
     */
    public void updateLayerDrawable(Context context, int resId, int opacity) {
        final Drawable icon = context.getDrawable(resId);
        icon.setAlpha(opacity);
        this.setDrawable(/* index= */ 1, icon);
        this.setConstantState(context, resId, opacity);
    }

    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    /** Stores the constant state and data to the given drawable. */
    private void setConstantState(Context context, int resId, int opacity) {
        mState = new AccessibilityLayerDrawableState(context, resId, opacity);
    }

    /** {@link ConstantState} to store the data of {@link AccessibilityLayerDrawable}. */
    @VisibleForTesting
    static class AccessibilityLayerDrawableState extends ConstantState {

        private final Context mContext;
        private final int mResId;
        private final int mOpacity;

        AccessibilityLayerDrawableState(Context context, int resId, int opacity) {
            mContext = context;
            mResId = resId;
            mOpacity = opacity;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return createLayerDrawable(mContext, mResId, mOpacity);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final AccessibilityLayerDrawableState that = (AccessibilityLayerDrawableState) o;
            return mResId == that.mResId
                    && mOpacity == that.mOpacity
                    && Objects.equals(mContext, that.mContext);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mContext, mResId, mOpacity);
        }
    }
}
