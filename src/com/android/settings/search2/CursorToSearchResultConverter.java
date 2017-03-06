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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.SiteMapManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_CLASS_NAME;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_ICON;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_ID;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_INTENT_ACTION;
import static com.android.settings.search2.DatabaseResultLoader
        .COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS;
import static com.android.settings.search2.DatabaseResultLoader
        .COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_KEY;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_PAYLOAD;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_PAYLOAD_TYPE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_SCREEN_TITLE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_SUMMARY_ON;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_TITLE;

/**
 * Controller to Build search results from {@link Cursor} Objects.
 *
 * Each converted {@link Cursor} has the following fields:
 * - String Title
 * - String Summary
 * - int rank
 * - {@link Drawable} icon
 * - {@link ResultPayload} payload
 */
class CursorToSearchResultConverter {

    private final String TAG = "CursorConverter";

    private final String mQueryText;

    private final Context mContext;

    private final Set<String> mKeys;

    private final int LONG_TITLE_LENGTH = 20;

    private static final String[] whiteList = {
            "main_toggle_wifi",
            "main_toggle_bluetooth",
            "toggle_airplane",
            "tether_settings",
            "battery_saver",
            "toggle_nfc",
            "restrict_background",
            "data_usage_enable",
            "button_roaming_key",
    };
    private static final Set<String> prioritySettings = new HashSet(Arrays.asList(whiteList));


    public CursorToSearchResultConverter(Context context, String queryText) {
        mContext = context;
        mKeys = new HashSet<>();
        mQueryText = queryText;
    }

    public List<SearchResult> convertCursor(SiteMapManager sitemapManager,
            Cursor cursorResults, int baseRank) {
        if (cursorResults == null) {
            return null;
        }
        final Map<String, Context> contextMap = new HashMap<>();
        final List<SearchResult> results = new ArrayList<>();

        while (cursorResults.moveToNext()) {
            SearchResult result = buildSingleSearchResultFromCursor(sitemapManager,
                    contextMap, cursorResults, baseRank);
            if (result != null) {
                results.add(result);
            }
        }
        Collections.sort(results);
        return results;
    }

    private SearchResult buildSingleSearchResultFromCursor(SiteMapManager sitemapManager,
            Map<String, Context> contextMap, Cursor cursor, int baseRank) {
        final String docId = cursor.getString(COLUMN_INDEX_ID);
        /* Make sure that this result has not yet been added as a result. Checking the docID
           covers the case of multiple queries matching the same row, but we need to also to check
           for potentially the same named or slightly varied names pointing to the same page.
         */
        if (mKeys.contains(docId)) {
            return null;
        }
        mKeys.add(docId);

        final String pkgName = cursor.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
        final String action = cursor.getString(COLUMN_INDEX_INTENT_ACTION);
        final String title = cursor.getString(COLUMN_INDEX_TITLE);
        final String summaryOn = cursor.getString(COLUMN_INDEX_SUMMARY_ON);
        final String className = cursor.getString(COLUMN_INDEX_CLASS_NAME);
        final String key = cursor.getString(COLUMN_INDEX_KEY);
        final String iconResStr = cursor.getString(COLUMN_INDEX_ICON);
        final int payloadType = cursor.getInt(COLUMN_INDEX_PAYLOAD_TYPE);
        final byte[] marshalledPayload = cursor.getBlob(COLUMN_INDEX_PAYLOAD);
        final ResultPayload payload;

        if (marshalledPayload != null) {
            payload = getUnmarshalledPayload(marshalledPayload, payloadType);
        } else if (payloadType == ResultPayload.PayloadType.INTENT) {
            payload = getIntentPayload(cursor, action, key, className, pkgName);
        } else {
            Log.w(TAG, "Error creating payload - bad marshalling data or mismatched types");
            return null;
        }

        final List<String> breadcrumbs = getBreadcrumbs(sitemapManager, cursor);
        final int rank = getRank(title, breadcrumbs, baseRank, key);

        final SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(title)
                .addSummary(summaryOn)
                .addBreadcrumbs(breadcrumbs)
                .addRank(rank)
                .addIcon(getIconForPackage(contextMap, pkgName, className, iconResStr))
                .addPayload(payload);
        return builder.build();
    }

