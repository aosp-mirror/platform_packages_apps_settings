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

package com.android.settings.search.indexing;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.SettingsSearchIndexablesProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;

/**
 * Collects all data from {@link android.provider.SearchIndexablesProvider} to be indexed.
 */
public class PreIndexDataCollector {

    private static final String TAG = "IndexableDataCollector";

    // TODO (b/64938328) update to new search package.
    private final String BASE_AUTHORITY = "com.android.settings";

    private static final List<String> EMPTY_LIST = Collections.emptyList();

    private Context mContext;

    private PreIndexData mIndexData;

    public PreIndexDataCollector(Context context) {
        mContext = context;
    }

    public PreIndexData collectIndexableData(List<ResolveInfo> providers, boolean isFullIndex) {
        mIndexData = new PreIndexData();

        for (final ResolveInfo info : providers) {
            if (!isWellKnownProvider(info)) {
                continue;
            }
            final String authority = info.providerInfo.authority;
            final String packageName = info.providerInfo.packageName;

            if (isFullIndex) {
                addIndexablesFromRemoteProvider(packageName, authority);
            }

            final long nonIndexableStartTime = System.currentTimeMillis();
            addNonIndexablesKeysFromRemoteProvider(packageName, authority);
            if (SettingsSearchIndexablesProvider.DEBUG) {
                final long nonIndexableTime = System.currentTimeMillis() - nonIndexableStartTime;
                Log.d(TAG, "performIndexing update non-indexable for package " + packageName
                        + " took time: " + nonIndexableTime);
            }
        }

        return mIndexData;
    }

    private boolean addIndexablesFromRemoteProvider(String packageName, String authority) {
        try {
            final Context context = BASE_AUTHORITY.equals(authority) ?
                    mContext : mContext.createPackageContext(packageName, 0);

            final Uri uriForResources = buildUriForXmlResources(authority);
            mIndexData.dataToUpdate.addAll(getIndexablesForXmlResourceUri(context, packageName,
                    uriForResources, SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS));

            final Uri uriForRawData = buildUriForRawData(authority);
            mIndexData.dataToUpdate.addAll(getIndexablesForRawDataUri(context, packageName,
                    uriForRawData, SearchIndexablesContract.INDEXABLES_RAW_COLUMNS));
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return false;
        }
    }

