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

package com.android.settings.notification.modes;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.Gravity;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import com.android.settings.R;
import com.android.settingslib.Utils;

class IconUtil {

    static Drawable applyNormalTint(@NonNull Context context, @NonNull Drawable icon) {
        return applyTint(context, icon, android.R.attr.colorControlNormal);
    }

    static Drawable applyAccentTint(@NonNull Context context, @NonNull Drawable icon) {
        return applyTint(context, icon, android.R.attr.colorAccent);
    }

    private static Drawable applyTint(@NonNull Context context, @NonNull Drawable icon,
            @AttrRes int colorAttr) {
        icon = icon.mutate();
        icon.setTintList(Utils.getColorAttr(context, colorAttr));
        return icon;
    }

    /**
     * Returns a variant of the supplied {@code icon} to be used as the header in the icon picker.
     * The inner icon is 48x48dp and it's contained into a circle of diameter 90dp.
     */
    static Drawable makeBigIconCircle(@NonNull Context context, Drawable icon) {
        return composeIconCircle(
                Utils.getColorAttr(context,
                        com.android.internal.R.attr.materialColorSecondaryContainer),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_header_circle_diameter),
                icon,
                Utils.getColorAttr(context,
                        com.android.internal.R.attr.materialColorOnSecondaryContainer),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_header_icon_size));
    }

    /**
     * Returns a variant of the supplied {@code icon} to be used as an option in the icon picker.
     * The inner icon is 36x36dp and it's contained into a circle of diameter 54dp. It's also set up
     * so that selection and pressed states are represented in the color.
     */
    static Drawable makeSmallIconCircle(@NonNull Context context, @DrawableRes int iconResId) {
        return composeIconCircle(
                context.getColorStateList(R.color.modes_icon_picker_item_background),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_item_circle_diameter),
                checkNotNull(context.getDrawable(iconResId)),
                context.getColorStateList(R.color.modes_icon_picker_item_icon),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_item_icon_size));
    }

    private static Drawable composeIconCircle(ColorStateList circleColor, @Px int circleDiameterPx,
            Drawable icon, ColorStateList iconColor, @Px int iconSizePx) {
        ShapeDrawable background = new ShapeDrawable(new OvalShape());
        background.setTintList(circleColor);
        Drawable foreground = checkNotNull(icon.getConstantState()).newDrawable().mutate();
        foreground.setTintList(iconColor);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { background, foreground });

        layerDrawable.setBounds(0, 0, circleDiameterPx, circleDiameterPx);
        layerDrawable.setLayerSize(0, circleDiameterPx, circleDiameterPx);
        layerDrawable.setLayerGravity(1, Gravity.CENTER);
        layerDrawable.setLayerSize(1, iconSizePx, iconSizePx);

        return layerDrawable;
    }
}
