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
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.TypedValue;

import androidx.window.embedding.ActivityEmbeddingController;
import androidx.window.embedding.SplitController;

import com.android.settings.R;

import com.google.android.setupcompat.util.WizardManagerHelper;

/** An util class collecting all common methods for the embedding activity features. */
public class ActivityEmbeddingUtils {
    // The minimum width of the activity to show the regular homepage layout.
    private static final float MIN_REGULAR_HOMEPAGE_LAYOUT_WIDTH_DP = 380f;

    /**
     * Indicates whether to enable large screen optimization if the device supports
     * the Activity Embedding split feature.
     * <p>
     * Note that the large screen optimization won't be enabled if the device doesn't support the
     * Activity Embedding feature regardless of this property value.
     *
     * @see androidx.window.embedding.SplitController#getSplitSupportStatus
     * @see androidx.window.embedding.SplitController.SplitSupportStatus#SPLIT_AVAILABLE
     * @see androidx.window.embedding.SplitController.SplitSupportStatus#SPLIT_UNAVAILABLE
     */
    private static final boolean SHOULD_ENABLE_LARGE_SCREEN_OPTIMIZATION =
            SystemProperties.getBoolean("persist.settings.large_screen_opt.enabled", false);

    private static final String TAG = "ActivityEmbeddingUtils";

    /** Get the smallest width dp of the window when the split should be used. */
    public static int getMinCurrentScreenSplitWidthDp(Context context) {
        return context.getResources().getInteger(R.integer.config_activity_embed_split_min_cur_dp);
    }

    /**
     * Get the smallest dp value of the smallest-width (sw) of the window in any rotation when
     * the split should be used.
     */
    public static int getMinSmallestScreenSplitWidthDp(Context context) {
        return context.getResources().getInteger(R.integer.config_activity_embed_split_min_sw_dp);
    }

    /**
     * Get the ratio to use when splitting windows. This should be a float which describes
     * the percentage of the screen which the first window should occupy.
     */
    public static float getSplitRatio(Context context) {
        return context.getResources().getFloat(R.dimen.config_activity_embed_split_ratio);
    }

    /**
     * Returns {@code true} to indicate that Settings app support the Activity Embedding feature on
     * this device. Returns {@code false}, otherwise.
     */
    public static boolean isSettingsSplitEnabled(Context context) {
        return SHOULD_ENABLE_LARGE_SCREEN_OPTIMIZATION
                && SplitController.getInstance(context).getSplitSupportStatus()
                == SplitController.SplitSupportStatus.SPLIT_AVAILABLE;
    }

    /**
     * Checks whether to support embedding activity feature with following conditions:
     * <ul>
     *     <li>Whether {@link #isSettingsSplitEnabled(Context)}</li>
     *     <li>Whether {@link FeatureFlagUtils#SETTINGS_SUPPORT_LARGE_SCREEN} is enabled</li>
     *     <li>Whether User setup is completed</li>
     * </ul>
     */
    public static boolean isEmbeddingActivityEnabled(Context context) {
        // Activity Embedding feature is not enabled if Settings doesn't enable large screen
        // optimization or the device is not supported.
        if (!isSettingsSplitEnabled(context)) {
            Log.d(TAG, "isSettingsSplitSupported = false");
            return false;
        }
        // Activity Embedding feature is not enabled if a user chooses to disable the feature.
        if (!FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_SUPPORT_LARGE_SCREEN)) {
            Log.d(TAG, "isFlagEnabled = false");
            return false;
        }
        // Don't enable Activity embedding for setup wizard.
        if (!WizardManagerHelper.isUserSetupComplete(context)) {
            Log.d(TAG, "isUserSetupComplete = false");
            return false;
        }
        Log.d(TAG, "isEmbeddingActivityEnabled = true");
        return true;
    }

    /** Whether to show the regular or simplified homepage layout. */
    public static boolean isRegularHomepageLayout(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return dm.widthPixels >= (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MIN_REGULAR_HOMEPAGE_LAYOUT_WIDTH_DP, dm);
    }

    /**
     * Check if activity is already embedded
     */
    public static boolean isAlreadyEmbedded(Activity activity) {
        return isEmbeddingActivityEnabled(activity) && ActivityEmbeddingController.getInstance(
                activity).isActivityEmbedded(activity);
    }
}
