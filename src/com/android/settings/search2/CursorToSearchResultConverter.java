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
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.search.Index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_RANK;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_TITLE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_SUMMARY_ON;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_CLASS_NAME;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_ICON;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_INTENT_ACTION;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_KEY;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_PAYLOAD_TYPE;
import static com.android.settings.search2.DatabaseResultLoader.COLUMN_INDEX_PAYLOAD;

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

    private final Context mContext;

    public CursorToSearchResultConverter(Context context) {
        mContext = context;
    }

    public List<SearchResult> convertCursor(Cursor cursorResults) {
        if (cursorResults == null) {
            return null;
        }
        final Map<String, Context> contextMap = new HashMap<>();
        final ArrayList<SearchResult> results = new ArrayList<>();

        while (cursorResults.moveToNext()) {
            SearchResult result = buildSingleSearchResultFromCursor(contextMap, cursorResults);
            if (result != null) {
                results.add(result);
            }
        }
        Collections.sort(results);
        return results;
    }

    private SearchResult buildSingleSearchResultFromCursor(Map<String, Context> contextMap,
            Cursor cursor) {
        final String pkgName = cursor.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
        final String action = cursor.getString(COLUMN_INDEX_INTENT_ACTION);
        final String title = cursor.getString(COLUMN_INDEX_TITLE);
        final String summaryOn = cursor.getString(COLUMN_INDEX_SUMMARY_ON);
        final String className = cursor.getString(COLUMN_INDEX_CLASS_NAME);
        final int rank = cursor.getInt(COLUMN_INDEX_RANK);
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

        final SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(title)
                .addSummary(summaryOn)
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
            final String screenTitle = cursor.getString(Index.COLUMN_INDEX_SCREEN_TITLE);
            // Action is null, we will launch it as a sub-setting
            final Bundle args = new Bundle();
            args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
            final Intent intent = Utils.onBuildStartFragmentIntent(mContext,
                    className, args, null, 0, screenTitle, false);
            payload = new IntentPayload(intent);
        } else {
            final Intent intent = new Intent(action);
            final String targetClass = cursor.getString(
                    Index.COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS);
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
}
