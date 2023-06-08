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

package com.android.settings.activityembedding;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.TypedValue;

import androidx.window.embedding.SplitController;

import com.android.settings.R;

/** An util class collecting all common methods for the embedding activity features. */
public class ActivityEmbeddingUtils {
    // The smallest value of current width of the window when the split should be used.
    private static final int MIN_CURRENT_SCREEN_SPLIT_WIDTH_DP = 720;
    // The smallest value of the smallest-width (sw) of the window in any rotation when
    // the split should be used.
    private static final int MIN_SMALLEST_SCREEN_SPLIT_WIDTH_DP = 600;
    // The minimum width of the activity to show the regular homepage layout.
    private static final float MIN_REGULAR_HOMEPAGE_LAYOUT_WIDTH_DP = 380f;
    private static final String TAG = "ActivityEmbeddingUtils";

    /** Get the smallest width dp of the window when the split should be used. */
    public static int getMinCurrentScreenSplitWidthDp() {
        return MIN_CURRENT_SCREEN_SPLIT_WIDTH_DP;
    }

    /**
     * Get the smallest dp value of the smallest-width (sw) of the window in any rotation when
     * the split should be used.
     */
    public static int getMinSmallestScreenSplitWidthDp() {
        return MIN_SMALLEST_SCREEN_SPLIT_WIDTH_DP;
    }

    /**
     * Get the ratio to use when splitting windows. This should be a float which describes
     * the percentage of the screen which the first window should occupy.
     */
    public static float getSplitRatio(Context context) {
        return context.getResources().getFloat(R.dimen.config_activity_embed_split_ratio);
    }

    /** Whether to support embedding activity feature. */
    public static boolean isEmbeddingActivityEnabled(Context context) {
        final boolean isFlagEnabled = FeatureFlagUtils.isEnabled(context,
                FeatureFlagUtils.SETTINGS_SUPPORT_LARGE_SCREEN);
        final boolean isSplitSupported = SplitController.getInstance(context).isSplitSupported();

        Log.d(TAG, "isFlagEnabled = " + isFlagEnabled);
        Log.d(TAG, "isSplitSupported = " + isSplitSupported);

        return isFlagEnabled && isSplitSupported;
    }

    /** Whether to show the regular or simplified homepage layout. */
    public static boolean isRegularHomepageLayout(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return dm.widthPixels >= (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MIN_REGULAR_HOMEPAGE_LAYOUT_WIDTH_DP, dm);
    }
}
