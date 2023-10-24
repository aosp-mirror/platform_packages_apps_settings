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

import static com.android.settingslib.drawer.SwitchesProvider.EXTRA_SWITCH_CHECKED_STATE;
import static com.android.settingslib.drawer.SwitchesProvider.EXTRA_SWITCH_SET_CHECKED_ERROR;
import static com.android.settingslib.drawer.SwitchesProvider.EXTRA_SWITCH_SET_CHECKED_ERROR_MESSAGE;
import static com.android.settingslib.drawer.SwitchesProvider.METHOD_GET_DYNAMIC_SUMMARY;
import static com.android.settingslib.drawer.SwitchesProvider.METHOD_GET_DYNAMIC_TITLE;
import static com.android.settingslib.drawer.SwitchesProvider.METHOD_GET_PROVIDER_ICON;
import static com.android.settingslib.drawer.SwitchesProvider.METHOD_IS_CHECKED;
import static com.android.settingslib.drawer.SwitchesProvider.METHOD_ON_CHECKED_CHANGED;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SWITCH_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE_URI;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.dashboard.profileselector.ProfileSelectDialog;
import com.android.settings.homepage.TopLevelHighlightMixin;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.AdaptiveIcon;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
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
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
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
    public List<DynamicDataObserver> bindPreferenceToTileAndGetObservers(FragmentActivity activity,
            DashboardFragment fragment, boolean forceRoundedIcon, Preference pref, Tile tile,
            String key, int baseOrder) {
        if (pref == null) {
            return null;
        }
        if (!TextUtils.isEmpty(key)) {
            pref.setKey(key);
        } else {
            pref.setKey(getDashboardKeyForTile(tile));
        }
        final List<DynamicDataObserver> outObservers = new ArrayList<>();
        DynamicDataObserver observer = bindTitleAndGetObserver(pref, tile);
        if (observer != null) {
            outObservers.add(observer);
        }
        observer = bindSummaryAndGetObserver(pref, tile);
        if (observer != null) {
            outObservers.add(observer);
        }
        observer = bindSwitchAndGetObserver(pref, tile);
        if (observer != null) {
            outObservers.add(observer);
        }
        bindIcon(pref, tile, forceRoundedIcon);

        if (tile.hasPendingIntent()) {
            // Pending intent cannot be launched within the settings app panel, and will thus always
            // be executed directly.
            pref.setOnPreferenceClickListener(preference -> {
                launchPendingIntentOrSelectProfile(activity, tile, fragment.getMetricsCategory());
                return true;
            });
        } else if (tile instanceof ActivityTile) {
            final int sourceMetricsCategory = fragment.getMetricsCategory();
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
                // Register the rule for injected apps.
                if (fragment instanceof TopLevelSettings) {
                    ActivityEmbeddingRulesController.registerTwoPanePairRuleForSettingsHome(
                            mContext,
                            new ComponentName(tile.getPackageName(), tile.getComponentName()),
                            action,
                            true /* clearTop */);
                }
                pref.setOnPreferenceClickListener(preference -> {
                    TopLevelHighlightMixin highlightMixin = null;
                    boolean isDuplicateClick = false;
                    if (fragment instanceof TopLevelSettings
                            && ActivityEmbeddingUtils.isEmbeddingActivityEnabled(mContext)) {
                        // Highlight the preference whenever it's clicked
                        final TopLevelSettings topLevelSettings = (TopLevelSettings) fragment;
                        highlightMixin = topLevelSettings.getHighlightMixin();
                        isDuplicateClick = topLevelSettings.isDuplicateClick(preference);
                        topLevelSettings.setHighlightPreferenceKey(key);
                    }
                    launchIntentOrSelectProfile(activity, tile, intent, sourceMetricsCategory,
                            highlightMixin, isDuplicateClick);
                    return true;
                });
            }
        }

        if (tile.hasOrder()) {
            final String skipOffsetPackageName = activity.getPackageName();
            final int order = tile.getOrder();
            boolean shouldSkipBaseOrderOffset = TextUtils.equals(
                    skipOffsetPackageName, tile.getIntent().getComponent().getPackageName());
            if (shouldSkipBaseOrderOffset || baseOrder == Preference.DEFAULT_ORDER) {
                pref.setOrder(order);
            } else {
                pref.setOrder(order + baseOrder);
            }
        }
        return outObservers.isEmpty() ? null : outObservers;
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
        launchIntentOrSelectProfile(activity, tile, intent, SettingsEnums.DASHBOARD_SUMMARY,
                /* highlightMixin= */ null, /* isDuplicateClick= */ false);
    }

    private DynamicDataObserver createDynamicDataObserver(String method, Uri uri, Preference pref) {
        return new DynamicDataObserver() {
            @Override
            public Uri getUri() {
                return uri;
            }

            @Override
            public void onDataChanged() {
                switch (method) {
                    case METHOD_GET_DYNAMIC_TITLE:
                        refreshTitle(uri, pref, this);
                        break;
                    case METHOD_GET_DYNAMIC_SUMMARY:
                        refreshSummary(uri, pref, this);
                        break;
                    case METHOD_IS_CHECKED:
                        refreshSwitch(uri, pref, this);
                        break;
                }
            }
        };
    }

    private DynamicDataObserver bindTitleAndGetObserver(Preference preference, Tile tile) {
        final CharSequence title = tile.getTitle(mContext.getApplicationContext());
        if (title != null) {
            preference.setTitle(title);
            return null;
        }
        if (tile.getMetaData() != null && tile.getMetaData().containsKey(
                META_DATA_PREFERENCE_TITLE_URI)) {
            // Set a placeholder title before starting to fetch real title, this is necessary
            // to avoid preference height change.
            if (preference.getTitle() == null) {
                preference.setTitle(R.string.summary_placeholder);
            }

            final Uri uri = TileUtils.getCompleteUri(tile, META_DATA_PREFERENCE_TITLE_URI,
                    METHOD_GET_DYNAMIC_TITLE);
            return createDynamicDataObserver(METHOD_GET_DYNAMIC_TITLE, uri, preference);
        }
        return null;
    }

    private void refreshTitle(Uri uri, Preference preference, DynamicDataObserver observer) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final Map<String, IContentProvider> providerMap = new ArrayMap<>();
            final String titleFromUri = TileUtils.getTextFromUri(
                    mContext, uri, providerMap, META_DATA_PREFERENCE_TITLE);
            if (!TextUtils.equals(titleFromUri, preference.getTitle())) {
                observer.post(() -> preference.setTitle(titleFromUri));
            }
        });
    }

    private DynamicDataObserver bindSummaryAndGetObserver(Preference preference, Tile tile) {
        final CharSequence summary = tile.getSummary(mContext);
        if (summary != null) {
            preference.setSummary(summary);
        } else if (tile.getMetaData() != null
                && tile.getMetaData().containsKey(META_DATA_PREFERENCE_SUMMARY_URI)) {
            // Set a placeholder summary before starting to fetch real summary, this is necessary
            // to avoid preference height change.
            if (preference.getSummary() == null) {
                preference.setSummary(R.string.summary_placeholder);
            }

            final Uri uri = TileUtils.getCompleteUri(tile, META_DATA_PREFERENCE_SUMMARY_URI,
                    METHOD_GET_DYNAMIC_SUMMARY);
            return createDynamicDataObserver(METHOD_GET_DYNAMIC_SUMMARY, uri, preference);
        }
        return null;
    }

    private void refreshSummary(Uri uri, Preference preference, DynamicDataObserver observer) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final Map<String, IContentProvider> providerMap = new ArrayMap<>();
            final String summaryFromUri = TileUtils.getTextFromUri(
                    mContext, uri, providerMap, META_DATA_PREFERENCE_SUMMARY);
            if (!TextUtils.equals(summaryFromUri, preference.getSummary())) {
                observer.post(() -> preference.setSummary(summaryFromUri));
            }
        });
    }

    private DynamicDataObserver bindSwitchAndGetObserver(Preference preference, Tile tile) {
        if (!tile.hasSwitch()) {
            return null;
        }

        final Uri onCheckedChangedUri = TileUtils.getCompleteUri(tile,
                META_DATA_PREFERENCE_SWITCH_URI, METHOD_ON_CHECKED_CHANGED);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            onCheckedChanged(onCheckedChangedUri, pref, (boolean) newValue);
            return true;
        });

        final Uri isCheckedUri = TileUtils.getCompleteUri(tile, META_DATA_PREFERENCE_SWITCH_URI,
                METHOD_IS_CHECKED);
        setSwitchEnabled(preference, false);
        return createDynamicDataObserver(METHOD_IS_CHECKED, isCheckedUri, preference);
    }

    private void onCheckedChanged(Uri uri, Preference pref, boolean checked) {
        setSwitchEnabled(pref, false);
        ThreadUtils.postOnBackgroundThread(() -> {
            final Map<String, IContentProvider> providerMap = new ArrayMap<>();
            final Bundle result = TileUtils.putBooleanToUriAndGetResult(mContext, uri, providerMap,
                    EXTRA_SWITCH_CHECKED_STATE, checked);

            ThreadUtils.postOnMainThread(() -> {
                setSwitchEnabled(pref, true);
                final boolean error = result.getBoolean(EXTRA_SWITCH_SET_CHECKED_ERROR);
                if (!error) {
                    return;
                }

                setSwitchChecked(pref, !checked);
                final String errorMsg = result.getString(EXTRA_SWITCH_SET_CHECKED_ERROR_MESSAGE);
                if (!TextUtils.isEmpty(errorMsg)) {
                    Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void refreshSwitch(Uri uri, Preference preference, DynamicDataObserver observer) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final Map<String, IContentProvider> providerMap = new ArrayMap<>();
            final boolean checked = TileUtils.getBooleanFromUri(mContext, uri, providerMap,
                    EXTRA_SWITCH_CHECKED_STATE);
            observer.post(() -> {
                setSwitchChecked(preference, checked);
                setSwitchEnabled(preference, true);
            });
        });
    }

    private void setSwitchChecked(Preference pref, boolean checked) {
        if (pref instanceof PrimarySwitchPreference primarySwitchPreference) {
            primarySwitchPreference.setChecked(checked);
        } else if (pref instanceof TwoStatePreference twoStatePreference) {
            twoStatePreference.setChecked(checked);
        }
    }

    private void setSwitchEnabled(Preference pref, boolean enabled) {
        if (pref instanceof PrimarySwitchPreference primarySwitchPreference) {
            primarySwitchPreference.setSwitchEnabled(enabled);
        } else {
            pref.setEnabled(enabled);
        }
    }

    @VisibleForTesting
    void bindIcon(Preference preference, Tile tile, boolean forceRoundedIcon) {
        // Icon provided by the content provider overrides any static icon.
        if (tile.getMetaData() != null
                && tile.getMetaData().containsKey(META_DATA_PREFERENCE_ICON_URI)) {
            // Reserve the icon space to avoid preference padding change.
            preference.setIconSpaceReserved(true);

            ThreadUtils.postOnBackgroundThread(() -> {
                final Intent intent = tile.getIntent();
                String packageName = null;
                if (!TextUtils.isEmpty(intent.getPackage())) {
                    packageName = intent.getPackage();
                } else if (intent.getComponent() != null) {
                    packageName = intent.getComponent().getPackageName();
                }
                final Map<String, IContentProvider> providerMap = new ArrayMap<>();
                final Uri uri = TileUtils.getCompleteUri(tile, META_DATA_PREFERENCE_ICON_URI,
                        METHOD_GET_PROVIDER_ICON);
                final Pair<String, Integer> iconInfo = TileUtils.getIconFromUri(
                        mContext, packageName, uri, providerMap);
                if (iconInfo == null) {
                    Log.w(TAG, "Failed to get icon from uri " + uri);
                    return;
                }
                final Icon icon = Icon.createWithResource(iconInfo.first, iconInfo.second);
                ThreadUtils.postOnMainThread(() -> {
                    setPreferenceIcon(preference, tile, forceRoundedIcon, iconInfo.first, icon);
                });
            });
            return;
        }

        // Use preference context instead here when get icon from Tile, as we are using the context
        // to get the style to tint the icon. Using mContext here won't get the correct style.
        final Icon tileIcon = tile.getIcon(preference.getContext());
        if (tileIcon == null) {
            return;
        }
        setPreferenceIcon(preference, tile, forceRoundedIcon, tile.getPackageName(), tileIcon);
    }

    private void setPreferenceIcon(Preference preference, Tile tile, boolean forceRoundedIcon,
            String iconPackage, Icon icon) {
        Drawable iconDrawable = icon.loadDrawable(preference.getContext());
        if (iconDrawable == null) {
            Log.w(TAG, "Set null preference icon for: " + iconPackage);
            preference.setIcon(null);
            return;
        }
        if (TextUtils.equals(tile.getCategory(), CategoryKey.CATEGORY_HOMEPAGE)) {
            iconDrawable.setTint(Utils.getHomepageIconColor(preference.getContext()));
        } else if (forceRoundedIcon && !TextUtils.equals(mContext.getPackageName(), iconPackage)) {
            iconDrawable = new AdaptiveIcon(mContext, iconDrawable,
                    R.dimen.dashboard_tile_foreground_image_inset);
            ((AdaptiveIcon) iconDrawable).setBackgroundColor(mContext, tile);
        }
        preference.setIcon(iconDrawable);
    }

    private void launchPendingIntentOrSelectProfile(FragmentActivity activity, Tile tile,
            int sourceMetricCategory) {
        ProfileSelectDialog.updatePendingIntentsIfNeeded(mContext, tile);

        if (tile.pendingIntentMap.isEmpty()) {
            Log.w(TAG, "Cannot resolve pendingIntent, skipping. " + tile.getIntent());
            return;
        }

        mMetricsFeatureProvider.logSettingsTileClick(tile.getKey(mContext), sourceMetricCategory);

        // Launch the pending intent directly if there's only one available.
        if (tile.pendingIntentMap.size() == 1) {
            PendingIntent pendingIntent = Iterables.getOnlyElement(tile.pendingIntentMap.values());
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Failed executing pendingIntent. " + pendingIntent.getIntent(), e);
            }
            return;
        }

        ProfileSelectDialog.show(activity.getSupportFragmentManager(), tile,
                sourceMetricCategory, /* onShowListener= */ null,
                /* onDismissListener= */ null, /* onCancelListener= */ null);
    }

    private void launchIntentOrSelectProfile(FragmentActivity activity, Tile tile, Intent intent,
            int sourceMetricCategory, TopLevelHighlightMixin highlightMixin,
            boolean isDuplicateClick) {
        if (!isIntentResolvable(intent)) {
            Log.w(TAG, "Cannot resolve intent, skipping. " + intent);
            return;
        }
        ProfileSelectDialog.updateUserHandlesIfNeeded(mContext, tile);

        if (tile.userHandle == null || tile.isPrimaryProfileOnly()) {
            if (!isDuplicateClick) {
                mMetricsFeatureProvider.logStartedIntent(intent, sourceMetricCategory);
                activity.startActivity(intent);
            }
        } else if (tile.userHandle.size() == 1) {
            if (!isDuplicateClick) {
                mMetricsFeatureProvider.logStartedIntent(intent, sourceMetricCategory);
                activity.startActivityAsUser(intent, tile.userHandle.get(0));
            }
        } else {
            final UserHandle userHandle = intent.getParcelableExtra(EXTRA_USER);
            if (userHandle != null && tile.userHandle.contains(userHandle)) {
                if (!isDuplicateClick) {
                    mMetricsFeatureProvider.logStartedIntent(intent, sourceMetricCategory);
                    activity.startActivityAsUser(intent, userHandle);
                }
                return;
            }

            final List<UserHandle> resolvableUsers = getResolvableUsers(intent, tile);
            if (resolvableUsers.size() == 1) {
                if (!isDuplicateClick) {
                    mMetricsFeatureProvider.logStartedIntent(intent, sourceMetricCategory);
                    activity.startActivityAsUser(intent, resolvableUsers.get(0));
                }
                return;
            }

            // Show the profile select dialog regardless of the duplicate click.
            mMetricsFeatureProvider.logStartedIntent(intent, sourceMetricCategory);
            ProfileSelectDialog.show(activity.getSupportFragmentManager(), tile,
                    sourceMetricCategory, /* onShowListener= */ highlightMixin,
                    /* onDismissListener= */ highlightMixin,
                    /* onCancelListener= */ highlightMixin);
        }
    }

    private boolean isIntentResolvable(Intent intent) {
        return mPackageManager.resolveActivity(intent, 0) != null;
    }

    private List<UserHandle> getResolvableUsers(Intent intent, Tile tile) {
        final ArrayList<UserHandle> eligibleUsers = new ArrayList<>();
        for (UserHandle user : tile.userHandle) {
            if (mPackageManager.resolveActivityAsUser(intent, 0, user.getIdentifier()) != null) {
                eligibleUsers.add(user);
            }
        }
        return eligibleUsers;
    }
}
