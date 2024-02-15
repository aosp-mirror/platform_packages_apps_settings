/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;
import static android.provider.SearchIndexablesContract.SITE_MAP_COLUMNS;
import static android.provider.SearchIndexablesContract.SLICE_URI_PAIRS_COLUMNS;

import static com.android.settings.dashboard.DashboardFragmentRegistry.CATEGORY_KEY_TO_PARENT_MAP;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.slice.SliceViewManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.CategoryManager;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFragmentRegistry;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableData;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {

    public static final boolean DEBUG = false;

    /**
     * Flag for a system property which checks if we should crash if there are issues in the
     * indexing pipeline.
     */
    public static final String SYSPROP_CRASH_ON_ERROR =
            "debug.com.android.settings.search.crash_on_error";

    private static final String TAG = "SettingsSearchProvider";

    private static final Collection<String> INVALID_KEYS;

    // Search enabled states for injection (key: category key, value: search enabled)
    private Map<String, Boolean> mSearchEnabledByCategoryKeyMap;

    static {
        INVALID_KEYS = new ArraySet<>();
        INVALID_KEYS.add(null);
        INVALID_KEYS.add("");
    }

    @Override
    public boolean onCreate() {
        mSearchEnabledByCategoryKeyMap = new ArrayMap<>();
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        final MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        final List<SearchIndexableResource> resources =
                getSearchIndexableResourcesFromProvider(getContext());
        for (SearchIndexableResource val : resources) {
            final Object[] ref = new Object[INDEXABLES_XML_RES_COLUMNS.length];
            ref[COLUMN_INDEX_XML_RES_RANK] = val.rank;
            ref[COLUMN_INDEX_XML_RES_RESID] = val.xmlResId;
            ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = val.className;
            ref[COLUMN_INDEX_XML_RES_ICON_RESID] = val.iconResId;
            ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = val.intentAction;
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = val.intentTargetPackage;
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = null; // intent target class
            cursor.addRow(ref);
        }

        return cursor;
    }

    /**
     * Gets a Cursor of RawData. We use those data in search indexing time
     */
    @Override
    public Cursor queryRawData(String[] projection) {
        final MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        final List<SearchIndexableRaw> raws = getSearchIndexableRawFromProvider(getContext());
        for (SearchIndexableRaw val : raws) {
            cursor.addRow(createIndexableRawColumnObjects(val));
        }

        return cursor;
    }

    /**
     * Gets a combined list non-indexable keys that come from providers inside of settings.
     * The non-indexable keys are used in Settings search at both index and update time to verify
     * the validity of results in the database.
     */
    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        final MatrixCursor cursor = new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
        final List<String> nonIndexableKeys = getNonIndexableKeysFromProvider(getContext());
        for (String nik : nonIndexableKeys) {
            final Object[] ref = new Object[NON_INDEXABLES_KEYS_COLUMNS.length];
            ref[COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE] = nik;
            cursor.addRow(ref);
        }

        return cursor;
    }

    /**
     * Gets a Cursor of dynamic Raw data similar to queryRawData. We use those data in search query
     * time
     */
    @Nullable
    @Override
    public Cursor queryDynamicRawData(String[] projection) {
        final Context context = getContext();
        final List<SearchIndexableRaw> rawList = new ArrayList<>();
        final Collection<SearchIndexableData> bundles = FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();

        for (SearchIndexableData bundle : bundles) {
            rawList.addAll(getDynamicSearchIndexableRawData(context, bundle));

            // Refresh the search enabled state for indexing injection raw data
            final Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();
            if (provider instanceof BaseSearchIndexProvider) {
                refreshSearchEnabledState(context, (BaseSearchIndexProvider) provider);
            }
        }
        rawList.addAll(getInjectionIndexableRawData(context));

        final MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        for (SearchIndexableRaw raw : rawList) {
            cursor.addRow(createIndexableRawColumnObjects(raw));
        }

        return cursor;
    }

    @Override
    public Cursor querySiteMapPairs() {
        final MatrixCursor cursor = new MatrixCursor(SITE_MAP_COLUMNS);
        final Context context = getContext();
        // Loop through all IA categories and pages and build additional SiteMapPairs
        final List<DashboardCategory> categories = FeatureFactory.getFeatureFactory()
                .getDashboardFeatureProvider().getAllCategories();
        for (DashboardCategory category : categories) {
            // Use the category key to look up parent (which page hosts this key)
            final String parentClass = CATEGORY_KEY_TO_PARENT_MAP.get(category.key);
            if (parentClass == null) {
                continue;
            }
            // Build parent-child class pairs for all children listed under this key.
            for (Tile tile : category.getTiles()) {
                String childClass = null;
                CharSequence childTitle = "";
                if (tile.getMetaData() != null) {
                    childClass = tile.getMetaData().getString(
                            SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
                }
                if (childClass == null) {
                    childClass = tile.getComponentName();
                    childTitle = tile.getTitle(getContext());
                }
                if (childClass == null) {
                    continue;
                }
                cursor.newRow()
                        .add(SearchIndexablesContract.SiteMapColumns.PARENT_CLASS, parentClass)
                        .add(SearchIndexablesContract.SiteMapColumns.CHILD_CLASS, childClass)
                        .add(SearchIndexablesContract.SiteMapColumns.CHILD_TITLE, childTitle);
            }
        }

        // Loop through custom site map registry to build additional SiteMapPairs
        for (String childClass : CustomSiteMapRegistry.CUSTOM_SITE_MAP.keySet()) {
            final String parentClass = CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(childClass);
            cursor.newRow()
                    .add(SearchIndexablesContract.SiteMapColumns.PARENT_CLASS, parentClass)
                    .add(SearchIndexablesContract.SiteMapColumns.CHILD_CLASS, childClass);
        }
        // Done.
        return cursor;
    }

    @Override
    public Cursor querySliceUriPairs() {
        final SliceViewManager manager = SliceViewManager.getInstance(getContext());
        final MatrixCursor cursor = new MatrixCursor(SLICE_URI_PAIRS_COLUMNS);
        final String queryUri = getContext().getString(R.string.config_non_public_slice_query_uri);
        final Uri baseUri = !TextUtils.isEmpty(queryUri) ? Uri.parse(queryUri)
                : new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                        .build();

        final Uri platformBaseUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(SettingsSlicesContract.AUTHORITY)
                        .build();

        final Collection<Uri> sliceUris = manager.getSliceDescendants(baseUri);
        sliceUris.addAll(manager.getSliceDescendants(platformBaseUri));

        for (Uri uri : sliceUris) {
            cursor.newRow()
                    .add(SearchIndexablesContract.SliceUriPairColumns.KEY, uri.getLastPathSegment())
                    .add(SearchIndexablesContract.SliceUriPairColumns.SLICE_URI, uri);
        }

        return cursor;
    }

    private List<String> getNonIndexableKeysFromProvider(Context context) {
        final Collection<SearchIndexableData> bundles = FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();

        final List<String> nonIndexableKeys = new ArrayList<>();

        for (SearchIndexableData bundle : bundles) {
            final long startTime = System.currentTimeMillis();
            Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();
            List<String> providerNonIndexableKeys;
            try {
                providerNonIndexableKeys = provider.getNonIndexableKeys(context);
            } catch (Exception e) {
                // Catch a generic crash. In the absence of the catch, the background thread will
                // silently fail anyway, so we aren't losing information by catching the exception.
                // We crash when the system property exists so that we can test if crashes need to
                // be fixed.
                // The gain is that if there is a crash in a specific controller, we don't lose all
                // non-indexable keys, but we can still find specific crashes in development.
                if (System.getProperty(SYSPROP_CRASH_ON_ERROR) != null) {
                    throw new RuntimeException(e);
                }
                Log.e(TAG, "Error trying to get non-indexable keys from: "
                        + bundle.getTargetClass().getName(), e);
                continue;
            }

            if (providerNonIndexableKeys == null || providerNonIndexableKeys.isEmpty()) {
                if (DEBUG) {
                    final long totalTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "No indexable, total time " + totalTime);
                }
                continue;
            }

            if (providerNonIndexableKeys.removeAll(INVALID_KEYS)) {
                Log.v(TAG, provider + " tried to add an empty non-indexable key");
            }

            if (DEBUG) {
                final long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Non-indexables " + providerNonIndexableKeys.size() + ", total time "
                        + totalTime);
            }

            nonIndexableKeys.addAll(providerNonIndexableKeys);
        }

        return nonIndexableKeys;
    }

    private List<SearchIndexableResource> getSearchIndexableResourcesFromProvider(Context context) {
        final Collection<SearchIndexableData> bundles = FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();
        List<SearchIndexableResource> resourceList = new ArrayList<>();

        for (SearchIndexableData bundle : bundles) {
            Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();
            final List<SearchIndexableResource> resList =
                    provider.getXmlResourcesToIndex(context, true);

            if (resList == null) {
                continue;
            }

            for (SearchIndexableResource item : resList) {
                item.className = TextUtils.isEmpty(item.className)
                        ? bundle.getTargetClass().getName()
                        : item.className;
            }

            resourceList.addAll(resList);
        }

        return resourceList;
    }

    private List<SearchIndexableRaw> getSearchIndexableRawFromProvider(Context context) {
        final Collection<SearchIndexableData> bundles = FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();
        final List<SearchIndexableRaw> rawList = new ArrayList<>();

        for (SearchIndexableData bundle : bundles) {
            Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();
            final List<SearchIndexableRaw> providerRaws = provider.getRawDataToIndex(context,
                    true /* enabled */);

            if (providerRaws == null) {
                continue;
            }

            for (SearchIndexableRaw raw : providerRaws) {
                // The classname and intent information comes from the PreIndexData
                // This will be more clear when provider conversion is done at PreIndex time.
                raw.className = bundle.getTargetClass().getName();
            }
            rawList.addAll(providerRaws);
        }

        return rawList;
    }

    private List<SearchIndexableRaw> getDynamicSearchIndexableRawData(Context context,
            SearchIndexableData bundle) {
        final Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();
        final List<SearchIndexableRaw> providerRaws =
                provider.getDynamicRawDataToIndex(context, true /* enabled */);
        if (providerRaws == null) {
            return new ArrayList<>();
        }

        for (SearchIndexableRaw raw : providerRaws) {
            // The classname and intent information comes from the PreIndexData
            // This will be more clear when provider conversion is done at PreIndex time.
            raw.className = bundle.getTargetClass().getName();
        }
        return providerRaws;
    }

    @VisibleForTesting
    List<SearchIndexableRaw> getInjectionIndexableRawData(Context context) {
        final DashboardFeatureProvider dashboardFeatureProvider =
                FeatureFactory.getFeatureFactory().getDashboardFeatureProvider();
        final List<SearchIndexableRaw> rawList = new ArrayList<>();
        final String currentPackageName = context.getPackageName();
        for (DashboardCategory category : dashboardFeatureProvider.getAllCategories()) {
            if (mSearchEnabledByCategoryKeyMap.containsKey(category.key)
                    && !mSearchEnabledByCategoryKeyMap.get(category.key)) {
                Log.i(TAG, "Skip indexing category: " + category.key);
                continue;
            }
            for (Tile tile : category.getTiles()) {
                if (!isEligibleForIndexing(currentPackageName, tile)) {
                    continue;
                }
                final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                final CharSequence title = tile.getTitle(context);
                raw.title = TextUtils.isEmpty(title) ? null : title.toString();
                if (TextUtils.isEmpty(raw.title)) {
                    continue;
                }
                raw.key = dashboardFeatureProvider.getDashboardKeyForTile(tile);
                final CharSequence summary = tile.getSummary(context);
                raw.summaryOn = TextUtils.isEmpty(summary) ? null : summary.toString();
                raw.summaryOff = raw.summaryOn;
                raw.className = CATEGORY_KEY_TO_PARENT_MAP.get(tile.getCategory());
                rawList.add(raw);
            }
        }

        return rawList;
    }

    @VisibleForTesting
    void refreshSearchEnabledState(Context context, BaseSearchIndexProvider provider) {
        // Provider's class name is like "com.android.settings.Settings$1"
        String className = provider.getClass().getName();
        final int delimiter = className.lastIndexOf("$");
        if (delimiter > 0) {
            // Parse the outer class name of this provider
            className = className.substring(0, delimiter);
        }

        // Lookup the category key by the class name
        final String categoryKey = DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP
                .get(className);
        if (categoryKey == null) {
            return;
        }

        final DashboardCategory category = CategoryManager.get(context)
                .getTilesByCategory(context, categoryKey);
        if (category != null) {
            mSearchEnabledByCategoryKeyMap.put(category.key, provider.isPageSearchEnabled(context));
        }
    }

    @VisibleForTesting
    boolean isEligibleForIndexing(String packageName, Tile tile) {
        if (TextUtils.equals(packageName, tile.getPackageName())
                && tile instanceof ActivityTile) {
            // Skip Settings injected items because they should be indexed in the sub-pages.
            return false;
        }
        return tile.isSearchable();
    }

    private static Object[] createIndexableRawColumnObjects(SearchIndexableRaw raw) {
        final Object[] ref = new Object[INDEXABLES_RAW_COLUMNS.length];
        ref[COLUMN_INDEX_RAW_TITLE] = raw.title;
        ref[COLUMN_INDEX_RAW_SUMMARY_ON] = raw.summaryOn;
        ref[COLUMN_INDEX_RAW_SUMMARY_OFF] = raw.summaryOff;
        ref[COLUMN_INDEX_RAW_ENTRIES] = raw.entries;
        ref[COLUMN_INDEX_RAW_KEYWORDS] = raw.keywords;
        ref[COLUMN_INDEX_RAW_SCREEN_TITLE] = raw.screenTitle;
        ref[COLUMN_INDEX_RAW_CLASS_NAME] = raw.className;
        ref[COLUMN_INDEX_RAW_ICON_RESID] = raw.iconResId;
        ref[COLUMN_INDEX_RAW_INTENT_ACTION] = raw.intentAction;
        ref[COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE] = raw.intentTargetPackage;
        ref[COLUMN_INDEX_RAW_INTENT_TARGET_CLASS] = raw.intentTargetClass;
        ref[COLUMN_INDEX_RAW_KEY] = raw.key;
        ref[COLUMN_INDEX_RAW_USER_ID] = raw.userId;
        return ref;
    }
}
