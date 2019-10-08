/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TopLevelDisplayPreferenceController extends BasePreferenceController {

    public TopLevelDisplayPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_top_level_display)
        ? AVAILABLE
        : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        final WallpaperPreferenceController controller =
                new WallpaperPreferenceController(mContext, "dummy_key");
        if (controller.isAvailable()) {
            return mContext.getText(
                    controller.areStylesAvailable()
                    ? R.string.display_dashboard_summary_with_style
                    : R.string.display_dashboard_summary);
        } else {
            return mContext.getText(R.string.display_dashboard_nowallpaper_summary);
        }
    }
}
