/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.util.ArraySet;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.codeinspection.CodeInspector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link CodeInspector} to ensure fragments implement search components correctly.
 */
public class SearchIndexProviderCodeInspector extends CodeInspector {
    private static final String TAG = "SearchCodeInspector";

    private final List<String> notImplementingIndexableWhitelist;
    private final List<String> notImplementingIndexProviderWhitelist;

    public SearchIndexProviderCodeInspector(List<Class<?>> classes) {
        super(classes);
        notImplementingIndexableWhitelist = new ArrayList<>();
        notImplementingIndexProviderWhitelist = new ArrayList<>();
        initializeGrandfatherList(notImplementingIndexableWhitelist,
                "grandfather_not_implementing_indexable");
        initializeGrandfatherList(notImplementingIndexProviderWhitelist,
                "grandfather_not_implementing_index_provider");
    }

    @Override
    public void run() {
        final Set<String> notImplementingIndexable = new ArraySet<>();
        final Set<String> notImplementingIndexProvider = new ArraySet<>();

        for (Class clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                continue;
            }
            final String className = clazz.getName();
            // Skip fragments if it's not SettingsPreferenceFragment.
            if (!SettingsPreferenceFragment.class.isAssignableFrom(clazz)) {
                continue;
            }
            // If it's a SettingsPreferenceFragment, it must also be Indexable.
            final boolean implementsIndexable = Indexable.class.isAssignableFrom(clazz);
            if (!implementsIndexable && !notImplementingIndexableWhitelist.contains(className)) {
                notImplementingIndexable.add(className);
            }
            // If it implements Indexable, it must also implement the index provider field.
            if (implementsIndexable && !hasSearchIndexProvider(clazz)
                    && !notImplementingIndexProviderWhitelist.contains(className)) {
                notImplementingIndexProvider.add(className);
            }
        }

        // Build error messages
        final StringBuilder indexableError = new StringBuilder(
                "SettingsPreferenceFragment should implement Indexable, but these are not:\n");
        for (String c : notImplementingIndexable) {
            indexableError.append(c).append("\n");
        }
        final StringBuilder indexProviderError = new StringBuilder(
                "Indexable should have public field " + Index.FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER
                        + " but these are not:\n");
        for (String c : notImplementingIndexProvider) {
            indexProviderError.append(c).append("\n");
        }

        assertWithMessage(indexableError.toString())
                .that(notImplementingIndexable.isEmpty())
                .isTrue();
        assertWithMessage(indexProviderError.toString())
                .that(notImplementingIndexProvider.isEmpty())
                .isTrue();
    }

    private boolean hasSearchIndexProvider(Class clazz) {
        try {
            final Field f = clazz.getField(Index.FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return f != null;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "error fetching search provider from class " + clazz.getName());
            return false;
        }
    }
}
