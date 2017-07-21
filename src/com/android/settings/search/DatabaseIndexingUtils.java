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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for {@like DatabaseIndexingManager} to handle the mapping between Payloads
 * and Preference controllers, and managing indexable classes.
 */
public class DatabaseIndexingUtils {

    private static final String TAG = "IndexingUtil";

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String EMPTY = "";
    private static final String LIST_DELIMITERS = "[,]\\s*";
    private static final String HYPHEN = "-";
    private static final String SPACE = " ";

    private static final Pattern REMOVE_DIACRITICALS_PATTERN
            = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Builds intent into a subsetting.
     */
    public static Intent buildSubsettingIntent(Context context, String className, String key,
            String screenTitle) {
        final Bundle args = new Bundle();
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
        return Utils.onBuildStartFragmentIntent(context,
                className, args, null, 0, screenTitle, false,
                MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS);
    }

    /**
     * @param className which wil provide the map between from {@link Uri}s to
     * {@link PreferenceControllerMixin}
     * @param context
     * @return A map between {@link Uri}s and {@link PreferenceControllerMixin}s to get the payload
     * types for Settings.
     */
    public static Map<String, PreferenceControllerMixin> getPreferenceControllerUriMap(
            String className, Context context) {
        if (context == null) {
            return null;
        }

        final Class<?> clazz = getIndexableClass(className);

        if (clazz == null) {
            Log.d(TAG, "SearchIndexableResource '" + className +
                    "' should implement the " + Indexable.class.getName() + " interface!");
            return null;
        }

        // Will be non null only for a Local provider implementing a
        // SEARCH_INDEX_DATA_PROVIDER field
        final Indexable.SearchIndexProvider provider = getSearchIndexProvider(clazz);

        List<AbstractPreferenceController> controllers =
                provider.getPreferenceControllers(context);

        if (controllers == null ) {
            return null;
        }

        ArrayMap<String, PreferenceControllerMixin> map = new ArrayMap<>();

        for (AbstractPreferenceController controller : controllers) {
            if (controller instanceof PreferenceControllerMixin) {
                map.put(controller.getPreferenceKey(), (PreferenceControllerMixin) controller);
            } else {
                throw new IllegalStateException(controller.getClass().getName()
                        + " must implement " + PreferenceControllerMixin.class.getName());
            }
        }

        return map;
    }

    /**
     * @param uriMap Map between the {@link PreferenceControllerMixin} keys
     *               and the controllers themselves.
     * @param key The look-up key
     * @return The Payload from the {@link PreferenceControllerMixin} specified by the key,
     * if it exists. Otherwise null.
     */
    public static ResultPayload getPayloadFromUriMap(Map<String, PreferenceControllerMixin> uriMap,
            String key) {
        if (uriMap == null) {
            return null;
        }

        PreferenceControllerMixin controller = uriMap.get(key);
        if (controller == null) {
            return null;
        }

        return controller.getResultPayload();
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

    /**
     * Only allow a "well known" SearchIndexablesProvider. The provider should:
     *
     * - have read/write {@link Manifest.permission#READ_SEARCH_INDEXABLES}
     * - be from a privileged package
     */
    static boolean isWellKnownProvider(ResolveInfo info, Context context) {
        final String authority = info.providerInfo.authority;
        final String packageName = info.providerInfo.applicationInfo.packageName;

        if (TextUtils.isEmpty(authority) || TextUtils.isEmpty(packageName)) {
            return false;
        }

        final String readPermission = info.providerInfo.readPermission;
        final String writePermission = info.providerInfo.writePermission;

        if (TextUtils.isEmpty(readPermission) || TextUtils.isEmpty(writePermission)) {
            return false;
        }

        if (!android.Manifest.permission.READ_SEARCH_INDEXABLES.equals(readPermission) ||
                !android.Manifest.permission.READ_SEARCH_INDEXABLES.equals(writePermission)) {
            return false;
        }

        return isPrivilegedPackage(packageName, context);
    }

    static String normalizeHyphen(String input) {
        return (input != null) ? input.replaceAll(NON_BREAKING_HYPHEN, HYPHEN) : EMPTY;
    }

    static String normalizeString(String input) {
        final String nohyphen = (input != null) ? input.replaceAll(HYPHEN, EMPTY) : EMPTY;
        final String normalized = Normalizer.normalize(nohyphen, Normalizer.Form.NFD);

        return REMOVE_DIACRITICALS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    static String normalizeKeywords(String input) {
        return (input != null) ? input.replaceAll(LIST_DELIMITERS, SPACE) : EMPTY;
    }

    private static boolean isPrivilegedPackage(String packageName, Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packInfo = pm.getPackageInfo(packageName, 0);
            return ((packInfo.applicationInfo.privateFlags
                    & ApplicationInfo.PRIVATE_FLAG_PRIVILEGED) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