    @VisibleForTesting
    List<SearchIndexableResource> getIndexablesForXmlResourceUri(Context packageContext,
            String packageName, Uri uri, String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        List<SearchIndexableResource> resources = new ArrayList<>();

        if (cursor == null) {
            Log.w(TAG, "Cannot add index data for Uri: " + uri.toString());
            return resources;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int xmlResId = cursor.getInt(COLUMN_INDEX_XML_RES_RESID);

                    final String className = cursor.getString(COLUMN_INDEX_XML_RES_CLASS_NAME);
                    final int iconResId = cursor.getInt(COLUMN_INDEX_XML_RES_ICON_RESID);

                    final String action = cursor.getString(COLUMN_INDEX_XML_RES_INTENT_ACTION);
                    final String targetPackage = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS);

                    SearchIndexableResource sir = new SearchIndexableResource(packageContext);
                    sir.xmlResId = xmlResId;
                    sir.className = className;
                    sir.packageName = packageName;
                    sir.iconResId = iconResId;
                    sir.intentAction = action;
                    sir.intentTargetPackage = targetPackage;
                    sir.intentTargetClass = targetClass;

                    resources.add(sir);
                }
            }
        } finally {
            cursor.close();
        }
        return resources;
    }

    private void addNonIndexablesKeysFromRemoteProvider(String packageName,
            String authority) {
        final List<String> keys =
                getNonIndexablesKeysFromRemoteProvider(packageName, authority);

        if (keys != null && !keys.isEmpty()) {
            mIndexData.nonIndexableKeys.put(authority, new ArraySet<>(keys));
        }
    }

    @VisibleForTesting
    List<String> getNonIndexablesKeysFromRemoteProvider(String packageName,
            String authority) {
        try {
            final Context packageContext = mContext.createPackageContext(packageName, 0);

            final Uri uriForNonIndexableKeys = buildUriForNonIndexableKeys(authority);
            return getNonIndexablesKeys(packageContext, uriForNonIndexableKeys,
                    SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return EMPTY_LIST;
        }
    }

    @VisibleForTesting
    Uri buildUriForXmlResources(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.INDEXABLES_XML_RES_PATH);
    }

    @VisibleForTesting
    Uri buildUriForRawData(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.INDEXABLES_RAW_PATH);
    }

    @VisibleForTesting
    Uri buildUriForNonIndexableKeys(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.NON_INDEXABLES_KEYS_PATH);
    }

    @VisibleForTesting
    List<SearchIndexableRaw> getIndexablesForRawDataUri(Context packageContext, String packageName,
            Uri uri, String[] projection) {
        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        List<SearchIndexableRaw> rawData = new ArrayList<>();

        if (cursor == null) {
            Log.w(TAG, "Cannot add index data for Uri: " + uri.toString());
            return rawData;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int providerRank = cursor.getInt(COLUMN_INDEX_RAW_RANK);
                    // TODO Remove rank
                    final String title = cursor.getString(COLUMN_INDEX_RAW_TITLE);
                    final String summaryOn = cursor.getString(COLUMN_INDEX_RAW_SUMMARY_ON);
                    final String summaryOff = cursor.getString(COLUMN_INDEX_RAW_SUMMARY_OFF);
                    final String entries = cursor.getString(COLUMN_INDEX_RAW_ENTRIES);
                    final String keywords = cursor.getString(COLUMN_INDEX_RAW_KEYWORDS);

                    final String screenTitle = cursor.getString(COLUMN_INDEX_RAW_SCREEN_TITLE);

                    final String className = cursor.getString(COLUMN_INDEX_RAW_CLASS_NAME);
                    final int iconResId = cursor.getInt(COLUMN_INDEX_RAW_ICON_RESID);

                    final String action = cursor.getString(COLUMN_INDEX_RAW_INTENT_ACTION);
                    final String targetPackage = cursor.getString(
                            COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            COLUMN_INDEX_RAW_INTENT_TARGET_CLASS);

                    final String key = cursor.getString(COLUMN_INDEX_RAW_KEY);
                    final int userId = cursor.getInt(COLUMN_INDEX_RAW_USER_ID);

                    SearchIndexableRaw data = new SearchIndexableRaw(packageContext);
                    data.title = title;
                    data.summaryOn = summaryOn;
                    data.summaryOff = summaryOff;
                    data.entries = entries;
                    data.keywords = keywords;
                    data.screenTitle = screenTitle;
                    data.className = className;
                    data.packageName = packageName;
                    data.iconResId = iconResId;
                    data.intentAction = action;
                    data.intentTargetPackage = targetPackage;
                    data.intentTargetClass = targetClass;
                    data.key = key;
                    data.userId = userId;

                    rawData.add(data);
                }
            }
        } finally {
            cursor.close();
        }

        return rawData;
    }

    private List<String> getNonIndexablesKeys(Context packageContext, Uri uri,
            String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final List<String> result = new ArrayList<>();
        Cursor cursor;
        try {
            cursor = resolver.query(uri, projection, null, null, null);
        } catch (NullPointerException e) {
            Log.e(TAG, "Exception querying the keys!", e);
            return result;
        }

        if (cursor == null) {
            Log.w(TAG, "Cannot add index data for Uri: " + uri.toString());
            return result;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);

                    if (TextUtils.isEmpty(key) && Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Empty non-indexable key from: "
                                + packageContext.getPackageName());
                        continue;
                    }

                    result.add(key);
                }
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    /**
     * Only allow a "well known" SearchIndexablesProvider. The provider should:
     *
     * - have read/write {@link Manifest.permission#READ_SEARCH_INDEXABLES}
     * - be from a privileged package
     */
    @VisibleForTesting
    boolean isWellKnownProvider(ResolveInfo info) {
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

        return isPrivilegedPackage(packageName, mContext);
    }

    /**
     * @return true if the {@param packageName} is privileged.
     */
    private boolean isPrivilegedPackage(String packageName, Context context) {
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
