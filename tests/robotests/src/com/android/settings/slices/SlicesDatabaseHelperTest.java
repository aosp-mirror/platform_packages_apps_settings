package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.android.settings.TestConfig;
import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SlicesDatabaseHelperTest {

    private Context mContext;
    private SlicesDatabaseHelper mSlicesDatabaseHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSlicesDatabaseHelper = new SlicesDatabaseHelper(mContext);
        mDatabase = mSlicesDatabaseHelper.getWritableDatabase();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testDatabaseSchema() {
        Cursor cursor = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        String[] columnNames = cursor.getColumnNames();

        String[] expectedNames = new String[]{
                IndexColumns.KEY,
                IndexColumns.TITLE,
                IndexColumns.SUBTITLE,
                IndexColumns.SCREENTITLE,
                IndexColumns.ICON_RESOURCE,
                IndexColumns.FRAGMENT,
                IndexColumns.CONTROLLER
        };

        assertThat(columnNames).isEqualTo(expectedNames);
    }

    @Test
    public void testUpgrade_dropsOldData() {
        ContentValues dummyValues = getDummyRow();

        mDatabase.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, dummyValues);
        Cursor baseline = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        assertThat(baseline.getCount()).isEqualTo(1);

        mSlicesDatabaseHelper.onUpgrade(mDatabase, 0, 1);

        Cursor newCursor = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        assertThat(newCursor.getCount()).isEqualTo(0);
    }

    private ContentValues getDummyRow() {
        ContentValues values;

        values = new ContentValues();
        values.put(IndexColumns.KEY, "key");
        values.put(IndexColumns.TITLE, "title");
        values.put(IndexColumns.SUBTITLE, "subtitle");
        values.put(IndexColumns.ICON_RESOURCE, 99);
        values.put(IndexColumns.FRAGMENT, "fragmentClassName");
        values.put(IndexColumns.CONTROLLER, "preferenceController");

        return values;
    }
}
