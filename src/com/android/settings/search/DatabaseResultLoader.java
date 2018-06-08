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

import static com.android.settings.search.IndexDatabaseHelper.IndexColumns;

/**
 * AsyncTask to retrieve Settings, first party app and any intent based results.
 */
public class DatabaseResultLoader {

    private static final String TAG = "DatabaseResultLoader";

    public static final String[] SELECT_COLUMNS = {
            IndexColumns.DOCID,
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_SUMMARY_ON,
            IndexColumns.DATA_SUMMARY_OFF,
            IndexColumns.CLASS_NAME,
            IndexColumns.SCREEN_TITLE,
            IndexColumns.ICON,
            IndexColumns.INTENT_ACTION,
            IndexColumns.INTENT_TARGET_PACKAGE,
            IndexColumns.INTENT_TARGET_CLASS,
            IndexColumns.DATA_KEY_REF,
            IndexColumns.PAYLOAD_TYPE,
            IndexColumns.PAYLOAD
    };

    /**
     * These indices are used to match the columns of the this loader's SELECT statement.
     * These are not necessarily the same order nor similar coverage as the schema defined in
     * IndexDatabaseHelper
     */
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE = 8;
    public static final int COLUMN_INDEX_KEY = 10;
}