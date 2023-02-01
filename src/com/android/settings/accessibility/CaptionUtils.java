/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.graphics.Color;
import android.view.accessibility.CaptioningManager.CaptionStyle;

/** Provides utility methods related caption. */
public final class CaptionUtils {

    /**
     * Unpacks the specified color value to get the color value.
     *
     * @param value the specified color value.
     */
    public static int parseColor(int value) {
        final int colorValue;
        if (!CaptionStyle.hasColor(value)) {
            // "Default" color with variable alpha.
            colorValue = CaptionStyle.COLOR_UNSPECIFIED;
        } else if ((value >>> 24) == 0) {
            // "None" color with variable alpha.
            colorValue = Color.TRANSPARENT;
        } else {
            // Normal color.
            colorValue = value | 0xFF000000;
        }
        return colorValue;
    }

    /**
     * Unpacks the specified color value to get the opacity value.
     *
     * @param value the specified color value.
     */
    public static int parseOpacity(int value) {
        final int opacityValue;
        if (!CaptionStyle.hasColor(value)) {
            // "Default" color with variable alpha.
            opacityValue = (value & 0xFF) << 24;
        } else if ((value >>> 24) == 0) {
            // "None" color with variable alpha.
            opacityValue = (value & 0xFF) << 24;
        } else {
            // Normal color.
            opacityValue = value & 0xFF000000;
        }

        // Opacity value is always white.
        return opacityValue | 0xFFFFFF;
    }

    /**
     * Packs the specified color value and specified opacity value into merged color value.
     *
     * @param colorValue the color value.
     * @param opacityValue the opacity value.
     */
    public static int mergeColorOpacity(int colorValue, int opacityValue) {
        final int value;
        // "Default" is 0x00FFFFFF or, for legacy support, 0x00000100.
        if (!CaptionStyle.hasColor(colorValue)) {
            // Encode "default" as 0x00FFFFaa.
            value = 0x00FFFF00 | Color.alpha(opacityValue);
        } else if (colorValue == Color.TRANSPARENT) {
            // Encode "none" as 0x000000aa.
            value = Color.alpha(opacityValue);
        } else {
            // Encode custom color normally.
            value = (colorValue & 0x00FFFFFF) | (opacityValue & 0xFF000000);
        }
        return value;
    }
}
