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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.Gravity;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.android.settings.R;
import com.android.settingslib.Utils;

import com.google.common.base.Strings;

import java.util.Locale;

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
     * Returns a variant of the supplied mode icon to be used as the header in the mode page. The
     * inner icon is 64x64 dp and it's contained in a 12-sided-cookie of 136dp diameter. It's
     * tinted with the "material secondary" color combination and the "selected" color variant
     * should be used for modes currently active.
     */
    static Drawable makeModeHeader(@NonNull Context context, Drawable modeIcon) {
        return composeIcons(
                checkNotNull(context.getDrawable(R.drawable.ic_zen_mode_icon_cookie)),
                context.getColorStateList(R.color.modes_icon_selectable_background),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_header_size),
                modeIcon,
                context.getColorStateList(R.color.modes_icon_selectable_icon),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_header_inner_icon_size));
    }

    /**
     * Returns a variant of the supplied {@code icon} to be used as the header in the icon picker.
     * The inner icon is 48x48dp and it's contained in a circle of diameter 90dp.
     */
    static Drawable makeIconPickerHeader(@NonNull Context context, Drawable icon) {
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
     * The inner icon is 36x36dp and it's contained in a circle of diameter 54dp. It's also set up
     * so that selection and pressed states are represented in the color.
     */
    static Drawable makeIconPickerItem(@NonNull Context context, @DrawableRes int iconResId) {
        return composeIconCircle(
                context.getColorStateList(R.color.modes_icon_selectable_background),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_item_circle_diameter),
                checkNotNull(context.getDrawable(iconResId)),
                context.getColorStateList(R.color.modes_icon_selectable_icon),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_icon_list_item_icon_size));
    }

    /**
     * Returns a variant of the supplied icon to be used in a {@link CircularIconsPreference}. The
     * inner icon is 20x20 dp and it's contained in a circle of diameter 32dp, and is tinted
     * with the "material secondary" color combination.
     */
    static Drawable makeCircularIconPreferenceItem(@NonNull Context context,
            @DrawableRes int iconResId) {
        return composeIconCircle(
                Utils.getColorAttr(context,
                        com.android.internal.R.attr.materialColorSecondaryContainer),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_circular_icon_diameter),
                checkNotNull(context.getDrawable(iconResId)),
                Utils.getColorAttr(context,
                        com.android.internal.R.attr.materialColorOnSecondaryContainer),
                context.getResources().getDimensionPixelSize(
                        R.dimen.zen_mode_circular_icon_inner_icon_size));
    }

    /**
     * Returns an icon representing a contact that doesn't have an associated photo, to be used in
     * a {@link CircularIconsPreference}, tinted with the "material tertiary". If the contact's
     * display name is not empty, it's the contact's monogram, otherwise it's a generic icon.
     */
    static Drawable makeContactMonogram(@NonNull Context context, @Nullable String displayName) {
        Resources res = context.getResources();
        if (Strings.isNullOrEmpty(displayName)) {
            return composeIconCircle(
                    Utils.getColorAttr(context,
                            com.android.internal.R.attr.materialColorTertiaryContainer),
                    res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter),
                    checkNotNull(context.getDrawable(R.drawable.ic_zen_mode_generic_contact)),
                    Utils.getColorAttr(context,
                            com.android.internal.R.attr.materialColorOnTertiaryContainer),
                    res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_inner_icon_size));
        }

        float diameter = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter);
        Bitmap bitmap = Bitmap.createBitmap((int) diameter, (int) diameter,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint();
        circlePaint.setColor(Utils.getColorAttrDefaultColor(context,
                com.android.internal.R.attr.materialColorTertiaryContainer));
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, circlePaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Utils.getColorAttrDefaultColor(context,
                com.android.internal.R.attr.materialColorOnTertiaryContainer));
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_text_size));

        String text = displayName.substring(0, 1).toUpperCase(Locale.getDefault());
        Rect textRect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textRect);

        float textX = diameter / 2f - textRect.width() / 2f - textRect.left;
        float textY = diameter / 2f + textRect.height() / 2f - textRect.bottom;
        canvas.drawText(text, textX, textY, textPaint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static Drawable composeIconCircle(ColorStateList circleColor, @Px int circleDiameterPx,
            Drawable icon, ColorStateList iconColor, @Px int iconSizePx) {
        return composeIcons(new ShapeDrawable(new OvalShape()), circleColor, circleDiameterPx, icon,
                iconColor, iconSizePx);
    }

    private static Drawable composeIcons(Drawable outer, ColorStateList outerColor,
            @Px int outerSizePx, Drawable icon, ColorStateList iconColor, @Px int iconSizePx) {
        Drawable background = checkNotNull(outer.getConstantState()).newDrawable().mutate();
        background.setTintList(outerColor);
        Drawable foreground = checkNotNull(icon.getConstantState()).newDrawable().mutate();
        foreground.setTintList(iconColor);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { background, foreground });

        layerDrawable.setLayerSize(0, outerSizePx, outerSizePx);
        layerDrawable.setLayerGravity(1, Gravity.CENTER);
        layerDrawable.setLayerSize(1, iconSizePx, iconSizePx);

        layerDrawable.setBounds(0, 0, outerSizePx, outerSizePx);
        return layerDrawable;
    }
}
