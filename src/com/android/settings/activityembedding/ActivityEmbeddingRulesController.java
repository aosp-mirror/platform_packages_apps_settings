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
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.EmbeddingAspectRatio;
import androidx.window.embedding.RuleController;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.embedding.SplitRule;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.face.FaceEnrollIntroductionInternal;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroductionInternal;
import com.android.settings.biometrics2.ui.view.FingerprintEnrollmentActivity;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.DeepLinkHomepageActivity;
import com.android.settings.homepage.DeepLinkHomepageActivityInternal;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.remoteauth.RemoteAuthActivity;
import com.android.settings.remoteauth.RemoteAuthActivityInternal;
import com.android.settingslib.users.AvatarPickerActivity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** A class to initialize split rules for activity embedding. */
public class ActivityEmbeddingRulesController {

    private static final String TAG = "ActivityEmbeddingCtrl";
    private static final ComponentName COMPONENT_NAME_WILDCARD = new ComponentName(
            "*" /* pkg */, "*" /* cls */);
    private final Context mContext;
    private final RuleController mRuleController;

    public ActivityEmbeddingRulesController(Context context) {
        mContext = context;
        mRuleController = RuleController.getInstance(context);
    }

    /**
     * Set up embedding rules to place activities to the right pane.
     */
    public void initRules() {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(mContext)) {
            Log.d(TAG, "Not support this feature now");
            return;
        }

        mRuleController.clearRules();

        // Set a placeholder for home page.
        registerHomepagePlaceholderRule();

