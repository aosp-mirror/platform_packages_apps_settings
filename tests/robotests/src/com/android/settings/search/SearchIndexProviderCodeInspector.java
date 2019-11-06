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

import static com.google.common.truth.Truth.assertWithMessage;

import android.provider.SearchIndexableResource;
import android.util.ArraySet;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.dashboard.DashboardFragmentSearchIndexProviderInspector;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableData;
import com.android.settingslib.search.SearchIndexableResources;

import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link CodeInspector} to ensure fragments implement search components correctly.
 */
public class SearchIndexProviderCodeInspector extends CodeInspector {
    private static final String TAG = "SearchCodeInspector";

    private static final String NOT_CONTAINING_PROVIDER_OBJECT_ERROR =
            "Indexable should have public field "
                    + DatabaseIndexingUtils.FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER
                    + " but these are not:\n";
    private static final String NOT_SHARING_PREF_CONTROLLERS_BETWEEN_FRAG_AND_PROVIDER =
            "DashboardFragment should share pref controllers with its SearchIndexProvider, but "
                    + " these are not: \n";
    private static final String NOT_IN_INDEXABLE_PROVIDER_REGISTRY =
            "Class containing " + DatabaseIndexingUtils.FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER
                    + " must be added to " + SearchIndexableResources.class.getName()
                    + " but these are not: \n";
    private static final String NOT_PROVIDING_VALID_RESOURCE_ERROR =
            "SearchIndexableProvider must either provide no resource to index, or valid ones. "
                    + "But the followings contain resource with xml id = 0\n";

    private final List<String> notImplementingIndexProviderGrandfatherList;
    private final List<String> notInSearchIndexableRegistryGrandfatherList;
    private final List<String> notSharingPrefControllersGrandfatherList;

    public SearchIndexProviderCodeInspector(List<Class<?>> classes) {
        super(classes);
        notImplementingIndexProviderGrandfatherList = new ArrayList<>();
        notInSearchIndexableRegistryGrandfatherList = new ArrayList<>();
        notSharingPrefControllersGrandfatherList = new ArrayList<>();
        initializeGrandfatherList(notImplementingIndexProviderGrandfatherList,
                "grandfather_not_implementing_index_provider");
        initializeGrandfatherList(notInSearchIndexableRegistryGrandfatherList,
                "grandfather_not_in_search_index_provider_registry");
        initializeGrandfatherList(notSharingPrefControllersGrandfatherList,
                "grandfather_not_sharing_pref_controllers_with_search_provider");
    }

    @Override
    public void run() {
        final Set<String> notImplementingIndexProvider = new ArraySet<>();
        final Set<String> notInSearchProviderRegistry = new ArraySet<>();
        final Set<String> notSharingPreferenceControllers = new ArraySet<>();
        final Set<String> notProvidingValidResource = new ArraySet<>();
        final Set<Class> providerClasses = new ArraySet<>();

        final SearchFeatureProvider provider = new SearchFeatureProviderImpl();
        for (SearchIndexableData bundle :
                provider.getSearchIndexableResources().getProviderValues()) {
            providerClasses.add(bundle.getTargetClass());
        }

        for (Class clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                continue;
            }
            final String className = clazz.getName();
            // Skip fragments if it's not SettingsPreferenceFragment.
            if (!SettingsPreferenceFragment.class.isAssignableFrom(clazz)) {
                continue;
            }
            final boolean hasSearchIndexProvider = hasSearchIndexProvider(clazz);
            // If it implements Indexable, it must also implement the index provider field.
            if (!hasSearchIndexProvider) {
                if (!notImplementingIndexProviderGrandfatherList.remove(className)) {
                    notImplementingIndexProvider.add(className);
                }
                continue;
            }
            // If it implements index provider field AND it's a DashboardFragment, its fragment and
            // search provider must share the same set of PreferenceControllers.
            final boolean isSharingPrefControllers = DashboardFragmentSearchIndexProviderInspector
                    .isSharingPreferenceControllers(clazz);
            if (!isSharingPrefControllers) {
                if (!notSharingPrefControllersGrandfatherList.remove(className)) {
                    notSharingPreferenceControllers.add(className);
                }
                continue;
            }
            // Must be in SearchProviderRegistry
            if (!providerClasses.contains(clazz)) {
                if (!notInSearchIndexableRegistryGrandfatherList.remove(className)) {
                    notInSearchProviderRegistry.add(className);
                }
            }
            // Search provider must either don't provider resource xml, or provide valid ones.
            if (!hasValidResourceFromProvider(clazz)) {
                notProvidingValidResource.add(className);
            }
        }

        // Build error messages
        final String indexProviderError = buildErrorMessage(NOT_CONTAINING_PROVIDER_OBJECT_ERROR,
                notImplementingIndexProvider);
        final String notSharingPrefControllerError = buildErrorMessage(
                NOT_SHARING_PREF_CONTROLLERS_BETWEEN_FRAG_AND_PROVIDER,
                notSharingPreferenceControllers);
        final String notInProviderRegistryError =
                buildErrorMessage(NOT_IN_INDEXABLE_PROVIDER_REGISTRY, notInSearchProviderRegistry);
        final String notProvidingValidResourceError = buildErrorMessage(
                NOT_PROVIDING_VALID_RESOURCE_ERROR, notProvidingValidResource);
        assertWithMessage(indexProviderError)
                .that(notImplementingIndexProvider)
                .isEmpty();
        assertWithMessage(notSharingPrefControllerError)
                .that(notSharingPreferenceControllers)
                .isEmpty();
        assertWithMessage(notInProviderRegistryError)
                .that(notInSearchProviderRegistry)
                .isEmpty();
        assertWithMessage(notProvidingValidResourceError)
                .that(notProvidingValidResource)
                .isEmpty();
        assertNoObsoleteInGrandfatherList("grandfather_not_implementing_index_provider",
                notImplementingIndexProviderGrandfatherList);
        assertNoObsoleteInGrandfatherList("grandfather_not_in_search_index_provider_registry",
                notInSearchIndexableRegistryGrandfatherList);
        assertNoObsoleteInGrandfatherList(
                "grandfather_not_sharing_pref_controllers_with_search_provider",
                notSharingPrefControllersGrandfatherList);
    }

    private boolean hasSearchIndexProvider(Class clazz) {
        try {
            final Field f = clazz.getField(
                    DatabaseIndexingUtils.FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return f != null;
        } catch (NoClassDefFoundError e) {
            // Cannot find class def, ignore
            return true;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "error fetching search provider from class " + clazz.getName());
            return false;
        }
    }

    private boolean hasValidResourceFromProvider(Class clazz) {
        try {
            final Indexable.SearchIndexProvider provider =
                    DatabaseIndexingUtils.getSearchIndexProvider(clazz);
            final List<SearchIndexableResource> resources = provider.getXmlResourcesToIndex(
                    RuntimeEnvironment.application, true /* enabled */);
            if (resources == null) {
                // No resource, that's fine.
                return true;
            }
            for (SearchIndexableResource res : resources) {
                if (res.xmlResId == 0) {
                    // Invalid resource
                    return false;
                }
            }
        } catch (Exception e) {
            // Ignore.
        }
        return true;
    }

    private String buildErrorMessage(String errorSummary, Set<String> errorClasses) {
        final StringBuilder error = new StringBuilder(errorSummary);
        for (String c : errorClasses) {
            error.append(c).append("\n");
        }
        return error.toString();
    }
}
