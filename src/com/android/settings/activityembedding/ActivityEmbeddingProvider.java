/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.activityembedding;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.settings.SettingsApplication;

/**
 * A content provider for querying the state of activity embedding feature
 */
public class ActivityEmbeddingProvider extends ContentProvider {

    private static final String METHOD_IS_EMBEDDING_ACTIVITY_ENABLED = "isEmbeddingActivityEnabled";
    private static final String METHOD_IS_IN_SETTINGS_TWO_PANE = "isInSettingsTwoPane";
    private static final String EXTRA_ENABLED_STATE = "enabled_state";
    private static final String EXTRA_TWO_PANE_STATE = "two_pane_state";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (TextUtils.equals(method, METHOD_IS_EMBEDDING_ACTIVITY_ENABLED)) {
            final Bundle bundle = new Bundle();
            bundle.putBoolean(EXTRA_ENABLED_STATE,
                    ActivityEmbeddingUtils.isEmbeddingActivityEnabled(getContext()));
            return bundle;
        } else if (TextUtils.equals(method, METHOD_IS_IN_SETTINGS_TWO_PANE)) {
            final Activity homeActivity =
                    ((SettingsApplication) getContext().getApplicationContext()).getHomeActivity();
            final Bundle bundle = new Bundle();
            bundle.putBoolean(EXTRA_TWO_PANE_STATE,
                    homeActivity == null ? false
                            : ActivityEmbeddingUtils.isTwoPaneResolution(homeActivity));
            return bundle;
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
