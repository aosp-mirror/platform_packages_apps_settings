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
 * limitations under the License
 */

package com.android.settings.slices;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.Locale;

/**
 * Defines the schema for the Slices database.
 */
public class SlicesDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "SlicesDatabaseHelper";

    private static final String DATABASE_NAME = "slices_index.db";
    private static final String SHARED_PREFS_TAG = "slices_shared_prefs";

    private static final int DATABASE_VERSION = 9;

    public interface Tables {
        String TABLE_SLICES_INDEX = "slices_index";
    }

    public interface IndexColumns {
        /**
         * Primary key of the DB. Preference key from preference controllers.
         */
        String KEY = "key";

        /**
         * Title of the Setting.
         */
        String TITLE = "title";

        /**
         * Summary / Subtitle for the setting.
         */
        String SUMMARY = "summary";

        /**
         * Title of the Setting screen on which the Setting lives.
         */
        String SCREENTITLE = "screentitle";

        /**
         * String with a comma separated list of keywords relating to the Slice.
         */
        String KEYWORDS = "keywords";

        /**
         * Resource ID for the icon of the setting. Should be 0 for no icon.
         */
        String ICON_RESOURCE = "icon";

        /**
         * Classname of the fragment name of the page that hosts the setting.
         */
        String FRAGMENT = "fragment";

        /**
         * Class name of the controller backing the setting. Must be a
         * {@link com.android.settings.core.BasePreferenceController}.
         */
        String CONTROLLER = "controller";

        /**
         * {@link SliceData.SliceType} representing the inline type of the result.
         */
        String SLICE_TYPE = "slice_type";

        /**
         * Customized subtitle if it's a unavailable slice
         */
        String UNAVAILABLE_SLICE_SUBTITLE = "unavailable_slice_subtitle";

        /**
         * The uri of slice.
         */
        String SLICE_URI = "slice_uri";

        /**
         * Whether the slice should be exposed publicly.
         */
        String PUBLIC_SLICE = "public_slice";

        /**
         * Resource ID for the menu entry of the setting.
         */
        String HIGHLIGHT_MENU_RESOURCE = "highlight_menu";
    }

    private static final String CREATE_SLICES_TABLE =
            "CREATE VIRTUAL TABLE " + Tables.TABLE_SLICES_INDEX + " USING fts4"
                    + "("
                    + IndexColumns.KEY
                    + ", "
                    + IndexColumns.SLICE_URI
                    + ", "
                    + IndexColumns.TITLE
                    + ", "
                    + IndexColumns.SUMMARY
                    + ", "
                    + IndexColumns.SCREENTITLE
                    + ", "
                    + IndexColumns.KEYWORDS
                    + ", "
                    + IndexColumns.ICON_RESOURCE
                    + ", "
                    + IndexColumns.FRAGMENT
                    + ", "
                    + IndexColumns.CONTROLLER
                    + ", "
                    + IndexColumns.SLICE_TYPE
                    + ", "
                    + IndexColumns.UNAVAILABLE_SLICE_SUBTITLE
                    + ", "
                    + IndexColumns.PUBLIC_SLICE
                    + ", "
                    + IndexColumns.HIGHLIGHT_MENU_RESOURCE
                    + " INTEGER DEFAULT 0 "
                    + ");";

    private final Context mContext;

    private static SlicesDatabaseHelper sSingleton;

    public static synchronized SlicesDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new SlicesDatabaseHelper(context.getApplicationContext());
        }
        return sSingleton;
    }

    private SlicesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null /* CursorFactor */, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabases(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DATABASE_VERSION) {
            Log.d(TAG, "Reconstructing DB from " + oldVersion + " to " + newVersion);
            reconstruct(db);
        }
    }

    /**
     * Drops the currently stored databases rebuilds them.
     * Also un-marks the state of the data such that any subsequent call to
     * {@link#isNewIndexingState(Context)} will return {@code true}.
     */
    void reconstruct(SQLiteDatabase db) {
        mContext.getSharedPreferences(SHARED_PREFS_TAG, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        dropTables(db);
        createDatabases(db);
    }

    /**
     * Marks the current state of the device for the validity of the data. Should be called after
     * a full index of the TABLE_SLICES_INDEX.
     */
    public void setIndexedState() {
        setBuildIndexed();
        setLocaleIndexed();
    }

    /**
     * Indicates if the indexed slice data reflects the current state of the phone.
     *
     * @return {@code true} if database should be rebuilt, {@code false} otherwise.
     */
    public boolean isSliceDataIndexed() {
        return isBuildIndexed() && isLocaleIndexed();
    }

    private void createDatabases(SQLiteDatabase db) {
        db.execSQL(CREATE_SLICES_TABLE);
        Log.d(TAG, "Created databases");
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_SLICES_INDEX);
    }

    private void setBuildIndexed() {
        mContext.getSharedPreferences(SHARED_PREFS_TAG, 0 /* mode */)
                .edit()
                .putBoolean(getBuildTag(), true /* value */)
                .apply();
    }

    private void setLocaleIndexed() {
        mContext.getSharedPreferences(SHARED_PREFS_TAG, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Locale.getDefault().toString(), true /* value */)
                .apply();
    }

    private boolean isBuildIndexed() {
        return mContext.getSharedPreferences(SHARED_PREFS_TAG,
                Context.MODE_PRIVATE)
                .getBoolean(getBuildTag(), false /* default */);
    }

    private boolean isLocaleIndexed() {
        return mContext.getSharedPreferences(SHARED_PREFS_TAG,
                Context.MODE_PRIVATE)
                .getBoolean(Locale.getDefault().toString(), false /* default */);
    }

    @VisibleForTesting
    String getBuildTag() {
        return Build.VERSION.INCREMENTAL;
    }
}
