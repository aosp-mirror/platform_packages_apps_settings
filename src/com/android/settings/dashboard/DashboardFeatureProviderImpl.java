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

import static android.content.Intent.EXTRA_USER;

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY_URI;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.profileselector.ProfileSelectDialog;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.AdaptiveIcon;

import java.util.List;
import java.util.Map;

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
        mCategoryManager = CategoryManager.get(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mPackageManager = context.getPackageManager();
    }

    @Override
    public DashboardCategory getTilesForCategory(String key) {
        return mCategoryManager.getTilesByCategory(mContext, key);
    }

    @Override
    public List<DashboardCategory> getAllCategories() {
        return mCategoryManager.getCategories(mContext);
    }

    @Override
    public String getDashboardKeyForTile(Tile tile) {
        if (tile == null) {
            return null;
        }
        if (tile.hasKey()) {
            return tile.getKey(mContext);
        }
        final StringBuilder sb = new StringBuilder(DASHBOARD_TILE_PREF_KEY_PREFIX);
        final ComponentName component = tile.getIntent().getComponent();
        sb.append(component.getClassName());
        return sb.toString();
    }

    @Override
    public void bindPreferenceToTile(FragmentActivity activity, boolean forceRoundedIcon,
            int sourceMetricsCategory, Preference pref, Tile tile, String key, int baseOrder) {
        if (pref == null) {
            return;
        }
        pref.setTitle(tile.getTitle(activity.getApplicationContext()));
        if (!TextUtils.isEmpty(key)) {
            pref.setKey(key);
        } else {
            pref.setKey(getDashboardKeyForTile(tile));
        }
        bindSummary(pref, tile);
        bindIcon(pref, tile, forceRoundedIcon);
        final Bundle metadata = tile.getMetaData();
        String clsName = null;
        String action = null;

        if (metadata != null) {
            clsName = metadata.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
            action = metadata.getString(META_DATA_KEY_INTENT_ACTION);
        }
        if (!TextUtils.isEmpty(clsName)) {
            pref.setFragment(clsName);
        } else {
            final Intent intent = new Intent(tile.getIntent());
            intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                    sourceMetricsCategory);
            if (action != null) {
                intent.setAction(action);
            }
            pref.setOnPreferenceClickListener(preference -> {
                launchIntentOrSelectProfile(activity, tile, intent, sourceMetricsCategory);
                return true;
            });
        }
        final String skipOffsetPackageName = activity.getPackageName();


        if (tile.hasOrder()) {
            final int order = tile.getOrder();
            boolean shouldSkipBaseOrderOffset = TextUtils.equals(
                    skipOffsetPackageName, tile.getIntent().getComponent().getPackageName());
            if (shouldSkipBaseOrderOffset || baseOrder == Preference.DEFAULT_ORDER) {
                pref.setOrder(order);
            } else {
                pref.setOrder(order + baseOrder);
            }
        }
    }

    @Override
    public void openTileIntent(FragmentActivity activity, Tile tile) {
        if (tile == null) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
            return;
        }
        final Intent intent = new Intent(tile.getIntent())
                .putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                        SettingsEnums.DASHBOARD_SUMMARY)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launchIntentOrSelectProfile(activity, tile, intent, SettingsEnums.DASHBOARD_SUMMARY);
    }

    private void bindSummary(Preference preference, Tile tile) {
        final CharSequence summary = tile.getSummary(mContext);
        if (summary != null) {
            preference.setSummary(summary);
        } else if (tile.getMetaData() != null
                && tile.getMetaData().containsKey(META_DATA_PREFERENCE_SUMMARY_URI)) {
            // Set a placeholder summary before  starting to fetch real summary, this is necessary
            // to avoid preference height change.
            preference.setSummary(R.string.summary_placeholder);

            ThreadUtils.postOnBackgroundThread(() -> {
                final Map<String, IContentProvider> providerMap = new ArrayMap<>();
                final String uri = tile.getMetaData().getString(META_DATA_PREFERENCE_SUMMARY_URI);
                final String summaryFromUri = TileUtils.getTextFromUri(
                        mContext, uri, providerMap, META_DATA_PREFERENCE_SUMMARY);
                ThreadUtils.postOnMainThread(() -> preference.setSummary(summaryFromUri));
            });
        } else {
            preference.setSummary(R.string.summary_placeholder);
        }
    }

    @VisibleForTesting
    void bindIcon(Preference preference, Tile tile, boolean forceRoundedIcon) {
        // Use preference context instead here when get icon from Tile, as we are using the context
        // to get the style to tint the icon.  Using mContext here won't get the correct style.
        final Icon tileIcon = tile.getIcon(preference.getContext());
        if (tileIcon != null) {
            Drawable iconDrawable = tileIcon.loadDrawable(preference.getContext());
            if (forceRoundedIcon
                    && !TextUtils.equals(mContext.getPackageName(), tile.getPackageName())) {
                iconDrawable = new AdaptiveIcon(mContext, iconDrawable);
                ((AdaptiveIcon) iconDrawable).setBackgroundColor(mContext, tile);
            }
            preference.setIcon(iconDrawable);
        } else if (tile.getMetaData() != null
                && tile.getMetaData().containsKey(META_DATA_PREFERENCE_ICON_URI)) {
            ThreadUtils.postOnBackgroundThread(() -> {
                final Intent intent = tile.getIntent();
                String packageName = null;
                if (!TextUtils.isEmpty(intent.getPackage())) {
                    packageName = intent.getPackage();
                } else if (intent.getComponent() != null) {
                    packageName = intent.getComponent().getPackageName();
                }
                final Map<String, IContentProvider> providerMap = new ArrayMap<>();
                final String uri = tile.getMetaData().getString(META_DATA_PREFERENCE_ICON_URI);
                final Pair<String, Integer> iconInfo = TileUtils.getIconFromUri(
                        mContext, packageName, uri, providerMap);
                if (iconInfo == null) {
                    Log.w(TAG, "Failed to get icon from uri " + uri);
                    return;
                }
                final Icon icon = Icon.createWithResource(iconInfo.first, iconInfo.second);
                ThreadUtils.postOnMainThread(() ->
                        preference.setIcon(icon.loadDrawable(preference.getContext()))
                );
            });
        }
    }

    private void launchIntentOrSelectProfile(FragmentActivity activity, Tile tile, Intent intent,
            int sourceMetricCategory) {
        if (!isIntentResolvable(intent)) {
            Log.w(TAG, "Cannot resolve intent, skipping. " + intent);
            return;
        }
        ProfileSelectDialog.updateUserHandlesIfNeeded(mContext, tile);

        if (tile.userHandle == null || tile.isPrimaryProfileOnly()) {
            mMetricsFeatureProvider.logDashboardStartIntent(mContext, intent, sourceMetricCategory);
            activity.startActivityForResult(intent, 0);
        } else if (tile.userHandle.size() == 1) {
            mMetricsFeatureProvider.logDashboardStartIntent(mContext, intent, sourceMetricCategory);
            activity.startActivityForResultAsUser(intent, 0, tile.userHandle.get(0));
        } else {
            final UserHandle userHandle = intent.getParcelableExtra(EXTRA_USER);
            if (userHandle != null && tile.userHandle.contains(userHandle)) {
                mMetricsFeatureProvider.logDashboardStartIntent(
                    mContext, intent, sourceMetricCategory);
                activity.startActivityForResultAsUser(intent, 0, userHandle);
            } else {
                ProfileSelectDialog.show(activity.getSupportFragmentManager(), tile);
            }
        }
    }

    private boolean isIntentResolvable(Intent intent) {
        return mPackageManager.resolveActivity(intent, 0) != null;
    }
}
