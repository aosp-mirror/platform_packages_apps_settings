package com.android.settings.slices;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Defines the schema for the Slices database.
 */
public class SlicesDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "SlicesDatabaseHelper";

    private static final String DATABASE_NAME = "slices_index.db";
    private static final String SHARED_PREFS_TAG = "slices_shared_prefs";

    private static final int DATABASE_VERSION = 1;

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
        String SUBTITLE = "subtitle";

        /**
         * Title of the Setting screen on which the Setting lives.
         */
        String SCREENTITLE = "screentitle";

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
    }

    private static final String CREATE_SLICES_TABLE =
            "CREATE VIRTUAL TABLE " + Tables.TABLE_SLICES_INDEX + " USING fts4" +
                    "(" +
                    IndexColumns.KEY +
                    ", " +
                    IndexColumns.TITLE +
                    ", " +
                    IndexColumns.SUBTITLE +
                    ", " +
                    IndexColumns.SCREENTITLE +
                    ", " +
                    IndexColumns.ICON_RESOURCE +
                    ", " +
                    IndexColumns.FRAGMENT +
                    ", " +
                    IndexColumns.CONTROLLER +
                    ");";

    private final Context mContext;

    public SlicesDatabaseHelper(Context context) {
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
            Log.d(TAG, "Reconstructing DB from " + oldVersion + "to " + newVersion);
            reconstruct(db);
        }
    }

    @VisibleForTesting
    void reconstruct(SQLiteDatabase db) {
        mContext.getSharedPreferences(SHARED_PREFS_TAG, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        dropTables(db);
        createDatabases(db);
    }

    private void createDatabases(SQLiteDatabase db) {
        db.execSQL(CREATE_SLICES_TABLE);
        Log.d(TAG, "Created databases");
    }


    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_SLICES_INDEX);
    }
}