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

package com.android.settings.display;

import static android.hardware.display.ColorDisplayManager.COLOR_MODE_AUTOMATIC;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_BOOSTED;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_NATURAL;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_SATURATED;
import static android.hardware.display.ColorDisplayManager.VENDOR_COLOR_MODE_RANGE_MAX;
import static android.hardware.display.ColorDisplayManager.VENDOR_COLOR_MODE_RANGE_MIN;

import android.content.res.Resources;
import android.util.ArrayMap;

import com.android.settings.R;

import java.util.Map;

final class ColorModeUtils {

    private ColorModeUtils() {
        // Do not instantiate.
    }

    static Map<Integer, String> getColorModeMapping(Resources resources) {
        final String[] colorModeOptionsStrings = resources.getStringArray(
                R.array.config_color_mode_options_strings);
        final int[] colorModeOptionsValues = resources.getIntArray(
                R.array.config_color_mode_options_values);
        if (colorModeOptionsStrings.length != colorModeOptionsValues.length) {
            throw new RuntimeException("Color mode options of unequal length");
        }

        final Map<Integer, String> colorModesToSummaries = new ArrayMap<>();
        for (int i = 0; i < colorModeOptionsValues.length; i++) {
            final int colorMode = colorModeOptionsValues[i];
            if (colorMode == COLOR_MODE_NATURAL
                    || colorMode == COLOR_MODE_BOOSTED
                    || colorMode == COLOR_MODE_SATURATED
                    || colorMode == COLOR_MODE_AUTOMATIC
                    || (colorMode >= VENDOR_COLOR_MODE_RANGE_MIN
                    && colorMode <= VENDOR_COLOR_MODE_RANGE_MAX)) {
                colorModesToSummaries.put(colorMode, colorModeOptionsStrings[i]);
            }
        }
        return colorModesToSummaries;
    }
}
