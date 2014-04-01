/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;

import java.util.Collection;

import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    private static final String TAG = "SettingsSearchIndexablesProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        Collection<SearchIndexableResource> values = SearchIndexableResources.values();
        for (SearchIndexableResource val : values) {
            Object[] ref = new Object[7];
            ref[0] = val.rank;
            ref[1] = val.xmlResId;
            ref[2] = val.className;
            ref[3] = val.iconResId;
            ref[4] = null; // intent action
            ref[5] = null; // intent target package
            ref[6] = null; // intent target class
            cursor.addRow(ref);
        }
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        Cursor result = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        return result;
    }
}