        registerAlwaysExpandRule();
    }

    /** Register a SplitPairRule for 2-pane. */
    public static void registerTwoPanePairRule(Context context,
            ComponentName primaryComponent,
            ComponentName secondaryComponent,
            String secondaryIntentAction,
            SplitRule.FinishBehavior finishPrimaryWithSecondary,
            SplitRule.FinishBehavior finishSecondaryWithPrimary,
            boolean clearTop) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            return;
        }
        final Set<SplitPairFilter> filters = new HashSet<>();
        filters.add(new SplitPairFilter(primaryComponent, secondaryComponent,
                secondaryIntentAction));

        SplitAttributes attributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(
                        ActivityEmbeddingUtils.getSplitRatio(context)))
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build();
        SplitPairRule splitPairRule = new SplitPairRule.Builder(filters)
                .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
                .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
                .setClearTop(clearTop)
                .setMinWidthDp(ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthDp(context))
                .setMinSmallestWidthDp(
                        ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthDp(context))
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setDefaultSplitAttributes(attributes)
                .build();
        RuleController.getInstance(context).addRule(splitPairRule);
    }

    /**
     * Registers a {@link SplitPairRule} for all classes that Settings homepage can be invoked from.
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
                new ComponentName(context, Settings.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary ? SplitRule.FinishBehavior.ADJACENT
                        : SplitRule.FinishBehavior.NEVER,
                finishSecondaryWithPrimary ? SplitRule.FinishBehavior.ADJACENT
                        : SplitRule.FinishBehavior.NEVER,
                clearTop);

        registerTwoPanePairRule(
                context,
                new ComponentName(context, SettingsHomepageActivity.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary ? SplitRule.FinishBehavior.ADJACENT
                        : SplitRule.FinishBehavior.NEVER,
                finishSecondaryWithPrimary ? SplitRule.FinishBehavior.ADJACENT
                        : SplitRule.FinishBehavior.NEVER,
                clearTop);

        // We should finish HomePageActivity altogether even if it shows in single pane for all deep
        // link cases.
        registerTwoPanePairRule(
                context,
                new ComponentName(context, DeepLinkHomepageActivity.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary ? SplitRule.FinishBehavior.ALWAYS
                        : SplitRule.FinishBehavior.NEVER,
                finishSecondaryWithPrimary ? SplitRule.FinishBehavior.ALWAYS
                        : SplitRule.FinishBehavior.NEVER,
                clearTop);

        registerTwoPanePairRule(
                context,
                new ComponentName(context, DeepLinkHomepageActivityInternal.class),
                secondaryComponent,
                secondaryIntentAction,
                finishPrimaryWithSecondary ? SplitRule.FinishBehavior.ALWAYS
                        : SplitRule.FinishBehavior.NEVER,
                finishSecondaryWithPrimary ? SplitRule.FinishBehavior.ALWAYS
                        : SplitRule.FinishBehavior.NEVER,
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
                new ComponentName(context, SubSettings.class),
                null /* secondaryIntentAction */,
                clearTop);

        registerTwoPanePairRuleForSettingsHome(
                context,
                COMPONENT_NAME_WILDCARD,
                Intent.ACTION_SAFETY_CENTER,
                clearTop
        );
    }

    private void registerHomepagePlaceholderRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        addActivityFilter(activityFilters, SettingsHomepageActivity.class);
        addActivityFilter(activityFilters, Settings.class);

        final Intent intent = new Intent(mContext, Settings.NetworkDashboardActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_IS_SECOND_LAYER_PAGE, true);
        SplitAttributes attributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(
                        ActivityEmbeddingUtils.getSplitRatio(mContext)))
                .build();
        final SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule.Builder(
                activityFilters, intent)
                .setMinWidthDp(ActivityEmbeddingUtils.getMinCurrentScreenSplitWidthDp(mContext))
                .setMinSmallestWidthDp(
                        ActivityEmbeddingUtils.getMinSmallestScreenSplitWidthDp(mContext))
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setSticky(false)
                .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ADJACENT)
                .setDefaultSplitAttributes(attributes)
                .build();

        mRuleController.addRule(placeholderRule);
    }

    private void registerAlwaysExpandRule() {
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlags.SETTINGS_SEARCH_ALWAYS_EXPAND)) {
            final Intent searchIntent = FeatureFactory.getFeatureFactory()
                    .getSearchFeatureProvider()
                    .buildSearchIntent(mContext, SettingsEnums.SETTINGS_HOMEPAGE);
            addActivityFilter(activityFilters, searchIntent);
        }
        addActivityFilter(activityFilters, FingerprintEnrollmentActivity.class);
        addActivityFilter(activityFilters, FingerprintEnrollmentActivity.InternalActivity.class);
        addActivityFilter(activityFilters, FingerprintEnrollIntroduction.class);
        addActivityFilter(activityFilters, FingerprintEnrollIntroductionInternal.class);
        addActivityFilter(activityFilters, FingerprintEnrollEnrolling.class);
        addActivityFilter(activityFilters, FaceEnrollIntroductionInternal.class);
        addActivityFilter(activityFilters, FaceEnrollIntroduction.class);
        addActivityFilter(activityFilters, RemoteAuthActivity.class);
        addActivityFilter(activityFilters, RemoteAuthActivityInternal.class);
        addActivityFilter(activityFilters, ChooseLockPattern.class);
        if (android.multiuser.Flags.avatarSync()) {
            String action = mContext.getString(R.string.config_avatar_picker_action);
            addActivityFilter(activityFilters, new Intent(action));
        } else {
            addActivityFilter(activityFilters, AvatarPickerActivity.class);
        }
        ActivityRule activityRule = new ActivityRule.Builder(activityFilters).setAlwaysExpand(true)
                .build();
        mRuleController.addRule(activityRule);
    }

    private static void addActivityFilter(Set<ActivityFilter> activityFilters, Intent intent) {
        activityFilters.add(new ActivityFilter(COMPONENT_NAME_WILDCARD, intent.getAction()));
    }

    private void addActivityFilter(Collection<ActivityFilter> activityFilters,
            Class<? extends Activity> activityClass) {
        activityFilters.add(new ActivityFilter(new ComponentName(mContext, activityClass),
                null /* intentAction */));
    }
}