    private Drawable getIconForPackage(Map<String, Context> contextMap, String pkgName,
            String className, String iconResStr) {
        final int iconId = TextUtils.isEmpty(iconResStr)
                ? 0 : Integer.parseInt(iconResStr);
        Drawable icon;
        Context packageContext;
        if (iconId == 0) {
            icon = null;
        } else {
            if (TextUtils.isEmpty(className) && !TextUtils.isEmpty(pkgName)) {
                packageContext = contextMap.get(pkgName);
                if (packageContext == null) {
                    try {
                        packageContext = mContext.createPackageContext(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Cannot create Context for package: " + pkgName);
                        return null;
                    }
                    contextMap.put(pkgName, packageContext);
                }
            } else {
                packageContext = mContext;
            }
            try {
                icon = packageContext.getDrawable(iconId);
            } catch (Resources.NotFoundException nfe) {
                icon = null;
            }
        }
        return icon;
    }

    private IntentPayload getIntentPayload(Cursor cursor, String action, String key,
            String className, String pkgName ) {
        IntentPayload payload;
        if (TextUtils.isEmpty(action)) {
            final String screenTitle = cursor.getString(COLUMN_INDEX_SCREEN_TITLE);
            // Action is null, we will launch it as a sub-setting
            final Bundle args = new Bundle();
            args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
            final Intent intent = Utils.onBuildStartFragmentIntent(mContext,
                    className, args, null, 0, screenTitle, false,
                    MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS);
            payload = new IntentPayload(intent);
        } else {
            final Intent intent = new Intent(action);
            final String targetClass = cursor.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS);
            if (!TextUtils.isEmpty(pkgName) && !TextUtils.isEmpty(targetClass)) {
                final ComponentName component = new ComponentName(pkgName, targetClass);
                intent.setComponent(component);
            }
            intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
            payload = new IntentPayload(intent);
        }
        return payload;
    }

    private ResultPayload getUnmarshalledPayload(byte[] unmarshalledPayload, int payloadType) {
        try {
            switch (payloadType) {
                case ResultPayload.PayloadType.INLINE_SWITCH:
                    return ResultPayloadUtils.unmarshall(unmarshalledPayload,
                            InlineSwitchPayload.CREATOR);
            }
        } catch (BadParcelableException e) {
            Log.w(TAG, "Error creating parcelable: " + e);
        }
        return null;
    }

    private List<String> getBreadcrumbs(SiteMapManager siteMapManager, Cursor cursor) {
        final String screenTitle = cursor.getString(COLUMN_INDEX_SCREEN_TITLE);
        final String screenClass = cursor.getString(COLUMN_INDEX_CLASS_NAME);
        return siteMapManager == null ? null : siteMapManager.buildBreadCrumb(mContext, screenClass,
                screenTitle);
    }

    /** Uses the breadcrumbs to determine the offset to the base rank.
     *  There are three checks
     *  A) If the result is prioritized and the highest base level
     *  B) If the query matches the highest level menu title
     *  C) If the query matches a subsequent menu title
     *  D) Is the title longer than 20
     *
     *  If the query matches A, set it to TOP_RANK
     *  If the query matches B and C, the offset is 0.
     *  If the query matches C only, the offset is 1.
     *  If the query matches neither B nor C, the offset is 2.
     *  If the query matches D, the offset is 2

     * @param title of the result.
     * @param crumbs from the Information Architecture
     * @param baseRank of the result. Lower if it's a better result.
     * @return
     */
    private int getRank(String title, List<String> crumbs, int baseRank, String key) {
        // The result can only be prioritized if it is a top ranked result.
        if (prioritySettings.contains(key) && baseRank < DatabaseResultLoader.BASE_RANKS[1]) {
            return SearchResult.TOP_RANK;
        }
        if (title.length() > LONG_TITLE_LENGTH) {
            return baseRank + 2;
        }
        return baseRank;
    }

}
