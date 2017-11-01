/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.CategoryManager;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.ProfileSelectDialog;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Impl for {@code DashboardFeatureProvider}.
 */
public class DashboardFeatureProviderImpl implements DashboardFeatureProvider {

    private static final String TAG = "DashboardFeatureImpl";

    private static final String DASHBOARD_TILE_PREF_KEY_PREFIX = "dashboard_tile_pref_";
    private static final String META_DATA_KEY_INTENT_ACTION = "com.android.settings.intent.action";

    protected final Context mContext;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final CategoryManager mCategoryManager;
    private final PackageManager mPackageManager;

    public DashboardFeatureProviderImpl(Context context) {
        mContext = context.getApplicationContext();
        mCategoryManager = CategoryManager.get(context, getExtraIntentAction());
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mPackageManager = context.getPackageManager();
    }

    @Override
    public DashboardCategory getTilesForCategory(String key) {
        return mCategoryManager.getTilesByCategory(mContext, key);
    }

    @Override
    public List<Preference> getPreferencesForCategory(Activity activity, Context context,
            int sourceMetricsCategory, String key) {
        final DashboardCategory category = getTilesForCategory(key);
        if (category == null) {
            Log.d(TAG, "NO dashboard tiles for " + TAG);
            return null;
        }
        final List<Tile> tiles = category.tiles;
        if (tiles == null || tiles.isEmpty()) {
            Log.d(TAG, "tile list is empty, skipping category " + category.title);
            return null;
        }
        final List<Preference> preferences = new ArrayList<>();
        for (Tile tile : tiles) {
            final Preference pref = new Preference(context);
            bindPreferenceToTile(activity, sourceMetricsCategory, pref, tile, null /* key */,
                    Preference.DEFAULT_ORDER /* baseOrder */);
            preferences.add(pref);
        }
        return preferences;
    }

    @Override
    public List<DashboardCategory> getAllCategories() {
        return mCategoryManager.getCategories(mContext);
    }

    @Override
    public boolean shouldTintIcon() {
        return mContext.getResources().getBoolean(R.bool.config_tintSettingIcon);
    }

    @Override
    public String getDashboardKeyForTile(Tile tile) {
        if (tile == null || tile.intent == null) {
            return null;
        }
        if (!TextUtils.isEmpty(tile.key)) {
            return tile.key;
        }
        final StringBuilder sb = new StringBuilder(DASHBOARD_TILE_PREF_KEY_PREFIX);
        final ComponentName component = tile.intent.getComponent();
        sb.append(component.getClassName());
        return sb.toString();
    }

    @Override
    public void bindPreferenceToTile(Activity activity, int sourceMetricsCategory, Preference pref,
            Tile tile, String key, int baseOrder) {
        pref.setTitle(tile.title);
        if (!TextUtils.isEmpty(key)) {
            pref.setKey(key);
        } else {
            pref.setKey(getDashboardKeyForTile(tile));
        }
        if (tile.summary != null) {
            pref.setSummary(tile.summary);
        } else {
            pref.setSummary(R.string.summary_placeholder);
        }
        if (tile.icon != null) {
            pref.setIcon(tile.icon.loadDrawable(activity));
        }
        final Bundle metadata = tile.metaData;
        String clsName = null;
        String action = null;
        if (metadata != null) {
            clsName = metadata.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
            action = metadata.getString(META_DATA_KEY_INTENT_ACTION);
        }
        if (!TextUtils.isEmpty(clsName)) {
            pref.setFragment(clsName);
        } else if (tile.intent != null) {
            final Intent intent = new Intent(tile.intent);
            intent.putExtra(SettingsActivity.EXTRA_SOURCE_METRICS_CATEGORY, sourceMetricsCategory);
            if (action != null) {
                intent.setAction(action);
            }
            pref.setOnPreferenceClickListener(preference -> {
                launchIntentOrSelectProfile(activity, tile, intent, sourceMetricsCategory);
                return true;
            });
        }
        final String skipOffsetPackageName = activity.getPackageName();
        // Use negated priority for order, because tile priority is based on intent-filter
        // (larger value has higher priority). However pref order defines smaller value has
        // higher priority.
        if (tile.priority != 0) {
            boolean shouldSkipBaseOrderOffset = false;
            if (tile.intent != null) {
                shouldSkipBaseOrderOffset = TextUtils.equals(
                        skipOffsetPackageName, tile.intent.getComponent().getPackageName());
            }
            if (shouldSkipBaseOrderOffset || baseOrder == Preference.DEFAULT_ORDER) {
                pref.setOrder(-tile.priority);
            } else {
                pref.setOrder(-tile.priority + baseOrder);
            }
        }
    }

    @Override
    public ProgressiveDisclosureMixin getProgressiveDisclosureMixin(Context context,
            DashboardFragment fragment, Bundle args) {
        boolean keepExpanded = false;
        if (args != null) {
            keepExpanded = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY) != null;
        }
        return new ProgressiveDisclosureMixin(context, fragment, keepExpanded);
    }

    @Override
    public String getExtraIntentAction() {
        return null;
    }

    @Override
    public void openTileIntent(Activity activity, Tile tile) {
        if (tile == null) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
            return;
        }

        if (tile.intent == null) {
            return;
        }
        final Intent intent = new Intent(tile.intent)
                .putExtra(SettingsActivity.EXTRA_SOURCE_METRICS_CATEGORY,
                        MetricsEvent.DASHBOARD_SUMMARY)
                .putExtra(SettingsDrawerActivity.EXTRA_SHOW_MENU, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launchIntentOrSelectProfile(activity, tile, intent, MetricsEvent.DASHBOARD_SUMMARY);
    }

    private void launchIntentOrSelectProfile(Activity activity, Tile tile, Intent intent,
            int sourceMetricCategory) {
        if (!isIntentResolvable(intent)) {
            Log.w(TAG, "Cannot resolve intent, skipping. " + intent);
            return;
        }
        ProfileSelectDialog.updateUserHandlesIfNeeded(mContext, tile);
        if (tile.userHandle == null) {
            mMetricsFeatureProvider.logDashboardStartIntent(mContext, intent, sourceMetricCategory);
            activity.startActivityForResult(intent, 0);
        } else if (tile.userHandle.size() == 1) {
            mMetricsFeatureProvider.logDashboardStartIntent(mContext, intent, sourceMetricCategory);
            activity.startActivityForResultAsUser(intent, 0, tile.userHandle.get(0));
        } else {
            ProfileSelectDialog.show(activity.getFragmentManager(), tile);
        }
    }

    private boolean isIntentResolvable(Intent intent) {
        return mPackageManager.resolveActivity(intent, 0) != null;
    }
}
