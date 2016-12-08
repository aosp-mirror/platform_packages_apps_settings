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
 *
 */

package com.android.settings.search2;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.search.Indexable;

import java.lang.reflect.Field;

/**
 * Utility class for {@like DatabaseIndexingManager} to handle the mapping between Payloads
 * and Preference controllers, and managing indexable classes.
 */
public class DatabaseIndexingUtils {

    private static final String LOG_TAG = "IndexingUtil";

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    public static Class<?> getIndexableClass(String className) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "Cannot find class: " + className);
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
            Log.d(LOG_TAG, "Cannot find field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (SecurityException se) {
            Log.d(LOG_TAG,
                    "Security exception for field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalAccessException e) {
            Log.d(LOG_TAG,
                    "Illegal access to field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalArgumentException e) {
            Log.d(LOG_TAG,
                    "Illegal argument when accessing field '" +
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
    public static boolean isWellKnownProvider(ResolveInfo info, Context context) {
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

    public static boolean isPrivilegedPackage(String packageName, Context context) {
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
