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
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.embedding.SplitRule;

import com.android.settings.Settings;
import com.android.settings.SubSettings;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.homepage.DeepLinkHomepageActivity;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.homepage.SliceDeepLinkHomepageActivity;

import java.util.HashSet;
import java.util.Set;

/** A class to initialize split rules for activity embedding. */
public class ActivityEmbeddingRulesController {

    private static final String TAG = "ActivityEmbeddingCtrl";
    private final Context mContext;
    private final SplitController mSplitController;

    public ActivityEmbeddingRulesController(Context context) {
        mContext = context;
        mSplitController = SplitController.getInstance();
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

        registerAlwaysExpandRule();
    }

    /** Register a SplitPairRule for 2-pane. */
    public static void registerTwoPanePairRule(Context context,
            ComponentName primaryComponent,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            boolean finishPrimaryWithSecondary,
            boolean finishSecondaryWithPrimary,
            boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            return;
        }
        final Set<SplitPairFilter> filters = new HashSet<>();
        filters.add(new SplitPairFilter(primaryComponent, secondaryComponent,
                secondaryIntentAction));

        SplitController.getInstance().registerRule(new SplitPairRule(filters,
                finishPrimaryWithSecondary ? SplitRule.FINISH_ADJACENT : SplitRule.FINISH_NEVER,
                finishSecondaryWithPrimary ? SplitRule.FINISH_ADJACENT : SplitRule.FINISH_NEVER,
                clearTop,
                ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthPx(context),
                ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthPx(context),
                ActivityEmbeddingUtils.SPLIT_RATIO,
                LayoutDirection.LOCALE));
    }

    /**
     * Register a new SplitPairRule for Settings home. Because homepage is able to be opened by
     * {@link Settings} or {@link SettingsHomepageActivity} or
     * {@link SliceDeepLinkHomepageActivity}, we register split rule for above cases.
     */
    public static void registerTwoPanePairRuleForSettingsHome(Context context,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            boolean finishPrimaryWithSecondary,
            boolean finishSecondaryWithPrimary,
            boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            return;
        }

        registerTwoPanePairRule(
                context,
                getComponentName(context, Settings.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop);

        registerTwoPanePairRule(
                context,
                new ComponentName(context, DeepLinkHomepageActivity.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop);

        registerTwoPanePairRule(
                context,
                getComponentName(context, SettingsHomepageActivity.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop);

        registerTwoPanePairRule(
                context,
                getComponentName(context, SliceDeepLinkHomepageActivity.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop);
    }

    /**
     * Register a new SplitPairRule for Settings home.
     */
    public static void registerTwoPanePairRuleForSettingsHome(Context context,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            return;
        }

        registerTwoPanePairRuleForSettingsHome(
                context,
                secondaryComponent,
                secondaryIntentAction,
                true /* finishPrimaryWithSecondary */,
                true /* finishSecondaryWithPrimary */,
                clearTop);
    }

    /** Register a SplitPairRule for SubSettings if the device supports 2-pane. */
    public static void registerSubSettingsPairRule(Context context, boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            return;
        }

        registerTwoPanePairRuleForSettingsHome(
                context,
                getComponentName(context, SubSettings.class),
                null /* secondaryIntentAction */,
                clearTop);
    }

    private void registerHomepagePlaceholderRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        addActivityFilter(activityFilters, SettingsHomepageActivity.class);
        addActivityFilter(activityFilters, DeepLinkHomepageActivity.class);
        addActivityFilter(activityFilters, SliceDeepLinkHomepageActivity.class);
        addActivityFilter(activityFilters, Settings.class);

        final Intent intent = new Intent();
        intent.setComponent(getComponentName(Settings.NetworkDashboardActivity.class));
        final SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule(
                activityFilters,
                intent,
                true /* stickyPlaceholder */,
                SplitRule.FINISH_ADJACENT,
                ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthPx(mContext),
                ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthPx(mContext),
                ActivityEmbeddingUtils.SPLIT_RATIO,
                LayoutDirection.LOCALE);

        mSplitController.registerRule(placeholderRule);
    }

    private void registerAlwaysExpandRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        addActivityFilter(activityFilters, FingerprintEnrollIntroduction.class);
        addActivityFilter(activityFilters, FingerprintEnrollEnrolling.class);
        mSplitController.registerRule(new ActivityRule(activityFilters, true /* alwaysExpand */));
    }

    private void addActivityFilter(Set<ActivityFilter> activityFilters,
            Class<? extends Activity> activityClass) {
        activityFilters.add(new ActivityFilter(getComponentName(activityClass),
                null /* intentAction */));
    }

    private void addActivityFilter(Set<ActivityFilter> activityFilters,
            ComponentName componentName) {
        activityFilters.add(new ActivityFilter(componentName, null /* intentAction */));
    }

    @NonNull
    private ComponentName getComponentName(Class<? extends Activity> activityClass) {
        return getComponentName(mContext, activityClass);
    }

    @NonNull
    private static ComponentName getComponentName(Context context,
            Class<? extends Activity> activityClass) {
        return new ComponentName(context.getPackageName(), activityClass.getName());
    }
}
