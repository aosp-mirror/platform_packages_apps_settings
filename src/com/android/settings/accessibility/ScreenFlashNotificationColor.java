/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;

import com.android.settings.R;

enum ScreenFlashNotificationColor {
    BLUE(0x4D0000FE, R.string.screen_flash_color_blue),
    AZURE(0x660080FF, R.string.screen_flash_color_azure),
    CYAN(0x4D00FFFF, R.string.screen_flash_color_cyan),
    SPRING_GREEN(0x6600FF7F, R.string.screen_flash_color_spring_green),
    GREEN(0x6600FF01, R.string.screen_flash_color_green),
    CHARTREUSE_GREEN(0x6680FF00, R.string.screen_flash_color_chartreuse_green),
    YELLOW(0x66FFFF00, R.string.screen_flash_color_yellow),
    ORANGE(0x66FF7F00, R.string.screen_flash_color_orange),
    RED(0x66FE0000, R.string.screen_flash_color_red),
    ROSE(0x4DFF017E, R.string.screen_flash_color_rose),
    MAGENTA(0x4DFF00FE, R.string.screen_flash_color_magenta),
    VIOLET(0x667F00FF, R.string.screen_flash_color_violet);

    static final int ALPHA_MASK = 0xFF000000;

    final int mColorInt;
    final int mOpaqueColorInt;
    final int mStringRes;

    ScreenFlashNotificationColor(@ColorInt int colorInt, @StringRes int stringRes) {
        this.mColorInt = colorInt;
        this.mStringRes = stringRes;
        this.mOpaqueColorInt = colorInt | ALPHA_MASK;
    }
}
