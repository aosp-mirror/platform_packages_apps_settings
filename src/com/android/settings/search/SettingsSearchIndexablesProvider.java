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

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;
import android.util.ArraySet;
import android.util.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    public static final boolean DEBUG = false;
    private static final String TAG = "SettingsSearchProvider";

    private static final Collection<String> INVALID_KEYS;

    static {
        INVALID_KEYS = new ArraySet<>();
        INVALID_KEYS.add(null);
        INVALID_KEYS.add("");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        Collection<SearchIndexableResource> values = SearchIndexableResources.values();
        for (SearchIndexableResource val : values) {
            Object[] ref = new Object[INDEXABLES_XML_RES_COLUMNS.length];
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

    @Override
    public Cursor queryRawData(String[] projection) {
        MatrixCursor result = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        return result;
    }

    /**
     * Gets a combined list non-indexable keys that come from providers inside of settings.
     * The non-indexable keys are used in Settings search at both index and update time to verify
     * the validity of results in the database.
     */
    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
        final Collection<String> values = new HashSet<>();
        final Context context = getContext();

        for (SearchIndexableResource sir : SearchIndexableResources.values()) {
            if (DEBUG) {
                Log.d(TAG, "Getting non-indexable from " + sir.className);
            }
            final long startTime = System.currentTimeMillis();
            final Class<?> clazz = DatabaseIndexingUtils.getIndexableClass(sir.className);
            if (clazz == null) {
                Log.d(TAG, "SearchIndexableResource '" + sir.className +
                        "' should implement the " + Indexable.class.getName() + " interface!");
                continue;
            }

            final Indexable.SearchIndexProvider provider =
                    DatabaseIndexingUtils.getSearchIndexProvider(clazz);

            if (provider == null) {
                Log.d(TAG, "Unable to get SearchIndexableProvider from " + clazz);
                continue;
            }

            List<String> providerNonIndexableKeys = provider.getNonIndexableKeys(context);

            if (providerNonIndexableKeys == null || providerNonIndexableKeys.isEmpty()) {
                if (DEBUG) {
                    final long totalTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "No indexable, total time " + totalTime);
                }
                continue;
            }

            if (providerNonIndexableKeys.removeAll(INVALID_KEYS)) {
                Log.v(TAG, clazz.getName() + " tried to add an empty non-indexable key");
            }
            if (DEBUG) {
                final long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Non-indexables " + providerNonIndexableKeys.size() + ", total time "
                        + totalTime);
            }
            values.addAll(providerNonIndexableKeys);
        }

        for (String nik : values) {

            final Object[] ref = new Object[NON_INDEXABLES_KEYS_COLUMNS.length];
            ref[COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE] = nik;
            cursor.addRow(ref);
        }
        return cursor;
    }
}
