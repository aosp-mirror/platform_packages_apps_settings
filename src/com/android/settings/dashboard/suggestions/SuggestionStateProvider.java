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
 */

package com.android.settings.dashboard.suggestions;

import static android.content.Intent.EXTRA_COMPONENT_NAME;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.overlay.FeatureFactory;

public class SuggestionStateProvider extends ContentProvider {

    private static final String TAG = "SugstStatusProvider";

    @VisibleForTesting
    static final String METHOD_GET_SUGGESTION_STATE = "getSuggestionState";
    @VisibleForTesting
    static final String EXTRA_CANDIDATE_ID = "candidate_id";
    private static final String RESULT_IS_COMPLETE = "candidate_is_complete";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException("query operation not supported currently.");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("getType operation not supported currently.");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("insert operation not supported currently.");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("delete operation not supported currently.");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("update operation not supported currently.");
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        final Bundle bundle = new Bundle();
        if (METHOD_GET_SUGGESTION_STATE.equals(method)) {
            final String id = extras.getString(EXTRA_CANDIDATE_ID);
            final ComponentName cn = extras.getParcelable(EXTRA_COMPONENT_NAME);
            final boolean isComplete;
            if (cn == null) {
                isComplete = true;
            } else {
                final Context context = getContext();
                isComplete = FeatureFactory.getFactory(context)
                        .getSuggestionFeatureProvider(context)
                        .isSuggestionComplete(context, cn);
            }
            Log.d(TAG, "Suggestion " + id + " complete: " + isComplete);
            bundle.putBoolean(RESULT_IS_COMPLETE, isComplete);
        }
        return bundle;
    }
}
