/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Utility class for {@like DatabaseIndexingManager} to handle the mapping between Payloads
 * and Preference controllers, and managing indexable classes.
 */
public class DatabaseIndexingUtils {

    private static final String TAG = "IndexingUtil";

    public static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    /**
     * Builds intent that launches the search destination as a sub-setting.
     */
    public static Intent buildSearchResultPageIntent(Context context, String className, String key,
            String screenTitle) {
        return buildSearchResultPageIntent(context, className, key, screenTitle,
                MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS);
    }

    public static Intent  buildSearchResultPageIntent(Context context, String className, String key,
            String screenTitle, int sourceMetricsCategory) {
        final Bundle args = new Bundle();
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
        final Intent searchDestination = new SubSettingLauncher(context)
                .setDestination(className)
                .setArguments(args)
                .setTitle(screenTitle)
                .setSourceMetricsCategory(sourceMetricsCategory)
                .toIntent();
        searchDestination.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key)
                .setAction("com.android.settings.SEARCH_RESULT_TRAMPOLINE")
                .setComponent(null);
        return searchDestination;
    }

    /**
     * @param className which wil provide the map between from {@link Uri}s to
     *                  {@link PreferenceControllerMixin}
     * @return A map between {@link Uri}s and {@link PreferenceControllerMixin}s to get the payload
     * types for Settings.
     */
    public static Map<String, ResultPayload> getPayloadKeyMap(String className, Context context) {
        ArrayMap<String, ResultPayload> map = new ArrayMap<>();
        if (context == null) {
            return map;
        }

        final Class<?> clazz = getIndexableClass(className);

        if (clazz == null) {
            Log.d(TAG, "SearchIndexableResource '" + className +
                    "' should implement the " + Indexable.class.getName() + " interface!");
            return map;
        }

        // Will be non null only for a Local provider implementing a
        // SEARCH_INDEX_DATA_PROVIDER field
        final Indexable.SearchIndexProvider provider = getSearchIndexProvider(clazz);

        final List<AbstractPreferenceController> controllers =
                provider.getPreferenceControllers(context);

        if (controllers == null) {
            return map;
        }

        for (AbstractPreferenceController controller : controllers) {
            ResultPayload payload;
            if (controller instanceof PreferenceControllerMixin) {
                payload = ((PreferenceControllerMixin) controller).getResultPayload();

            } else if (controller instanceof BasePreferenceController) {
                payload = ((BasePreferenceController) controller).getResultPayload();
            } else {
                throw new IllegalStateException(controller.getClass().getName()
                        + " must implement " + PreferenceControllerMixin.class.getName());
            }
            if (payload != null) {
                map.put(controller.getPreferenceKey(), payload);
            }
        }

        return map;
    }

    public static Class<?> getIndexableClass(String className) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Cannot find class: " + className);
            return null;
        }
        return isIndexableClass(clazz) ? clazz : null;
    }

    public static boolean isIndexableClass(final Class<?> clazz) {
        return (clazz != null) && Indexable.class.isAssignableFrom(clazz);
    }

    public static Indexable.SearchIndexProvider getSearchIndexProvider(final Class<?> clazz) {
        try {
            final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return (Indexable.SearchIndexProvider) f.get(null);
        } catch (NoSuchFieldException e) {
            Log.d(TAG, "Cannot find field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (SecurityException se) {
            Log.d(TAG, "Security exception for field '" +
                    FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Illegal access to field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Illegal argument when accessing field '" +
                    FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        }
        return null;
    }
}