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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.LayoutDirection;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;

import com.android.settings.Settings;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.homepage.SettingsHomepageActivity;

import java.util.HashSet;
import java.util.Set;

/** A class to initialize split rules for activity embedding. */
public class ActivityEmbeddingRulesController {

    private static final String TAG = "ActivityEmbeddingCtrl ";
    private final Context mContext;
    private final SplitController mSplitController;

    public ActivityEmbeddingRulesController(Context context) {
        mContext = context;
        mSplitController = new SplitController(context);
    }

    /**
     * Set up embedding rules to place activities to the right pane.
     */
    public void initRules() {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(mContext)) {
            Log.d(TAG, "Not support this feature now");
            return;
        }

        mSplitController.clearRegisteredRules();

        // Set a placeholder for home page.
        registerHomepagePlaceholderRule();
        // Set subsettings rule.
        registerTwoPanePairRule(mContext,
                getComponentName(Settings.class),
                getComponentName(SubSettings.class),
                true /* finishPrimaryWithSecondary */,
                true /* finishSecondaryWithPrimary */);
    }

    /** Register a SplitPairRule for 2-pane. */
    public static void registerTwoPanePairRule(Context context,
            ComponentName primary, ComponentName secondary,
            boolean finishPrimaryWithSecondary, boolean finishSecondaryWithPrimary) {
        final Set<SplitPairFilter> filters = new HashSet<>();
        filters.add(new SplitPairFilter(primary, secondary,
                null /* secondaryActivityIntentAction */,
                null /* secondaryActivityIntentCategory */));

        new SplitController(context).registerRule(new SplitPairRule(filters,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary, true /* clearTop */,
                ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthPx(context),
                ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthPx(context),
                ActivityEmbeddingUtils.SPLIT_RATIO,
                LayoutDirection.LOCALE));
    }

    private void registerHomepagePlaceholderRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        activityFilters.add(new ActivityFilter(getComponentName(SettingsHomepageActivity.class)));
        activityFilters.add(new ActivityFilter(getComponentName(Settings.class)));
        activityFilters.add(new ActivityFilter(new ComponentName(Utils.SETTINGS_PACKAGE_NAME,
                SettingsHomepageActivity.ALIAS_DEEP_LINK)));
        final Intent intent = new Intent();
        intent.setComponent(getComponentName(Settings.NetworkDashboardActivity.class));
        final SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule(
                activityFilters,
                intent,
                ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthPx(mContext),
                ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthPx(mContext),
                ActivityEmbeddingUtils.SPLIT_RATIO,
                LayoutDirection.LOCALE);

        mSplitController.registerRule(placeholderRule);
    }

    @NonNull
    private ComponentName getComponentName(Class<? extends Activity> activityClass) {
        return new ComponentName(mContext.getPackageName(), activityClass.getName());
    }
}
