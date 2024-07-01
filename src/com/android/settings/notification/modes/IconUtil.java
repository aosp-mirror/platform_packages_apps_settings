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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

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
     * Returns a variant of the supplied {@code icon} to be used in the icon picker. The inner icon
     * is 36x36dp and it's contained into a circle of diameter 54dp. It's also set up so that
     * selection and pressed states are represented in the color.
     */
    static Drawable makeIconCircle(@NonNull Context context, @NonNull Drawable icon) {
        ShapeDrawable background = new ShapeDrawable(new OvalShape());
        background.setTintList(
                context.getColorStateList(R.color.modes_icon_picker_item_background));
        icon = icon.mutate();
        icon.setTintList(
                context.getColorStateList(R.color.modes_icon_picker_item_icon));

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { background, icon });

        int circleDiameter = context.getResources().getDimensionPixelSize(
                R.dimen.zen_mode_icon_list_circle_diameter);
        int iconSize = context.getResources().getDimensionPixelSize(
                R.dimen.zen_mode_icon_list_icon_size);
        int iconPadding = (circleDiameter - iconSize) / 2;
        layerDrawable.setBounds(0, 0, circleDiameter, circleDiameter);
        layerDrawable.setLayerInset(1, iconPadding, iconPadding, iconPadding, iconPadding);

        return layerDrawable;
    }

    static Drawable makeIconCircle(@NonNull Context context, @DrawableRes int iconResId) {
        return makeIconCircle(context, checkNotNull(context.getDrawable(iconResId)));
    }
}
