/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.wallpaper;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;

public class StyleSuggestionActivity extends StyleSuggestionActivityBase {

    @VisibleForTesting
    public static boolean isSuggestionComplete(Context context) {
        if (!isWallpaperServiceEnabled(context)) {
            return true;
        }

        final String currentTheme = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, context.getUserId());
        if (TextUtils.isEmpty(currentTheme)) {
            // Empty value means the user has not visited the styles tab yet
            return false;
        }
        return true;
    }
}
